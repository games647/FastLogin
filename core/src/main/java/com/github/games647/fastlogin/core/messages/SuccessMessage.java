package com.github.games647.fastlogin.core.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public class SuccessMessage implements ChannelMessage {

    @Override
    public String getChannelName() {
        return "Success";
    }

    @Override
    public void readFrom(ByteArrayDataInput input) {
        //empty
    }

    @Override
    public void writeTo(ByteArrayDataOutput output) {
        //empty
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{}";
    }
}
