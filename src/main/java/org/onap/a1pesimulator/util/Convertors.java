/*
 * Copyright (C) 2021 Samsung Electronics
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License
 */

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
