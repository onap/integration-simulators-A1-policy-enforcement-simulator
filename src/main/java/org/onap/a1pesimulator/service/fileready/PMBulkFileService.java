package org.onap.a1pesimulator.service.fileready;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static org.onap.a1pesimulator.util.Constants.EMPTY_STRING;
import static org.onap.a1pesimulator.util.Constants.TEMP_DIR;
import static org.onap.a1pesimulator.util.Convertors.YYYYMMDD_PATTERN;
import static org.onap.a1pesimulator.util.Convertors.truncateToSpecifiedMinutes;
import static org.onap.a1pesimulator.util.Convertors.zonedDateTimeToString;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.onap.a1pesimulator.data.fileready.EventMemoryHolder;
import org.onap.a1pesimulator.data.fileready.FileData;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;

/**
 * Service for PM Bulk File creation and handling
 */

@Service
public class PMBulkFileService {

    private static Map<String, AtomicInteger> uniqueFileNamesWithCount;

    /**
     * Generate PM Bulk File xml from stored events
     *
     * @param collectedEvents list of stored events
     * @return generated file in Mono object
     */
    public Mono<FileData> generatePMBulkFileXml(List<EventMemoryHolder> collectedEvents) {
        return Mono.just(FileData.builder().pmBulkFile(getXmlFile(collectedEvents)).build());
    }

    /**
     * Generate PM Bulk File and its name. Example: D20050907.1030+0000-20050909.1500+0000_DomainId_-_2
     *
     * @param collectedEvents list of stored events
     * @return newly created File
     */
    private static File getXmlFile(List<EventMemoryHolder> collectedEvents) {
        StringBuilder fileNameBuilder = new StringBuilder("D");
        ZonedDateTime firstEventTime = earliestEventTime(collectedEvents);
        ZonedDateTime lastEventTime = latestEventTime(collectedEvents);
        fileNameBuilder.append(zonedDateTimeToString(firstEventTime, YYYYMMDD_PATTERN)).append(".");
        fileNameBuilder.append(zonedDateTimeToString(truncateToSpecifiedMinutes(firstEventTime, 5), "HHmmZ")).append("-");
        fileNameBuilder.append(zonedDateTimeToString(lastEventTime, YYYYMMDD_PATTERN)).append(".");
        fileNameBuilder.append(zonedDateTimeToString(truncateToSpecifiedMinutes(lastEventTime, 5), "HHmmZ"));
        fileNameBuilder.append(appendRcIfNecessary(fileNameBuilder));
        fileNameBuilder.append(".xml");

        return new File(TEMP_DIR, fileNameBuilder.toString());
    }

    /**
     * The RC parameter is a running count and shall be appended only if the filename is otherwise not unique, i.e. more than one file is generated and all
     * other parameters of the file name are identical.
     *
     * @param fileNameBuilder stringBuilder which contains currently generated file name
     * @return sequence number or empty string
     */
    private static String appendRcIfNecessary(StringBuilder fileNameBuilder) {
        String fileName = fileNameBuilder.toString();
        int sequence = 0;
        if (isNull(uniqueFileNamesWithCount)) {
            uniqueFileNamesWithCount = Collections.synchronizedMap(new HashMap<>());
        }
        if (uniqueFileNamesWithCount.containsKey(fileName)) {
            sequence = uniqueFileNamesWithCount.get(fileName).incrementAndGet();
        } else {
            uniqueFileNamesWithCount.put(fileName, new AtomicInteger(0));
        }
        return sequence > 0 ? "_-_" + sequence : EMPTY_STRING;
    }

    /**
     * Get ZonedDateTime of the earliest event in that reporting period
     *
     * @param collectedEvents list of compared events
     * @return the earliest ZonedDateTime
     */
    private static ZonedDateTime earliestEventTime(List<EventMemoryHolder> collectedEvents) {
        return collectedEvents.stream()
                .map(EventMemoryHolder::getEventDate)
                .min(comparing(ZonedDateTime::toEpochSecond, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(ZonedDateTime.now());
    }

    /**
     * Get ZonedDateTime of the latest event in that reporting period
     *
     * @param collectedEvents list of compared events
     * @return the latest ZonedDateTime
     */
    private static ZonedDateTime latestEventTime(List<EventMemoryHolder> collectedEvents) {
        return collectedEvents.stream().map(EventMemoryHolder::getEventDate)
                .max(comparing(ZonedDateTime::toEpochSecond, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(ZonedDateTime.now());
    }
}
