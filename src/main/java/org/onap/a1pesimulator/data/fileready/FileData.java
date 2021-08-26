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

package org.onap.a1pesimulator.data.fileready;

import java.io.File;
import java.time.ZonedDateTime;

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
    ZonedDateTime startEventDate;
    ZonedDateTime endEventDate;
}
