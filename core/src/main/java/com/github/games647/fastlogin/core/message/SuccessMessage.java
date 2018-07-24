package com.github.games647.fastlogin.core.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public class SuccessMessage implements ChannelMessage {

    public static final String SUCCESS_CHANNEL = "success";

    @Override
    public String getChannelName() {
        return SUCCESS_CHANNEL;
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
