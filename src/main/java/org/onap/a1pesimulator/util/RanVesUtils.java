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

import java.text.MessageFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import org.onap.a1pesimulator.data.ue.UserEquipment;
import org.onap.a1pesimulator.data.ves.CommonEventHeader;
import org.onap.a1pesimulator.data.ves.Event;
import org.onap.a1pesimulator.data.ves.MeasurementFields.AdditionalMeasurement;
import org.onap.a1pesimulator.service.ue.RanUeHolder;

public class RanVesUtils {

    private static final String PATTERN_DIGIT = "\\d+";
    private static final String PATTERN_SPLIT_RANDOM = "-";
    private static final String PATTERN_SPLIT_TRENDING = "->";
    private static final String MARKER_START = "[[";
    private static final String MARKER_END = "]]";
    private static final String PATTERN_MARKER_START = "\\[\\[";
    private static final String PATTERN_MARKER_END = "\\]\\]";

    private static final String UE_PARAM_TRAFFIC_MODEL = "trafficModel";
    private static final int TEN_MINUTES_MICROSECONDS = 10 * 60 * 1000_000;

    private static final Random random = new Random();

    private RanVesUtils() {
    }

    public static void updateHeader(Event event) {
        CommonEventHeader commonEventHeader = event.getCommonEventHeader();
        commonEventHeader.setLastEpochMicrosec(ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now()));
        commonEventHeader.setStartEpochMicrosec(getStartEpochMicroseconds());
    }

    public static AdditionalMeasurement buildTrafficModelMeasurement(AdditionalMeasurement identityMeasurement,
            RanUeHolder ranUeHolder, String valuePattern) {
        String cellId = identityMeasurement.getHashMap().get(Constants.MEASUREMENT_FIELD_VALUE);
        AdditionalMeasurement trafficModel = new AdditionalMeasurement();
        Map<String, String> hashMap = new HashMap<>();
        trafficModel.setName(UE_PARAM_TRAFFIC_MODEL);
        trafficModel.setHashMap(hashMap);
        Collection<UserEquipment> cellUes = ranUeHolder.getUserEquipmentsConnectedToCell(cellId);
        cellUes.stream().map(UserEquipment::getId).forEach(ueId -> hashMap.put(ueId, valuePattern));

        return trafficModel;
    }

    public static List<AdditionalMeasurement> randomizeAdditionalMeasurements(
            Collection<AdditionalMeasurement> toRandomize) {
        return toRandomize.stream().map(measurement -> transformAdditionalMeasurementValues(measurement,
                RanVesUtils::randomizeValue)).collect(Collectors.toList());
    }

    public static List<AdditionalMeasurement> setLowRangeValues(List<AdditionalMeasurement> toUpdateMeasurements) {
        return toUpdateMeasurements.stream().map(measurement -> transformAdditionalMeasurementValues(measurement,
                RanVesUtils::getLowRangeValue)).collect(Collectors.toList());
    }

    private static AdditionalMeasurement transformAdditionalMeasurementValues(AdditionalMeasurement measurement,
            UnaryOperator<String> transformAction) {
        AdditionalMeasurement randomizedMeasurement = new AdditionalMeasurement();
        randomizedMeasurement.setName(measurement.getName());
        randomizedMeasurement.setHashMap(transformValues(measurement.getHashMap(), transformAction));
        return randomizedMeasurement;
    }

    private static Map<String, String> transformValues(Map<String, String> values,
            UnaryOperator<String> transformAction) {
        Map<String, String> randomizedMap = new HashMap<>(values.size());
        values.forEach((key, value) -> randomizedMap.put(key, transformAction.apply(value)));
        return randomizedMap;
    }

    private static String randomizeValue(String value) {
        if (!isRange(value)) {
            return value;
        }
        String toRandomize = value.substring(MARKER_START.length(), value.length() - MARKER_END.length());
        String[] ranges = toRandomize.split(PATTERN_SPLIT_RANDOM);
        int randomNumber = getRandomNumber(parseInt(ranges[0]), parseInt(ranges[1]));
        return String.valueOf(randomNumber);
    }

    private static String getLowRangeValue(String value) {
        if (!isRange(value)) {
            return value;
        }
        String toRandomize = value.substring(MARKER_START.length(), value.length() - MARKER_END.length());
        String[] ranges = toRandomize.split(PATTERN_SPLIT_RANDOM);
        return String.valueOf(ranges[0]);
    }

    private static Long getStartEpochMicroseconds() {
        long epochMicrosecondsNow = ChronoUnit.MICROS.between(Instant.EPOCH, Instant.now());
        long lowest10minInterval = epochMicrosecondsNow - epochMicrosecondsNow % TEN_MINUTES_MICROSECONDS;
        long highest10minInterval = lowest10minInterval + TEN_MINUTES_MICROSECONDS;

        if ((epochMicrosecondsNow - lowest10minInterval) < (highest10minInterval - epochMicrosecondsNow)) {
            return lowest10minInterval;
        } else {
            return highest10minInterval;
        }
    }

    public static int getRandomNumber(int min, int max) {
        return random.nextInt(max - min) + min;
    }

    private static int parseInt(String strNum) {
        try {
            return Integer.parseInt(strNum);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException(MessageFormat.format("Cannot parse int for value: {0}", strNum), nfe);
        }
    }

    public static boolean isRange(String value) {
        return value.startsWith(MARKER_START) && value.endsWith(MARKER_END);
    }

    private static boolean isRange(String value, String splitPattern) {
        String pattern = PATTERN_MARKER_START + PATTERN_DIGIT + splitPattern + PATTERN_DIGIT + PATTERN_MARKER_END;
        return value.matches(pattern);
    }

    public static boolean isRandomRange(String value) {
        return isRange(value, PATTERN_SPLIT_RANDOM);
    }

    public static boolean isTrandingRange(String value) {
        return isRange(value, PATTERN_SPLIT_TRENDING);
    }

    public static String[] splitRandomRange(String value) {
        return splitRange(value, PATTERN_SPLIT_RANDOM);
    }

    public static String[] splitTrendingRange(String value) {
        return splitRange(value, PATTERN_SPLIT_TRENDING);
    }

    private static String[] splitRange(String value, String splitPattern) {
        String truncated = value.substring(MARKER_START.length(), value.length() - MARKER_END.length());
        return truncated.split(splitPattern);
    }
}
