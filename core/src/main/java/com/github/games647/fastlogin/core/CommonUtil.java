/*
 * SPDX-License-Identifier: MIT
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2021 <Your name and contributors>
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

import com.github.games647.craftapi.cache.SafeCacheBuilder;
import com.google.common.cache.CacheLoader;

import java.lang.reflect.Constructor;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.jul.JDK14LoggerAdapter;

public class CommonUtil {

    private static final char COLOR_CHAR = '&';
    private static final char TRANSLATED_CHAR = '§';

    public static <K, V> ConcurrentMap<K, V> buildCache(int expireAfterWrite, int maxSize) {
        SafeCacheBuilder<Object, Object> builder = SafeCacheBuilder.newBuilder();

        if (expireAfterWrite > 0) {
            builder.expireAfterWrite(expireAfterWrite, TimeUnit.MINUTES);
        }

        if (maxSize > 0) {
            builder.maximumSize(maxSize);
        }

        return builder.build(CacheLoader.from(() -> {
            throw new UnsupportedOperationException();
        }));
    }

    public static String translateColorCodes(String rawMessage) {
        char[] chars = rawMessage.toCharArray();
        for (int i = 0; i < chars.length - 1; i++) {
            if (chars[i] == COLOR_CHAR && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(chars[i + 1]) > -1) {
                chars[i] = TRANSLATED_CHAR;
                chars[i + 1] = Character.toLowerCase(chars[i + 1]);
            }
        }

        return new String(chars);
    }

    /**
     * This creates a SLF4J logger. In the process it initializes the SLF4J service provider. This method looks
     * for the provider in the plugin jar instead of in the server jar when creating a Logger. The provider is only
     * initialized once, so this method should be called early.
     *
     * The provider is bound to the service class `SLF4JServiceProvider`. Relocating this class makes it available
     * for exclusive own usage. Other dependencies will use the relocated service too, and therefore will find the
     * initialized provider.
     *
     * @param parent JDK logger
     * @return slf4j logger
     */
    public static Logger initializeLoggerService(java.util.logging.Logger parent) {
        // set the class loader to the plugin one to find our own SLF4J provider in the plugin jar and not in the global
        ClassLoader oldLoader = Thread.currentThread().getContextClassLoader();

        ClassLoader pluginLoader = CommonUtil.class.getClassLoader();
        Thread.currentThread().setContextClassLoader(pluginLoader);
        try {
            parent.setLevel(Level.ALL);

            Class<JDK14LoggerAdapter> adapterClass = JDK14LoggerAdapter.class;
            Constructor<JDK14LoggerAdapter> cons = adapterClass.getDeclaredConstructor(java.util.logging.Logger.class);
            cons.setAccessible(true);
            return cons.newInstance(parent);
        } catch (ReflectiveOperationException reflectEx) {
            parent.log(Level.WARNING, "Cannot create slf4j logging adapter", reflectEx);
            parent.log(Level.WARNING, "Creating logger instance manually...");
            return LoggerFactory.getLogger(parent.getName());
        } finally {
            // restore previous class loader
            Thread.currentThread().setContextClassLoader(oldLoader);
        }
    }

    private CommonUtil() {
        //Utility class
    }
}
