package alvin.docker.utils;

import alvin.docker.core.Context;
import org.apache.logging.log4j.util.Strings;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Pattern;

public class Times {
    private static final DateTimeFormatter STANDARD_DATETIME_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private static final DateTimeFormatter STANDARD_DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final Pattern STANDARD_DATE_PATTERN = Pattern.compile("^\\d{2,4}-\\d{1,2}-\\d{1,2}$");

    public LocalDateTime utcNow() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    public static Date utcToDate(LocalDateTime utc) {
        return Date.from(utc.atZone(ZoneOffset.UTC).toInstant());
    }

    public static LocalDateTime dateToUtc(Date date) {
        return LocalDateTime.ofInstant(date.toInstant(), ZoneOffset.UTC);
    }

    public static LocalDateTime utcToZone(LocalDateTime utc, ZoneId zoneId) {
        return utc.atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId).toLocalDateTime();
    }

    public static Instant toInstant(LocalDateTime dateTime) {
        return dateTime.toInstant(ZoneOffset.UTC);
    }

    public static Instant toInstant(LocalDateTime dateTime, ZoneId zoneId) {
        return dateTime.atZone(zoneId).toInstant();
    }

    public static LocalDateTime fromInstant(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZoneOffset.UTC);
    }

    public static String dateFormat(LocalDate date) {
        return date.format(Times.STANDARD_DATE_FORMATTER);
    }

    public static String dateFormat(LocalDateTime datetime) {
        return datetime.format(Times.STANDARD_DATE_FORMATTER);
    }

    public static ZonedDateTime parseDatetime(String datetimeStr, ZoneId zoneId) {
        return ZonedDateTime.of(LocalDateTime.parse(datetimeStr, STANDARD_DATETIME_FORMATTER), zoneId);
    }

    public static ZonedDateTime parseDate(String datetimeStr, ZoneId zoneId) {
        return ZonedDateTime.of(LocalDate.parse(datetimeStr, STANDARD_DATE_FORMATTER).atStartOfDay(), zoneId);
    }

    public static String datetimeFormat(LocalDateTime dateTime, Context context) {
        var formatter = STANDARD_DATETIME_FORMATTER;
        if (context != null && context.getI18n() != null) {
            var pattern = context.getI18n().getMessage("common.datetime.format");
            if (Strings.isEmpty(pattern)) {
                formatter = DateTimeFormatter.ofPattern(pattern);
            }
        }
        return dateTime.format(formatter);
    }

    public static boolean isStandardDate(String datetimeStr) {
        return STANDARD_DATE_PATTERN.matcher(datetimeStr).matches();
    }
}
