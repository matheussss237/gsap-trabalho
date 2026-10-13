package br.com.projeto.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Formatação de datas para a interface. */
public final class DateFormat {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private DateFormat() {
    }

    /** "14:32" para hoje, "12/10/2026 14:32" para outros dias. */
    public static String smart(Instant instant) {
        if (instant == null) {
            return "";
        }
        var zoned = instant.atZone(ZoneId.systemDefault());
        return zoned.toLocalDate().equals(LocalDate.now()) ? TIME.format(zoned) : DATE_TIME.format(zoned);
    }
}
