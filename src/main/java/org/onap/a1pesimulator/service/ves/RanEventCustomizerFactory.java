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

package org.onap.a1pesimulator.service.ves;

import java.text.MessageFormat;
import org.onap.a1pesimulator.data.ves.Event;
import org.onap.a1pesimulator.service.ue.RanUeHolder;
import org.onap.a1pesimulator.service.ves.RanSendVesRunnable.EventCustomizer;
import org.springframework.stereotype.Component;

@Component
public class RanEventCustomizerFactory {

    private final EventCustomizer regularEventCustomizer;
    private final RanUeHolder ranUeHolder;

    public RanEventCustomizerFactory(EventCustomizer regularEventCustomizer, RanUeHolder ranUeHolder) {
        this.ranUeHolder = ranUeHolder;
        this.regularEventCustomizer = regularEventCustomizer;
    }

    public EventCustomizer getEventCustomizer(Event event, Mode mode) {
        switch (mode) {
            case REGULAR:
                return regularEventCustomizer;
            case FAILURE:
                return new RanCellFailureEventCustomizer(event, ranUeHolder);
            default:
                throw new RuntimeException(
                        MessageFormat.format("Cannot construct event customizer for mode: {0}", mode));
        }
    }

    public enum Mode {
        REGULAR, FAILURE
    }
}
