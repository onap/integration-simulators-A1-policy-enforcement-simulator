package org.onap.a1pesimulator.data.fileready;

import java.io.File;

import lombok.Builder;
import lombok.Data;

/**
 * File data object to stored File Ready Event, PM Bulk File and its archive
 */
@Data
@Builder
public class FileData {

    File pmBulkFile;
    File archivedPmBulkFile;
    FileReadyEvent fileReadyEvent;
}
