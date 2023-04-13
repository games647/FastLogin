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
package com.github.games647.fastlogin.core.hooks;

import java.security.SecureRandom;
import java.util.Random;
import java.util.stream.IntStream;

public class DefaultPasswordGenerator<P> implements PasswordGenerator<P> {

    private static final int PASSWORD_LENGTH = 8;
    private static final char[] PASSWORD_CHARACTERS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
            .toCharArray();

    private final Random random = new SecureRandom();

    @Override
    public String getRandomPassword(P player) {
        StringBuilder generatedPassword = new StringBuilder(8);
        IntStream.rangeClosed(1, PASSWORD_LENGTH)
                .map(i -> random.nextInt(PASSWORD_CHARACTERS.length - 1))
                .mapToObj(pos -> PASSWORD_CHARACTERS[pos])
                .forEach(generatedPassword::append);

        return generatedPassword.toString();
    }
}
