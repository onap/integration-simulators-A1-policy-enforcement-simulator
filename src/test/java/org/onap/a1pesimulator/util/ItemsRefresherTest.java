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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.onap.a1pesimulator.service.cell.RanCellsHolder;
import org.onap.a1pesimulator.service.ue.RanUeHolder;

public class ItemsRefresherTest {

    private final RanCellsHolder cellsHolder = mock(RanCellsHolder.class);
    private final RanUeHolder ueHolder = mock(RanUeHolder.class);
    private final ItemsRefresher refresher = new ItemsRefresher(cellsHolder, ueHolder);

    @Test
    public void testHaveNotChanged() {
        // given
        when(cellsHolder.hasChanged()).thenReturn(false);
        when(ueHolder.hasChanged()).thenReturn(false);

        // when
        refresher.refresh();

        // then
        verify(cellsHolder).hasChanged();
        verify(cellsHolder, never()).refresh();
        verify(ueHolder).hasChanged();
        verify(ueHolder, never()).refresh();
    }

    @Test
    public void testOneHasChanged() {
        // given
        when(cellsHolder.hasChanged()).thenReturn(false);
        when(ueHolder.hasChanged()).thenReturn(true);

        // when
        refresher.refresh();

        // then
        verify(cellsHolder).hasChanged();
        verify(cellsHolder, never()).refresh();
        verify(ueHolder).hasChanged();
        verify(ueHolder).refresh();
    }

    @Test
    public void testBothHaveChanged() {
        // given
        when(cellsHolder.hasChanged()).thenReturn(true);
        when(ueHolder.hasChanged()).thenReturn(true);

        // when
        refresher.refresh();

        // then
        verify(cellsHolder).hasChanged();
        verify(cellsHolder).refresh();
        verify(ueHolder).hasChanged();
        verify(ueHolder).refresh();
    }
}
