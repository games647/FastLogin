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
package com.github.games647.fastlogin.bukkit.auth;

import com.github.games647.fastlogin.bukkit.FastLoginBukkit;
import com.github.games647.fastlogin.bukkit.auth.protocollib.SkinApplyListener;
import org.bukkit.plugin.PluginManager;

import java.util.Optional;

public abstract class LocalAuthentication implements AuthenticationBackend {

    protected final FastLoginBukkit plugin;

    protected LocalAuthentication(FastLoginBukkit plugin) {
        this.plugin = plugin;
    }

    @Override
    public void init(PluginManager pluginManager) {
        if (!plugin.getCore().setupDatabase()) {
            plugin.setEnabled(false);
            return;
        }

        // if server is using paper - we need to add one more listener to correct the user cache usage
        if (isPaper()) {
            pluginManager.registerEvents(new PaperCacheListener(this), this);
        } else if (plugin.getConfig().getBoolean("forwardSkin")) {
            //if server is using paper - we need to set the skin at pre login anyway, so no need for this listener
            pluginManager.registerEvents(new SkinApplyListener(plugin), plugin);
        }
    }

    private boolean isPaper() {
        return isClassAvailable("com.destroystokyo.paper.PaperConfig").isPresent()
                || isClassAvailable("io.papermc.paper.configuration.Configuration").isPresent();
    }

    private Optional<Class<?>> isClassAvailable(String clazzName) {
        try {
            return Optional.of(Class.forName(clazzName));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }
}
