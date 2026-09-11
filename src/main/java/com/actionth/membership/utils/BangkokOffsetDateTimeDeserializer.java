package com.actionth.membership.utils;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

public class BangkokOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

    private static final List<DateTimeFormatter> LOCAL_DATETIME_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

    private static final List<DateTimeFormatter> LOCAL_DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/yyyy"));

    @Override
    public OffsetDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        String raw = parser.getText();
        if (raw == null) {
            return null;
        }
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) {
            return null;
        }

        try {
            return OffsetDateTime.parse(trimmed);
        } catch (DateTimeParseException ignore) {
        }

        for (DateTimeFormatter fmt : LOCAL_DATETIME_FORMATTERS) {
            try {
                LocalDateTime ldt = LocalDateTime.parse(trimmed, fmt);
                return ldt.atZone(ExportDateTimeUtils.BANGKOK_ZONE).toOffsetDateTime();
            } catch (DateTimeParseException ignore) {
            }
        }

        for (DateTimeFormatter fmt : LOCAL_DATE_FORMATTERS) {
            try {
                LocalDate ld = LocalDate.parse(trimmed, fmt);
                return ld.atStartOfDay(ExportDateTimeUtils.BANGKOK_ZONE).toOffsetDateTime();
            } catch (DateTimeParseException ignore) {
            }
        }

        throw new IOException("Unsupported date-time format: " + raw);
    }
}
