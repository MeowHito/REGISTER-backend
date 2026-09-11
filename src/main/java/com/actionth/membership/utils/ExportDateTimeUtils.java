package com.actionth.membership.utils;

import java.sql.Timestamp;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public final class ExportDateTimeUtils {

    public static final ZoneId BANGKOK_ZONE = ZoneId.of("Asia/Bangkok");

    private static final DateTimeFormatter DISPLAY_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter SHORT_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter ISO_INPUT_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter ISO_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private ExportDateTimeUtils() {
    }

    public static OffsetDateTime toBangkok(OffsetDateTime dateTime) {
        return dateTime == null ? null : dateTime.atZoneSameInstant(BANGKOK_ZONE).toOffsetDateTime();
    }

    public static String toBangkokIsoString(OffsetDateTime dateTime) {
        OffsetDateTime bangkok = toBangkok(dateTime);
        return bangkok == null ? "" : bangkok.toString();
    }

    public static String toBangkokDateString(OffsetDateTime dateTime) {
        OffsetDateTime bangkok = toBangkok(dateTime);
        return bangkok == null ? "" : bangkok.format(ISO_DATE_FORMATTER);
    }

    public static String formatDisplay(Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof OffsetDateTime odt) {
            return toBangkok(odt).format(DISPLAY_FORMATTER);
        }
        if (value instanceof Timestamp tm) {
            return tm.toInstant().atZone(BANGKOK_ZONE).format(DISPLAY_FORMATTER);
        }
        if (value instanceof String str) {
            try {
                OffsetDateTime parsed = OffsetDateTime.parse(str, ISO_INPUT_FORMATTER);
                return toBangkok(parsed).format(DISPLAY_FORMATTER);
            } catch (Exception ignore) {
                return value.toString();
            }
        }
        return value.toString();
    }

    public static String formatShort(OffsetDateTime dateTime) {
        OffsetDateTime bangkok = toBangkok(dateTime);
        return bangkok == null ? null : bangkok.format(SHORT_FORMATTER);
    }
}
