package com.geopslabs.geops.identity.infrastructure.sunat;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.net.URI;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.List;

/**
 * geops.sunat.*: where the reduced registry comes from and how its lines are read. The column positions live only
 * here (geops.sunat.columns.*), so a change in SUNAT's layout is a configuration change.
 *
 * @param url            https only; the column order was checked against the header of this file on 2026-10-10
 * @param source         url downloads the zip; file reads a local file or classpath resource (tests and demo)
 * @param file           Spring resource location of the file, plain text or zip
 * @param ubigeoPrefixes only these districts are kept: Lima (1501) and Callao (0701)
 * @param maxAge         at startup the registry is loaded again when the last load is older than this
 * @param retryDelay     wait before another attempt when a load fails
 */
@ConfigurationProperties(prefix = "geops.sunat")
public record SunatProperties(@DefaultValue("file") Source source,
                              @DefaultValue("https://www2.sunat.gob.pe/padron_reducido_ruc.zip") URI url,
                              @DefaultValue("classpath:sunat/padron-muestra.txt") String file,
                              @DefaultValue({"1501", "0701"}) List<String> ubigeoPrefixes,
                              @DefaultValue("ISO-8859-1") Charset charset,
                              @DefaultValue("|") String separator,
                              @DefaultValue Columns columns,
                              @DefaultValue("24h") Duration maxAge,
                              @DefaultValue("1h") Duration retryDelay,
                              @DefaultValue("30s") Duration connectTimeout,
                              @DefaultValue("30m") Duration downloadTimeout) {
    private static final String HTTPS = "https";

    /** The registry is downloaded only over https, so nobody on the way can change who is active or habido. */
    public SunatProperties {
        if (url == null || !HTTPS.equalsIgnoreCase(url.getScheme())) {
            throw new IllegalArgumentException("geops.sunat.url must be an https URL, got " + url);
        }
    }

    public enum Source {
        URL,
        FILE
    }

    /** Zero-based position of each field in a line of the registry. */
    public record Columns(@DefaultValue("0") int ruc,
                          @DefaultValue("1") int businessName,
                          @DefaultValue("2") int status,
                          @DefaultValue("3") int condition,
                          @DefaultValue("4") int ubigeo) {

        int highest() {
            return Math.max(Math.max(Math.max(ruc, businessName), Math.max(status, condition)), ubigeo);
        }
    }
}
