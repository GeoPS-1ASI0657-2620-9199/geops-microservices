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
 * Replaces sunat_taxpayers with a new copy of the reduced registry, keeping only Lima and Callao (E2-12). The file is
 * read as a stream and copied with COPY into a temporary table; the swap happens in one transaction, so readers see
 * the old registry until the new one is complete. When the download or the reading fails, or the file has no row for
 * Lima and Callao, the transaction rolls back and the previous registry stays as it was (S2-04).
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
    private static final String DELETE_REGISTRY = "DELETE FROM sunat_taxpayers";
    private static final String INSERT_REGISTRY = """
            INSERT INTO sunat_taxpayers (ruc, business_name, taxpayer_status, taxpayer_condition, loaded_at)
            SELECT DISTINCT ON (ruc) ruc, business_name, taxpayer_status, taxpayer_condition, ?
              FROM sunat_taxpayers_load
             ORDER BY ruc""";
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
        var last = jdbc.queryForObject("SELECT max(loaded_at) FROM sunat_taxpayers", Timestamp.class);
        return Optional.ofNullable(last).map(Timestamp::toInstant);
    }

    private void fetch(URI url, Path target) throws IOException, InterruptedException {
        var request = HttpRequest.newBuilder(url).timeout(properties.downloadTimeout()).GET().build();
        var response = httpClient.send(request, HttpResponse.BodyHandlers.ofFile(target));
        if (response.statusCode() != HTTP_OK) {
            throw new IOException("SUNAT answered HTTP " + response.statusCode());
        }
    }

    private SunatLoadReport replaceRegistry(String origin, InputStream input, boolean zipped, Instant started) {
        var report = transactions.execute(status -> {
            if (!Boolean.TRUE.equals(jdbc.queryForObject(TRY_LOCK, Boolean.class, LOAD_LOCK))) {
                LOGGER.info("sunat.registry.load.skipped reason=another-load-in-progress");
                return null;
            }
            jdbc.execute(CREATE_LOAD_TABLE);
            var counts = copyIntoLoadTable(input, zipped);
            if (counts.kept() == 0) {
                throw new SunatRegistryLoadException("The file has no taxpayer of Lima or Callao");
            }
            jdbc.update(DELETE_REGISTRY);
            var stored = jdbc.update(INSERT_REGISTRY, Timestamp.from(clock.instant()));
            return new SunatLoadReport(stored, counts.filtered(), counts.malformed(),
                    Duration.between(started, clock.instant()), false);
        });
        if (report == null) {
            return SunatLoadReport.skipped();
        }
        LOGGER.info("sunat.registry.loaded source={} rows={} filtered={} malformed={} durationMs={}", origin,
                report.rows(), report.filtered(), report.malformed(), report.duration().toMillis());
        return report;
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
