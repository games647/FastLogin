package com.github.games647.fastlogin.core.messages;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;

import java.util.UUID;

public class LoginActionMessage implements ChannelMessage {

    private Type type;

    private String playerName;
    private UUID proxyId;

    public LoginActionMessage(Type type, String playerName, UUID proxyId) {
        this.type = type;
        this.playerName = playerName;
        this.proxyId = proxyId;
    }

    public LoginActionMessage() {
        //reading mode
    }

    public Type getType() {
        return type;
    }

    public String getPlayerName() {
        return playerName;
    }

    public UUID getProxyId() {
        return proxyId;
    }

    @Override
    public void readFrom(ByteArrayDataInput input) {
        this.type = Type.values()[input.readInt()];

        this.playerName = input.readUTF();

        //bungeecord UUID
        long mostSignificantBits = input.readLong();
        long leastSignificantBits = input.readLong();
        this.proxyId = new UUID(mostSignificantBits, leastSignificantBits);
    }

    @Override
    public void writeTo(ByteArrayDataOutput output) {
        output.writeInt(type.ordinal());

        //Data is sent through a random player. We have to tell the Bukkit version of this plugin the target
        output.writeUTF(playerName);

        //proxy identifier to check if it's a acceptable proxy
        output.writeLong(proxyId.getMostSignificantBits());
        output.writeLong(proxyId.getLeastSignificantBits());
    }

    @Override
    public String getChannelName() {
        return "LoginAction";
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + '{' +
                "type='" + type + '\'' +
                ", playerName='" + playerName + '\'' +
                ", proxyId=" + proxyId +
                '}';
    }

    public enum Type {

        LOGIN,

        REGISTER,

        CRACKED
    }
}
