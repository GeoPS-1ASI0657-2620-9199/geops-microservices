package com.geopslabs.geops.identity.infrastructure.sunat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * Reads the reduced registry line by line, without keeping it in memory. A line without the expected columns, with
 * a RUC that is not 11 digits or with an empty field is discarded and counted. A first line that does not start
 * with a RUC is the header and is skipped.
 */
public class SunatPadronParser {
    private static final Pattern RUC = Pattern.compile("\\d{11}");
    private static final int KEEP_TRAILING_EMPTY_FIELDS = -1;

    private final SunatProperties.Columns columns;
    private final Pattern separator;
    private final SunatProperties properties;

    public SunatPadronParser(SunatProperties properties) {
        this.properties = properties;
        this.columns = properties.columns();
        this.separator = Pattern.compile(Pattern.quote(properties.separator()));
    }

    public ParseResult parse(InputStream input, Consumer<SunatTaxpayerLine> consumer) throws IOException {
        var reader = new BufferedReader(new InputStreamReader(input, properties.charset()));
        long read = 0;
        long malformed = 0;
        var firstLine = true;
        for (var line = reader.readLine(); line != null; line = reader.readLine()) {
            if (line.isBlank()) {
                continue;
            }
            var fields = separator.split(line, KEEP_TRAILING_EMPTY_FIELDS);
            if (firstLine && isHeader(fields)) {
                firstLine = false;
                continue;
            }
            firstLine = false;
            var parsed = toLine(fields);
            if (parsed == null) {
                malformed++;
                continue;
            }
            read++;
            consumer.accept(parsed);
        }
        return new ParseResult(read, malformed);
    }

    private boolean isHeader(String[] fields) {
        return fields.length <= columns.ruc() || !RUC.matcher(fields[columns.ruc()].strip()).matches();
    }

    private SunatTaxpayerLine toLine(String[] fields) {
        if (fields.length <= columns.highest()) {
            return null;
        }
        var ruc = fields[columns.ruc()].strip();
        var businessName = fields[columns.businessName()].strip();
        var status = fields[columns.status()].strip();
        var condition = fields[columns.condition()].strip();
        var ubigeo = fields[columns.ubigeo()].strip();
        if (!RUC.matcher(ruc).matches() || businessName.isEmpty() || status.isEmpty() || condition.isEmpty()
                || ubigeo.isEmpty()) {
            return null;
        }
        return new SunatTaxpayerLine(ruc, businessName, status, condition, ubigeo);
    }

    /** Lines turned into taxpayers and lines discarded as malformed. */
    public record ParseResult(long read, long malformed) {
    }
}
