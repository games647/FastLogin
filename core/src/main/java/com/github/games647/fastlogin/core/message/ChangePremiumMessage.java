package com.github.games647.fastlogin.core.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

public class ChangePremiumMessage implements ChannelMessage {

    public static final String CHANGE_CHANNEL = "ChStatus";

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
        return this.getClass().getSimpleName() + '{' +
                "playerName='" + playerName + '\'' +
                ", shouldEnable=" + willEnable +
                ", isSourceInvoker=" + isSourceInvoker +
                '}';
    }
}
