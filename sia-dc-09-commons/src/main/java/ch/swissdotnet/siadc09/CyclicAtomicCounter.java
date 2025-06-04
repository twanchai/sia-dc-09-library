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
package ch.swissdotnet.siadc09;

import java.util.concurrent.atomic.AtomicInteger;


public class CyclicAtomicCounter {

    private final int min;
    private final int max;
    private final AtomicInteger counter;

    public CyclicAtomicCounter(final int max) {
        this(0, max);
    }

    public CyclicAtomicCounter(final int min, final int max) {
        this.max = max;
        this.min = min;
        this.counter = new AtomicInteger(min);
    }

    public int getAndIncrement() {
        int currentCounter, newCounter;
        do {
            currentCounter = this.counter.get();
            newCounter = (currentCounter + 1) % this.max;
            if (newCounter == 0) {
                newCounter = min;
            }
        } while (!this.counter.compareAndSet(currentCounter, newCounter));
        return currentCounter;
    }


}
