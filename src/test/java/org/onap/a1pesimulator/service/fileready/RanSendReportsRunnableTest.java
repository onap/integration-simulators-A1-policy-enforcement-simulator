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

package org.onap.a1pesimulator.service.fileready;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.onap.a1pesimulator.service.ue.RanUeHolder;
import org.onap.a1pesimulator.service.ves.RanCellEventCustomizer;
import org.onap.a1pesimulator.service.ves.RanEventCustomizerFactory;

class RanSendReportsRunnableTest extends CommonFileReady {

    private RanSendReportsRunnable ranSendReportsRunnable;

    @Mock
    RanFileReadyHolder ranFileReadyHolder;

    @Mock
    RanEventCustomizerFactory ranEventCustomizerFactory;

    @Mock
    RanUeHolder ranUeHolder;

    @BeforeEach
    void setUp() {
        super.setUp();
        doReturn(new RanCellEventCustomizer(ranUeHolder)).when(ranEventCustomizerFactory).getEventCustomizer(any(), any());
        ranSendReportsRunnable = spy(
                new RanSendReportsRunnable(ranFileReadyHolder));
    }

    @Test
    void successfulRun() {
        ranSendReportsRunnable.run();
        verify(ranFileReadyHolder, times(1)).createPMBulkFileAndSendFileReadyMessage();
    }
}