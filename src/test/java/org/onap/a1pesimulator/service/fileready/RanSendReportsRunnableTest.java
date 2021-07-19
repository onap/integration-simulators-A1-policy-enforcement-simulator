package org.onap.a1pesimulator.service.fileready;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.service.ue.RanUeHolder;
import org.onap.a1pesimulator.service.ves.RanCellEventCustomizer;
import org.onap.a1pesimulator.service.ves.RanEventCustomizerFactory;
import org.onap.a1pesimulator.service.ves.RanEventCustomizerFactory.Mode;

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
                new RanSendReportsRunnable(ranFileReadyHolder, loadEventFromFile(), ranEventCustomizerFactory.getEventCustomizer(new VesEvent(), Mode.REGULAR),
                        Collections.emptyList()));
    }

    @Test
    void successfulRun() {
        ranSendReportsRunnable.run();
        verify(ranFileReadyHolder, times(1)).createPMBulkFileAndSendFileReadyMessage();
    }
}