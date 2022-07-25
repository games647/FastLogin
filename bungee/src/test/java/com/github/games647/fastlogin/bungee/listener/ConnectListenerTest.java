/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 games647 and contributors
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
package com.github.games647.fastlogin.bungee.listener;

import java.lang.reflect.Field;
import java.util.UUID;

import net.md_5.bungee.BungeeCord;
import net.md_5.bungee.conf.Configuration;
import net.md_5.bungee.connection.InitialHandler;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class ConnectListenerTest {

    @Test
    void testUUIDSetter() throws Throwable {
        BungeeCord proxyMock = mock(BungeeCord.class);
        BungeeCord.setInstance(proxyMock);
        
        Configuration configMock = mock(Configuration.class);
        Field configField = proxyMock.getClass().getField("config");
        configField.setAccessible(true);
        configField.set(proxyMock, configMock);

        InitialHandler handler = new InitialHandler(proxyMock, null);

        UUID expectedUUID = UUID.randomUUID();
        ConnectListener.UNIQUE_ID_SETTER.invokeExact(handler, expectedUUID);
        assertEquals(expectedUUID, handler.getUniqueId());
    }
}
