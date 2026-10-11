package com.geopslabs.geops.identity.infrastructure.sunat;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SunatPadronParserTest {
    private static final String SAMPLE = "/sunat/padron-muestra.txt";

    @Test
    void readsTheSampleSkippingTheHeaderAndCountingTheMalformedLine() throws IOException {
        var lines = new ArrayList<SunatTaxpayerLine>();

        try (var sample = sample()) {
            var result = parser(new SunatProperties.Columns(0, 1, 2, 3, 4), "|").parse(sample, lines::add);

            assertThat(result.read()).isEqualTo(11);
            assertThat(result.malformed()).isEqualTo(1);
        }
        assertThat(lines.get(0)).isEqualTo(new SunatTaxpayerLine("20123456786", "RESTAURANTE DON PEPE S.A.C.",
                "ACTIVO", "HABIDO", "150122"));
    }

    @Test
    void decodesTheRegistryAsIso88591() throws IOException {
        var lines = new ArrayList<SunatTaxpayerLine>();

        try (var sample = sample()) {
            parser(new SunatProperties.Columns(0, 1, 2, 3, 4), "|").parse(sample, lines::add);
        }

        assertThat(lines).extracting(SunatTaxpayerLine::businessName).contains("PANADERÍA LA ESPAÑOLA S.R.L.");
    }

    @Test
    void takesEachFieldFromTheConfiguredColumn() throws IOException {
        var text = "150101;ACTIVO;HABIDO;20123456786;BODEGA REORDENADA\n";
        var lines = new ArrayList<SunatTaxpayerLine>();

        parser(new SunatProperties.Columns(3, 4, 1, 2, 0), ";").parse(latin1(text), lines::add);

        assertThat(lines).containsExactly(new SunatTaxpayerLine("20123456786", "BODEGA REORDENADA", "ACTIVO",
                "HABIDO", "150101"));
    }

    @Test
    void discardsShortLinesBadRucsAndEmptyFields() throws IOException {
        var text = String.join("\n",
                "20123456786|SOLO DOS",
                "2012345678X|RUC CON LETRA|ACTIVO|HABIDO|150101|",
                "20123456786||ACTIVO|HABIDO|150101|",
                "20123456786|BIEN FORMADA|ACTIVO|HABIDO|150101|") + "\n";
        var lines = new ArrayList<SunatTaxpayerLine>();

        var result = parser(new SunatProperties.Columns(0, 1, 2, 3, 4), "|").parse(latin1(text), lines::add);

        assertThat(result.malformed()).isEqualTo(3);
        assertThat(lines).extracting(SunatTaxpayerLine::businessName).containsExactly("BIEN FORMADA");
    }

    @Test
    void keepsOnlyTheConfiguredUbigeoPrefixes() {
        var lima = new SunatTaxpayerLine("20123456786", "A", "ACTIVO", "HABIDO", "150122");
        var callao = new SunatTaxpayerLine("20123456786", "A", "ACTIVO", "HABIDO", "070101");
        var arequipa = new SunatTaxpayerLine("20123456786", "A", "ACTIVO", "HABIDO", "040101");

        assertThat(lima.isInAnyOf(List.of("1501", "0701"))).isTrue();
        assertThat(callao.isInAnyOf(List.of("1501", "0701"))).isTrue();
        assertThat(arequipa.isInAnyOf(List.of("1501", "0701"))).isFalse();
    }

    private static SunatPadronParser parser(SunatProperties.Columns columns, String separator) {
        return new SunatPadronParser(new SunatProperties(SunatProperties.Source.FILE, URI.create("http://localhost"),
                "classpath:x", List.of("1501", "0701"), StandardCharsets.ISO_8859_1, separator, columns,
                Duration.ofHours(24), Duration.ofHours(1), Duration.ofSeconds(1), Duration.ofSeconds(1)));
    }

    private static InputStream sample() {
        return SunatPadronParserTest.class.getResourceAsStream(SAMPLE);
    }

    private static InputStream latin1(String text) {
        return new ByteArrayInputStream(text.getBytes(StandardCharsets.ISO_8859_1));
    }
}
