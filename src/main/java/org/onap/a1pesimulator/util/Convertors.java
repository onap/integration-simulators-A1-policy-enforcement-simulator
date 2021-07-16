package org.onap.a1pesimulator.util;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import lombok.experimental.UtilityClass;

@UtilityClass
public class Convertors {

    public static final String YYYYMMDD_PATTERN = "yyyyMMdd";
    public static final String ISO_8601_DATE = "yyyy-MM-dd'T'HH:mm:ssXXX";

    public static String zonedDateTimeToString(ZonedDateTime localDateTime) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(YYYYMMDD_PATTERN);
        return zonedDateTimeToString(localDateTime, formatter);
    }

    public static String zonedDateTimeToString(ZonedDateTime localDateTime, String pattern) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
        return zonedDateTimeToString(localDateTime, formatter);
    }

    public static String zonedDateTimeToString(ZonedDateTime localDateTime, DateTimeFormatter formatter) {
        return localDateTime.format(formatter);
    }

    public static ZonedDateTime truncateToSpecifiedMinutes(ZonedDateTime zonedDateTime, Integer minutes) {
        int minute = zonedDateTime.getMinute();
        int remainder = minute % minutes;
        return remainder != 0 ? zonedDateTime.withMinute(minute - remainder) : zonedDateTime;
    }
}
