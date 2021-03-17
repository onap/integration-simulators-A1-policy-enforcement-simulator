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

public class DistanceCalculator {

    private static final int EARTH_RADIUS = 6371; // Earth radius in KM

    private DistanceCalculator() {
    }

    /**
     * Calculate distance in KM using Haversine formula
     */
    public static double calculate(double startLat, double startLong, double endLat, double endLong) {
        return haversine(startLat, startLong, endLat, endLong);
    }

    public static boolean isInRange(double startLat, double startLong, double endLat, double endLong, double range) {
        double distance = calculate(startLat, startLong, endLat, endLong);
        return distance < range;
    }

    private static double haversine(double startLat, double startLong, double endLat, double endLong) {
        double dLat = Math.toRadians(endLat - startLat);
        double dLon = Math.toRadians(endLong - startLong);
        double startLatInRadians = Math.toRadians(startLat);
        double endLatInRadians = Math.toRadians(endLat);

        double a = Math.pow(Math.sin(dLat / 2), 2) + Math.pow(Math.sin(dLon / 2), 2) * Math.cos(startLatInRadians)
                                                             * Math.cos(endLatInRadians);
        double c = 2 * Math.asin(Math.sqrt(a));
        return EARTH_RADIUS * c;
    }
}
