package com.geopslabs.geops.identity.infrastructure.sunat;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.geopslabs.geops.identity.domain.models.Ruc;
import com.geopslabs.geops.identity.domain.models.Taxpayer;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.postgresql.ds.PGSimpleDataSource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The load against a real PostgreSQL: COPY into the temporary table, the Lima and Callao filter and the swap in one
 * transaction. SUNAT is a WireMock server; nothing is downloaded from the real site.
 */
class SunatRegistryLoaderTest {
    private static final DockerImageName POSTGIS_IMAGE =
            DockerImageName.parse("imresamu/postgis:16-3.4").asCompatibleSubstituteFor("postgres");
    private static final String SAMPLE = "classpath:sunat/padron-muestra.txt";
    private static final String ZIP_PATH = "/padron_reducido_ruc.zip";
    private static final long LIMA_AND_CALLAO_ROWS = 10;
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(POSTGIS_IMAGE);
    private static final WireMockServer SUNAT = new WireMockServer(wireMockConfig().dynamicPort());

    private static PGSimpleDataSource dataSource;
    private JdbcTemplate jdbc;
    private SunatRegistryLoader loader;
    private SunatTaxpayerRegistryAdapter registry;

    @BeforeAll
    static void startPostgresAndSunat() {
        POSTGRES.start();
        SUNAT.start();
        dataSource = new PGSimpleDataSource();
        dataSource.setUrl(POSTGRES.getJdbcUrl());
        dataSource.setUser(POSTGRES.getUsername());
        dataSource.setPassword(POSTGRES.getPassword());
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").load().migrate();
    }

    @AfterAll
    static void stopSunat() {
        SUNAT.stop();
    }

    @BeforeEach
    void emptyRegistry() {
        jdbc = new JdbcTemplate(dataSource);
        jdbc.update("DELETE FROM sunat_taxpayers");
        SUNAT.resetAll();
        loader = loader();
        registry = new SunatTaxpayerRegistryAdapter(jdbc);
    }

    @Test
    void loadsTheTenTaxpayersOfLimaAndCallaoFromTheSample() {
        var report = loader.loadFromResource(new DefaultResourceLoader().getResource(SAMPLE));

        assertThat(report.rows()).isEqualTo(LIMA_AND_CALLAO_ROWS);
        assertThat(report.filtered()).as("the taxpayer of Arequipa").isEqualTo(1);
        assertThat(report.malformed()).isEqualTo(1);
        assertThat(rows()).isEqualTo(LIMA_AND_CALLAO_ROWS);
        assertThat(registry.isLoaded()).isTrue();
        assertThat(registry.findByRuc(new Ruc("20333333333"))).isEmpty();
        assertThat(registry.findByRuc(new Ruc("20111111111")))
                .contains(new Taxpayer(new Ruc("20111111111"), "INVERSIONES EL OCASO E.I.R.L.", "BAJA DE OFICIO",
                        "HABIDO"));
        assertThat(registry.findByRuc(new Ruc("20601234572")).map(Taxpayer::businessName))
                .contains("PANADERÍA LA ESPAÑOLA S.R.L.");
        assertThat(loader.lastLoadedAt()).isPresent();
    }

    @Test
    void aBrokenFileKeepsThePreviousRegistry() {
        loader.loadFromResource(new DefaultResourceLoader().getResource(SAMPLE));
        var broken = new ByteArrayResource("PK\u0003\u0004not really a zip".getBytes(StandardCharsets.ISO_8859_1)) {
            @Override
            public String getFilename() {
                return "padron_reducido_ruc.zip";
            }
        };

        assertThatThrownBy(() -> loader.loadFromResource(broken)).isInstanceOf(SunatRegistryLoadException.class);

        assertThat(rows()).isEqualTo(LIMA_AND_CALLAO_ROWS);
    }

    @Test
    void aFileWithoutLimaOrCallaoKeepsThePreviousRegistry() {
        loader.loadFromResource(new DefaultResourceLoader().getResource(SAMPLE));
        var onlyArequipa = new ByteArrayResource(
                "20333333333|PICANTERIA|ACTIVO|HABIDO|040101|\n".getBytes(StandardCharsets.ISO_8859_1));

        assertThatThrownBy(() -> loader.loadFromResource(onlyArequipa))
                .isInstanceOf(SunatRegistryLoadException.class);

        assertThat(rows()).isEqualTo(LIMA_AND_CALLAO_ROWS);
    }

    @Test
    void downloadsTheZipAndReadsItAsAStream() throws IOException {
        SUNAT.stubFor(get(ZIP_PATH).willReturn(aResponse().withStatus(200).withBody(zippedSample())));

        var report = loader.loadFromUrl(URI.create(SUNAT.baseUrl() + ZIP_PATH));

        assertThat(report.rows()).isEqualTo(LIMA_AND_CALLAO_ROWS);
        assertThat(rows()).isEqualTo(LIMA_AND_CALLAO_ROWS);
    }

    @Test
    void aFailedDownloadKeepsThePreviousRegistry() {
        loader.loadFromResource(new DefaultResourceLoader().getResource(SAMPLE));
        SUNAT.stubFor(get(ZIP_PATH).willReturn(aResponse().withStatus(503)));

        assertThatThrownBy(() -> loader.loadFromUrl(URI.create(SUNAT.baseUrl() + ZIP_PATH)))
                .isInstanceOf(SunatRegistryLoadException.class);

        assertThat(rows()).isEqualTo(LIMA_AND_CALLAO_ROWS);
    }

    @Test
    void aSecondLoadReplacesTheRegistryInsteadOfAddingToIt() {
        loader.loadFromResource(new DefaultResourceLoader().getResource(SAMPLE));
        var newRegistry = new ByteArrayResource(
                "20123456789|RESTAURANTE DON PEPE S.A.C.|BAJA DE OFICIO|HABIDO|150122|\n"
                        .getBytes(StandardCharsets.ISO_8859_1));

        loader.loadFromResource(newRegistry);

        assertThat(rows()).isEqualTo(1);
        assertThat(registry.findByRuc(new Ruc("20123456789")).map(Taxpayer::status)).contains("BAJA DE OFICIO");
    }

    private long rows() {
        return jdbc.queryForObject("SELECT count(*) FROM sunat_taxpayers", Long.class);
    }

    private static SunatRegistryLoader loader() {
        var properties = new SunatProperties(SunatProperties.Source.FILE, URI.create("http://localhost"), SAMPLE,
                List.of("1501", "0701"), StandardCharsets.ISO_8859_1, "|",
                new SunatProperties.Columns(0, 1, 2, 3, 4), Duration.ofHours(24), Duration.ofHours(1),
                Duration.ofSeconds(2), Duration.ofSeconds(10));
        return new SunatRegistryLoader(dataSource, new TransactionTemplate(new DataSourceTransactionManager(dataSource)),
                new DefaultResourceLoader(), properties, Clock.systemUTC());
    }

    private static byte[] zippedSample() throws IOException {
        var bytes = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(bytes);
             InputStream sample = SunatRegistryLoaderTest.class.getResourceAsStream("/sunat/padron-muestra.txt")) {
            zip.putNextEntry(new ZipEntry("padron_reducido_ruc.txt"));
            sample.transferTo(zip);
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
