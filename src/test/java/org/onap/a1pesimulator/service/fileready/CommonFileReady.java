package org.onap.a1pesimulator.service.fileready;

import static org.onap.a1pesimulator.TestHelpers.deleteTempFiles;
import static org.onap.a1pesimulator.util.Constants.TEMP_DIR;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

public class CommonFileReady {

    public List<File> filesToDelete;  //we collect files created during testing and then delete them
    public static final String PM_BULK_FILE = "pmBulkFile.xml";
    public static final String ARCHIVED_PM_BULK_FILE = "pmBulkFile.xml.gz";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.initMocks(this);
        filesToDelete = Collections.synchronizedList(new ArrayList<>());
    }

    @AfterEach
    void cleanUpFiles() {
        deleteTempFiles(filesToDelete);
    }

    /**
     * Create temp file with simple text and adds it to filesToDelete list
     *
     * @param fileName name of file
     * @return created file
     */
    public File createTempFile(String fileName) {
        try {
            File tmpFile = new File(TEMP_DIR, fileName);
            tmpFile.createNewFile();
            Files.write(tmpFile.toPath(), "sample text".getBytes());
            filesToDelete.add(tmpFile);
            return tmpFile;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
