package com.geopslabs.geops.identity.infrastructure.sunat;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyIn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.support.TransactionOperations;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipInputStream;

/**
 * Replaces sunat_taxpayers with a new copy of the reduced registry, keeping only Lima and Callao (E2-12).
 * <ol>
 *   <li>Build, without touching the live table: the file is read as a stream and copied with COPY into a temporary
 *   table, then into sunat_taxpayers_new without indexes (DISTINCT ON the RUC); the primary key is created once the
 *   rows are in, and the table is analyzed.</li>
 *   <li>Guard: a file with fewer than 90 % of the rows of the previous load is taken as partial and refused.</li>
 *   <li>Swap, in a short transaction: sunat_taxpayers_new is renamed over sunat_taxpayers, the old table is dropped
 *   and the load is recorded once in sunat_registry_loads.</li>
 * </ol>
 * When the download, the reading or the guard fail, nothing is swapped and the previous registry stays (S2-04).
 */
public class SunatRegistryLoader {
    private static final Logger LOGGER = LoggerFactory.getLogger(SunatRegistryLoader.class);
    private static final String LOAD_LOCK = "identity.sunat-registry-load";
    private static final String ZIP_EXTENSION = ".zip";
    private static final int HTTP_OK = 200;
    private static final int COPY_BUFFER_CHARS = 64 * 1024;
    private static final int BUSINESS_NAME_LENGTH = 255;
    private static final int STATUS_LENGTH = 30;
    private static final String CREATE_LOAD_TABLE = """
            CREATE TEMPORARY TABLE sunat_taxpayers_load (
                ruc char(11) NOT NULL,
                business_name varchar(255) NOT NULL,
                taxpayer_status varchar(30) NOT NULL,
                taxpayer_condition varchar(30) NOT NULL
            ) ON COMMIT DROP""";
    private static final String COPY_LOAD_TABLE =
            "COPY sunat_taxpayers_load (ruc, business_name, taxpayer_status, taxpayer_condition) FROM STDIN";
    private static final String DROP_NEW_TABLE = "DROP TABLE IF EXISTS sunat_taxpayers_new";
    private static final String CREATE_NEW_TABLE = "CREATE TABLE sunat_taxpayers_new (LIKE sunat_taxpayers)";
    private static final String FILL_NEW_TABLE = """
            INSERT INTO sunat_taxpayers_new (ruc, business_name, taxpayer_status, taxpayer_condition, loaded_at)
            SELECT DISTINCT ON (ruc) ruc, business_name, taxpayer_status, taxpayer_condition, ?
              FROM sunat_taxpayers_load
             ORDER BY ruc""";
    private static final String INDEX_NEW_TABLE =
            "ALTER TABLE sunat_taxpayers_new ADD CONSTRAINT pk_sunat_taxpayers_new PRIMARY KEY (ruc)";
    private static final String ANALYZE_NEW_TABLE = "ANALYZE sunat_taxpayers_new";
    private static final String[] SWAP_TABLES = {
            "ALTER TABLE sunat_taxpayers RENAME TO sunat_taxpayers_old",
            "ALTER TABLE sunat_taxpayers_new RENAME TO sunat_taxpayers",
            "DROP TABLE sunat_taxpayers_old",
            "ALTER TABLE sunat_taxpayers RENAME CONSTRAINT pk_sunat_taxpayers_new TO pk_sunat_taxpayers"
    };
    private static final String RECORD_LOAD =
            "INSERT INTO sunat_registry_loads (loaded_at, row_count, source) VALUES (?, ?, ?)";
    private static final String PREVIOUS_ROWS = "SELECT row_count FROM sunat_registry_loads ORDER BY id DESC LIMIT 1";
    private static final String LAST_LOADED_AT = "SELECT loaded_at FROM sunat_registry_loads ORDER BY id DESC LIMIT 1";
    /** A new registry with fewer rows than this share of the previous load is taken as a partial file. */
    private static final double MINIMUM_SHARE_OF_PREVIOUS = 0.9;
    private static final int SOURCE_LENGTH = 255;
    private static final String TRY_LOCK = "SELECT pg_try_advisory_xact_lock(hashtext(?))";

    private final DataSource dataSource;
    private final JdbcTemplate jdbc;
    private final TransactionOperations transactions;
    private final ResourceLoader resourceLoader;
    private final SunatPadronParser parser;
    private final SunatProperties properties;
    private final Clock clock;
    private final HttpClient httpClient;

    public SunatRegistryLoader(DataSource dataSource, TransactionOperations transactions,
                               ResourceLoader resourceLoader, SunatProperties properties, Clock clock) {
        this.dataSource = dataSource;
        this.jdbc = new JdbcTemplate(dataSource);
        this.transactions = transactions;
        this.resourceLoader = resourceLoader;
        this.parser = new SunatPadronParser(properties);
        this.properties = properties;
        this.clock = clock;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    /** Loads from the configured source: the SUNAT URL or the local file. */
    public SunatLoadReport load() {
        return properties.source() == SunatProperties.Source.URL
                ? loadFromUrl(properties.url())
                : loadFromResource(resourceLoader.getResource(properties.file()));
    }

    public SunatLoadReport loadFromUrl(URI url) {
        var started = clock.instant();
        Path download = null;
        try {
            download = Files.createTempFile("padron-reducido", ZIP_EXTENSION);
            fetch(url, download);
            try (var input = Files.newInputStream(download)) {
                return replaceRegistry(url.toString(), input, isZip(url.getPath()), started);
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw failed(url.toString(), exception, started);
        } catch (IOException | RuntimeException exception) {
            throw failed(url.toString(), exception, started);
        } finally {
            deleteQuietly(download);
        }
    }

    public SunatLoadReport loadFromResource(Resource resource) {
        var started = clock.instant();
        var origin = resource.getDescription();
        try (var input = resource.getInputStream()) {
            return replaceRegistry(origin, input, isZip(resource.getFilename()), started);
        } catch (IOException | RuntimeException exception) {
            throw failed(origin, exception, started);
        }
    }

    /** The moment of the last complete load, empty while the registry has never been loaded. */
    public Optional<Instant> lastLoadedAt() {
        return jdbc.query(LAST_LOADED_AT, (row, index) -> row.getTimestamp(1).toInstant()).stream().findFirst();
    }

    private void fetch(URI url, Path target) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(url).timeout(properties.downloadTimeout()).GET().build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() != HTTP_OK) {
            throw new IOException("SUNAT answered HTTP " + response.statusCode());
        }
    }

    private SunatLoadReport replaceRegistry(String origin, InputStream input, boolean zipped, Instant started) {
        var loadedAt = clock.instant();
        var built = transactions.execute(status -> {
            if (!holdsLoadLock()) {
                return null;
            }
            jdbc.execute(DROP_NEW_TABLE);
            jdbc.execute(CREATE_LOAD_TABLE);
            var counts = copyIntoLoadTable(input, zipped);
            if (counts.kept() == 0) {
                throw new SunatRegistryLoadException("The file has no taxpayer of Lima or Callao");
            }
            jdbc.execute(CREATE_NEW_TABLE);
            long rows = jdbc.update(FILL_NEW_TABLE, Timestamp.from(loadedAt));
            ensureNotPartial(rows);
            jdbc.execute(INDEX_NEW_TABLE);
            jdbc.execute(ANALYZE_NEW_TABLE);
            return new CopyCounts(rows, counts.filtered(), counts.malformed());
        });
        if (built == null) {
            return SunatLoadReport.skipped();
        }
        var swapped = transactions.execute(status -> {
            if (!holdsLoadLock()) {
                return false;
            }
            for (var statement : SWAP_TABLES) {
                jdbc.execute(statement);
            }
            jdbc.update(RECORD_LOAD, Timestamp.from(loadedAt), built.kept(), cut(origin));
            return true;
        });
        if (!Boolean.TRUE.equals(swapped)) {
            return SunatLoadReport.skipped();
        }
        var report = new SunatLoadReport(built.kept(), built.filtered(), built.malformed(),
                Duration.between(started, clock.instant()), false);
        LOGGER.info("sunat.registry.loaded source={} rows={} filtered={} malformed={} durationMs={}", origin,
                report.rows(), report.filtered(), report.malformed(), report.duration().toMillis());
        return report;
    }

    private boolean holdsLoadLock() {
        if (Boolean.TRUE.equals(jdbc.queryForObject(TRY_LOCK, Boolean.class, LOAD_LOCK))) {
            return true;
        }
        LOGGER.info("sunat.registry.load.skipped reason=another-load-in-progress");
        return false;
    }

    /** The new table is still inside the build transaction: refusing it rolls it back with everything else. */
    private void ensureNotPartial(long rows) {
        var previous = jdbc.query(PREVIOUS_ROWS, (row, index) -> row.getLong(1)).stream().findFirst();
        if (previous.isPresent() && rows < previous.get() * MINIMUM_SHARE_OF_PREVIOUS) {
            throw new SunatRegistryLoadException("Partial registry: %d rows against %d of the previous load"
                    .formatted(rows, previous.get()));
        }
    }

    private static String cut(String value) {
        return value.length() <= SOURCE_LENGTH ? value : value.substring(0, SOURCE_LENGTH);
    }

    private CopyCounts copyIntoLoadTable(InputStream input, boolean zipped) {
        var connection = DataSourceUtils.getConnection(dataSource);
        CopyIn copy = null;
        try {
            copy = connection.unwrap(PGConnection.class).getCopyAPI().copyIn(COPY_LOAD_TABLE);
            var writer = new CopyWriter(copy);
            var counter = new long[2];
            var result = parser.parse(textOf(input, zipped), line -> {
                if (!line.isInAnyOf(properties.ubigeoPrefixes())) {
                    counter[1]++;
                    return;
                }
                counter[0]++;
                writer.write(line);
            });
            writer.flush();
            copy.endCopy();
            return new CopyCounts(counter[0], counter[1], result.malformed());
        } catch (SQLException exception) {
            cancel(copy);
            throw new SunatRegistryLoadException("The registry could not be copied", exception);
        } catch (IOException exception) {
            cancel(copy);
            throw new SunatRegistryLoadException("The registry file could not be read", exception);
        } catch (RuntimeException exception) {
            cancel(copy);
            throw exception;
        }
    }

    private static InputStream textOf(InputStream input, boolean zipped) throws IOException {
        if (!zipped) {
            return input;
        }
        var zip = new ZipInputStream(input);
        for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory()) {
                return zip;
            }
        }
        throw new IOException("The zip file has no registry inside");
    }

    private static boolean isZip(String name) {
        return name != null && name.toLowerCase(Locale.ROOT).endsWith(ZIP_EXTENSION);
    }

    private static void cancel(CopyIn copy) {
        try {
            if (copy != null && copy.isActive()) {
                copy.cancelCopy();
            }
        } catch (SQLException exception) {
            LOGGER.debug("sunat.registry.copy-cancel.failed reason={}", exception.getMessage());
        }
    }

    private SunatRegistryLoadException failed(String origin, Exception cause, Instant started) {
        var failure = cause instanceof SunatRegistryLoadException loadFailure
                ? loadFailure : new SunatRegistryLoadException("The registry could not be loaded", cause);
        LOGGER.error("sunat.registry.load.failed source={} durationMs={} reason={}", origin,
                Duration.between(started, clock.instant()).toMillis(), rootMessage(cause));
        return failure;
    }

    private static String rootMessage(Throwable failure) {
        var root = failure;
        while (root.getCause() != null) {
            root = root.getCause();
        }
        return root.getClass().getSimpleName() + ": " + root.getMessage();
    }

    private static void deleteQuietly(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException exception) {
            LOGGER.warn("sunat.registry.download.cleanup-failed path={}", path);
        }
    }

    private record CopyCounts(long kept, long filtered, long malformed) {
    }

    /** Text format of COPY: tab between fields, one row per line, with backslash escapes. */
    private static final class CopyWriter {
        private final CopyIn copy;
        private final StringBuilder buffer = new StringBuilder(COPY_BUFFER_CHARS + 512);

        private CopyWriter(CopyIn copy) {
            this.copy = copy;
        }

        void write(SunatTaxpayerLine line) {
            buffer.append(line.ruc()).append('\t')
                    .append(escape(cut(line.businessName(), BUSINESS_NAME_LENGTH))).append('\t')
                    .append(escape(cut(line.status(), STATUS_LENGTH))).append('\t')
                    .append(escape(cut(line.condition(), STATUS_LENGTH))).append('\n');
            if (buffer.length() >= COPY_BUFFER_CHARS) {
                flush();
            }
        }

        void flush() {
            if (buffer.isEmpty()) {
                return;
            }
            var bytes = buffer.toString().getBytes(StandardCharsets.UTF_8);
            try {
                copy.writeToCopy(bytes, 0, bytes.length);
            } catch (SQLException exception) {
                throw new UncheckedIOException(new IOException(exception));
            }
            buffer.setLength(0);
        }

        private static String cut(String value, int length) {
            return value.length() <= length ? value : value.substring(0, length);
        }

        private static String escape(String value) {
            return value.replace("\\", "\\\\").replace("\t", "\\t").replace("\n", "\\n").replace("\r", "\\r");
        }
    }
}
