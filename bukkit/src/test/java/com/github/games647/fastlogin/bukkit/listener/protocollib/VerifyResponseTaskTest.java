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
package com.github.games647.fastlogin.bukkit.listener.protocollib;

import com.comphenix.protocol.injector.packet.PacketRegistry;
import com.comphenix.protocol.reflect.accessors.Accessors;
import com.comphenix.protocol.reflect.accessors.FieldAccessor;
import com.comphenix.protocol.utility.MinecraftReflection;

import java.util.Optional;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static org.mockito.ArgumentMatchers.any;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mockStatic;

class VerifyResponseTaskTest {

    private static final String NETTY_INJECTOR_CLASS =
            "com.comphenix.protocol.injector.netty.channel.NettyChannelInjector";

    @Test
    void getNetworkManagerReflection() throws ClassNotFoundException {
        try (
                MockedStatic<Bukkit> bukkitMock = mockStatic(Bukkit.class);
                MockedStatic<MinecraftReflection> reflectionMock = mockStatic(MinecraftReflection.class);
                MockedStatic<PacketRegistry> registryMock = mockStatic(PacketRegistry.class)
        ) {
            bukkitMock.when(Bukkit::getVersion).thenReturn("git-Bukkit-18fbb24 (MC: 1.17)");
            reflectionMock.when(MinecraftReflection::getMinecraftPackage).thenReturn("xyz");
            reflectionMock.when(MinecraftReflection::getEnumProtocolClass).thenReturn(Object.class);

            registryMock.when(() -> PacketRegistry.tryGetPacketClass(any())).thenReturn(Optional.empty());
            
            
            Class<?> injectorClass = Class.forName(NETTY_INJECTOR_CLASS);
            FieldAccessor accessor = Accessors.getFieldAccessorOrNull(injectorClass, "networkManager", Object.class);
            assertNotNull(accessor.getField());
        }
    }
}
