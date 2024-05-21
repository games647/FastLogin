/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2024 games647 and contributors
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
package com.github.games647.fastlogin.core.shared;

public enum FloodgateState {

    /**
     * Purely Java profile
     */
    FALSE(0),

    /**
     * Purely Bedrock profile
     */
    TRUE(1),

    /**
     * Bedrock profile is bidirectional associated with the Java Mojang profile.
     */
    LINKED(2),

    /**
     * Data before floodgate database migration. Floodgate state is unknown.
     */
    NOT_MIGRATED(3);

    private final int value;

    FloodgateState(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    /**
     * Convert a number to FloodgateState
     * <ol start="0">
     *     <li>False</li>
     *     <li>True</li>
     *     <li>Linked</li>
     *     <li>Not Migrated</li>
     * </ol>
     * @param num the number, most likely loaded from the database
     * @return FloodgateStatus on success, null otherwise
     */
    public static FloodgateState fromInt(int num) {
        // using Enum.values()[i] is expensive as per https://stackoverflow.com/a/8762387/9767089
        switch (num) {
            case 0:
                return FloodgateState.FALSE;
            case 1:
                return FloodgateState.TRUE;
            case 2:
                return FloodgateState.LINKED;
            case 3:
                return FloodgateState.NOT_MIGRATED;
            default:
                return null;
        }
    }
}
