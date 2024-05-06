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
package com.github.games647.fastlogin.core.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public class ChangePremiumMessage implements ChannelMessage {

    public static final String CHANGE_CHANNEL = "ch-st";

    private String playerName;
    private boolean willEnable;
    private boolean isSourceInvoker;

    public ChangePremiumMessage(String playerName, boolean willEnable, boolean isSourceInvoker) {
        this.playerName = playerName;
        this.willEnable = willEnable;
        this.isSourceInvoker = isSourceInvoker;
    }

    public ChangePremiumMessage() {
        //reading from
    }

    public String getPlayerName() {
        return playerName;
    }

    public boolean shouldEnable() {
        return willEnable;
    }

    public boolean isSourceInvoker() {
        return isSourceInvoker;
    }

    @Override
    public String getChannelName() {
        return CHANGE_CHANNEL;
    }

    @Override
    public void readFrom(ByteArrayDataInput input) {
        willEnable = input.readBoolean();
        playerName = input.readUTF();
        isSourceInvoker = input.readBoolean();
    }

    @Override
    public void writeTo(ByteArrayDataOutput output) {
        output.writeBoolean(willEnable);
        output.writeUTF(playerName);
        output.writeBoolean(isSourceInvoker);
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{'
            + "playerName='" + playerName + '\''
            + ", shouldEnable=" + willEnable
            + ", isSourceInvoker=" + isSourceInvoker
            + '}';
    }
}
