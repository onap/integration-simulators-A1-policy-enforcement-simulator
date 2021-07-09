package org.onap.a1pesimulator.service.fileready;

import static java.util.Comparator.comparing;
import static java.util.Objects.isNull;
import static org.onap.a1pesimulator.util.Constants.EMPTY_STRING;
import static org.onap.a1pesimulator.util.Constants.MEASUREMENT_FIELD_IDENTIFIER;
import static org.onap.a1pesimulator.util.Constants.MEASUREMENT_FIELD_VALUE;
import static org.onap.a1pesimulator.util.Constants.TEMP_DIR;
import static org.onap.a1pesimulator.util.Convertors.ISO_8601_DATE;
import static org.onap.a1pesimulator.util.Convertors.YYYYMMDD_PATTERN;
import static org.onap.a1pesimulator.util.Convertors.truncateToSpecifiedMinutes;
import static org.onap.a1pesimulator.util.Convertors.zonedDateTimeToString;
import static org.onap.a1pesimulator.util.RanVesUtils.UE_PARAM_TRAFFIC_MODEL;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.onap.a1pesimulator.data.fileready.EventMemoryHolder;
import org.onap.a1pesimulator.data.fileready.FileData;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.util.VnfConfigReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import reactor.core.publisher.Mono;

/**
 * Service for PM Bulk File creation and handling
 */

@Service
public class PMBulkFileService {

    private static final Logger log = LoggerFactory.getLogger(PMBulkFileService.class);
    private static Map<String, AtomicInteger> uniqueFileNamesWithCount;
    private final VnfConfigReader vnfConfigReader;

    @Value("${xml.pm.bulk.fileFormatVersion}")
    private String fileFormatVersion;

    @Value("${xml.pm.bulk.vendorName}")
    private String vendorName;

    @Value("${xml.pm.bulk.fileSender}")
    private String fileSenderValue;

    @Value("${xml.pm.bulk.userLabel}")
    private String userLabel;

    public PMBulkFileService(VnfConfigReader vnfConfigReader) {
        this.vnfConfigReader = vnfConfigReader;
    }

    /**
     * Generate PM Bulk File xml from stored events
     *
     * @param collectedEvents list of stored events
     * @return generated file in Mono object
     */
    public Mono<FileData> generatePMBulkFileXml(List<EventMemoryHolder> collectedEvents) {

        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();

            //root elements
            Document doc = docBuilder.newDocument();

            Element measCollecFile = doc.createElement("measCollecFile");
            doc.appendChild(measCollecFile);
            measCollecFile.setAttribute("xmlns", "http://www.3gpp.org/ftp/specs/archive/32_series/32.435#measCollec");
            measCollecFile.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            measCollecFile.setAttribute("xsi:schemaLocation",
                    "http://www.3gpp.org/ftp/specs/archive/32_series/32.435#measCollec http://www.3gpp.org/ftp/specs/archive/32_series/32.435#measCollec");

            //fileHeader elements
            Element fileHeader = doc.createElement("fileHeader");
            measCollecFile.appendChild(fileHeader);
            fileHeader.setAttribute("fileFormatVersion", fileFormatVersion);
            fileHeader.setAttribute("vendorName", vendorName);

            //fileSender elements
            Element fileSender = doc.createElement("fileSender");
            fileHeader.appendChild(fileSender);
            fileSender.setAttribute("elementType", fileSenderValue);

            //measCollec elements
            Element measCollec = doc.createElement("measCollec");
            fileHeader.appendChild(measCollec);
            measCollec.setAttribute("beginTime", zonedDateTimeToString(earliestEventTime(collectedEvents), ISO_8601_DATE));

            //measData elements
            Element measData = doc.createElement("measData");
            measCollecFile.appendChild(measData);

            //managedElement elements
            Element managedElement = doc.createElement("managedElement");
            measData.appendChild(managedElement);
            managedElement.setAttribute("userLabel", userLabel);

            //add measInfo elements
            addMeansInfo(doc, measData, collectedEvents);

            //fileFooter elements
            Element fileFooter = doc.createElement("fileFooter");
            measCollecFile.appendChild(fileFooter);

            Element measCollecFooter = doc.createElement("measCollec");
            fileFooter.appendChild(measCollecFooter);
            measCollecFooter.setAttribute("endTime", zonedDateTimeToString(latestEventTime(collectedEvents), ISO_8601_DATE));

            File xmlFile = writeDocumentIntoXmlFile(doc, collectedEvents);

            log.trace("Removing all VES events from memory: {}", collectedEvents.size());
            collectedEvents.clear();
            return Mono.just(FileData.builder().pmBulkFile(xmlFile).build());

        } catch (ParserConfigurationException | TransformerException pce) {
            log.error("Error occurs while creating PM Bulk File", pce);
            return Mono.empty();
        }
    }

    /**
     * Add measurement elements for each cell and measurement time into PM Bulk File
     *
     * @param doc Document
     * @param measData main element of document, which stores meansData
     * @param collectedEvents list of stored events
     */
    private void addMeansInfo(Document doc, Element measData, List<EventMemoryHolder> collectedEvents) {
        collectedEvents.stream().sorted(comparing(EventMemoryHolder::getEventDate)).forEach(eventMemoryHolder -> {
            VesEvent event = eventMemoryHolder.getEvent();

            Element measInfo = doc.createElement("measInfo");
            measData.appendChild(measInfo);

            //job element
            Element job = doc.createElement("job");
            measInfo.appendChild(job);
            job.setAttribute("jobId", eventMemoryHolder.getJobId());

            //granPeriod elements
            Element granPeriod = doc.createElement("granPeriod");
            measInfo.appendChild(granPeriod);
            granPeriod.setAttribute("duration", getDurationString(eventMemoryHolder.getGranPeriod()));
            ZonedDateTime endDate = eventMemoryHolder.getEventDate();
            granPeriod.setAttribute("endTime", zonedDateTimeToString(endDate, ISO_8601_DATE));

            //repPeriod elements
            Element repPeriod = doc.createElement("repPeriod");
            measInfo.appendChild(repPeriod);
            repPeriod.setAttribute("duration", getDurationString(vnfConfigReader.getVnfConfig().getRepPeriod()));

            //measType definition
            HashMap<String, String> measurmentMap = new HashMap<>();
            AtomicInteger i = new AtomicInteger(1);
            event.getMeasurementFields().getAdditionalMeasurements().forEach(additionalMeasurement -> {
                if (Stream.of(UE_PARAM_TRAFFIC_MODEL, MEASUREMENT_FIELD_IDENTIFIER)
                        .noneMatch(elementName -> elementName.equalsIgnoreCase(additionalMeasurement.getName()))) {
                    Element measType = doc.createElement("measType");
                    measInfo.appendChild(measType);
                    measType.setAttribute("p", String.valueOf(i));
                    measType.setTextContent(additionalMeasurement.getName());
                    measurmentMap.put(additionalMeasurement.getName(), String.valueOf(i));
                    i.incrementAndGet();
                }
            });

            //measValue elements
            Element measValue = doc.createElement("measValue");
            measInfo.appendChild(measValue);
            measValue.setAttribute("measObjLdn", eventMemoryHolder.getCellId());
            event.getMeasurementFields().getAdditionalMeasurements().stream()
                    .filter(additionalMeasurement -> measurmentMap.containsKey(additionalMeasurement.getName())).forEach(additionalMeasurement -> {

                //r elements
                Element r = doc.createElement("r");
                measValue.appendChild(r);
                r.setAttribute("p", measurmentMap.get(additionalMeasurement.getName()));
                r.setTextContent(additionalMeasurement.getHashMap().get(MEASUREMENT_FIELD_VALUE));
            });
        });
    }

    /**
     * Converts Document into XML file and adds proper headers
     *
     * @param doc Document
     * @param collectedEvents list of stored events
     * @return newly created File in xml format
     */
    private File writeDocumentIntoXmlFile(Document doc, List<EventMemoryHolder> collectedEvents) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
        transformerFactory.setAttribute(XMLConstants.ACCESS_EXTERNAL_STYLESHEET, "");

        Transformer tr = transformerFactory.newTransformer();
        tr.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        tr.setOutputProperty(OutputKeys.VERSION, "1.0");
        Node pi = doc.createProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"MeasDataCollection.xsl\"");
        doc.insertBefore(pi, doc.getDocumentElement());

        File xmlFile = getXmlFile(collectedEvents);
        StreamResult result = new StreamResult(xmlFile);
        DOMSource source = new DOMSource(doc);
        tr.transform(source, result);
        return xmlFile;
    }

    /**
     * Generate PM Bulk File and its name
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

    /**
     * Convert duration interval in seconds to xml element required by the specification Examples: PT10S, PT900S
     *
     * @param interval interval in seconds
     * @return duration xml element representation
     */
    private static String getDurationString(int interval) {
        return "PT" + interval + "S";
    }
}
