/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2023 games647 and contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.games647.fastlogin.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledForJreRange;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

class AsyncSchedulerTest {

    @Test
    @DisabledForJreRange(min = JRE.JAVA_21)
    void legacyScheduler() {
        Logger logger = LoggerFactory.getLogger(AsyncSchedulerTest.class);
        AsyncScheduler scheduler = new AsyncScheduler(logger, Executors.newCachedThreadPool());

        AtomicBoolean virtual = new AtomicBoolean(false);
        scheduler.runAsync(() -> setVirtual(virtual)).join();

        Assertions.assertFalse(virtual.get());
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void greenThread() {
        Logger logger = LoggerFactory.getLogger(AsyncSchedulerTest.class);
        AsyncScheduler scheduler = new AsyncScheduler(logger, Executors.newCachedThreadPool());

        AtomicBoolean virtual = new AtomicBoolean(false);
        scheduler.runAsync(() -> setVirtual(virtual)).join();

        Assertions.assertTrue(virtual.get());
    }

    private static void setVirtual(AtomicBoolean virtual) {
        if (Thread.currentThread().isVirtual()) {
            virtual.set(true);
        }
    }
}
