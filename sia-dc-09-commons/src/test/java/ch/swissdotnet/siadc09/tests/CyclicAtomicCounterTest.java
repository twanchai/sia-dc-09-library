/*
 * Copyright (c) 2025 Swissdotnet SA
 *
 * This file is part of the SIA-DC-09 Library project.
 *
 * This source code is dual-licensed:
 * 1. Non-commercial use is permitted under the Polyform Noncommercial License 1.0.0
 *    https://polyformproject.org/licenses/noncommercial/1.0.0/
 * 2. Commercial use requires a separate commercial license.
 *    To inquire about licensing, please contact: info@swissdotnet.ch
 *
 * Unless required by applicable law or agreed to in writing, this software
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND.
 */
package ch.swissdotnet.siadc09.tests;

import ch.swissdotnet.siadc09.CyclicAtomicCounter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class CyclicAtomicCounterTest {

    @Test
    public void testSequenceGenerator1() {
        CyclicAtomicCounter counter = new CyclicAtomicCounter(5);
        Assertions.assertEquals(0, counter.getAndIncrement());
        Assertions.assertEquals(1, counter.getAndIncrement());
        Assertions.assertEquals(2, counter.getAndIncrement());
        Assertions.assertEquals(3, counter.getAndIncrement());
        Assertions.assertEquals(4, counter.getAndIncrement());
        Assertions.assertEquals(0, counter.getAndIncrement());
        Assertions.assertEquals(1, counter.getAndIncrement());
        Assertions.assertEquals(2, counter.getAndIncrement());
    }

    @Test
    public void testSequenceGenerator2() {
        CyclicAtomicCounter counter = new CyclicAtomicCounter(1, 5);
        Assertions.assertEquals(1, counter.getAndIncrement());
        Assertions.assertEquals(2, counter.getAndIncrement());
        Assertions.assertEquals(3, counter.getAndIncrement());
        Assertions.assertEquals(4, counter.getAndIncrement());
        Assertions.assertEquals(1, counter.getAndIncrement());
        Assertions.assertEquals(2, counter.getAndIncrement());
        Assertions.assertEquals(3, counter.getAndIncrement());
    }

    @Test
    public void testSequenceGenerator3() {
        CyclicAtomicCounter counter = new CyclicAtomicCounter(1, 1);
        Assertions.assertEquals(1, counter.getAndIncrement());
    }

}
