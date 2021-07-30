package org.onap.a1pesimulator.service.fileready;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.onap.a1pesimulator.data.ves.VesEvent;
import org.onap.a1pesimulator.exception.VesBrokerException;
import org.onap.a1pesimulator.service.ue.RanUeHolder;
import org.onap.a1pesimulator.service.ves.RanCellEventCustomizer;
import org.onap.a1pesimulator.service.ves.RanEventCustomizerFactory;
import org.onap.a1pesimulator.service.ves.RanEventCustomizerFactory.Mode;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class RanSaveFileReadyRunnableTest extends CommonFileReady {

    private RanSaveFileReadyRunnable ranSaveFileReadyRunnable;

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
        ranSaveFileReadyRunnable = spy(
                new RanSaveFileReadyRunnable(ranFileReadyHolder, TEST_CELL_ID, loadEventFromFile(), ranEventCustomizerFactory.getEventCustomizer(new VesEvent(),
                        Mode.REGULAR), 60, Collections.emptyList()));
    }

    @Test
    void successfulRun() throws VesBrokerException {
        ranSaveFileReadyRunnable.run();
        verify(ranFileReadyHolder, times(1)).saveEventToMemory(any(), any(), any(), any());
    }

    @Test
    void errorRun() throws VesBrokerException {
        ListAppender<ILoggingEvent> appender = createCommonLog(RanSaveFileReadyRunnable.class);
        doThrow(new VesBrokerException("error")).when(ranFileReadyHolder).saveEventToMemory(any(), any(), any(), any());
        ranSaveFileReadyRunnable.run();
        assertThat(appender.list).extracting(ILoggingEvent::getFormattedMessage).containsExactly("Saving file ready event failed: error");
    }
}