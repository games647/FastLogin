package com.github.games647.fastlogin.core.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public interface ChannelMessage {

    String getChannelName();

    void readFrom(ByteArrayDataInput input);

    void writeTo(ByteArrayDataOutput output);
}
