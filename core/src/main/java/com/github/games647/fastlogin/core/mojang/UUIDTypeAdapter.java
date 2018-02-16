package com.github.games647.fastlogin.core.mojang;

import com.github.games647.fastlogin.core.CommonUtil;
import com.google.gson.TypeAdapter;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.util.UUID;

public class UUIDTypeAdapter extends TypeAdapter<UUID> {

    public void write(JsonWriter out, UUID value) throws IOException {
        TypeAdapters.STRING.write(out, CommonUtil.toMojangId(value));
    }

    public UUID read(JsonReader in) throws IOException {
        return CommonUtil.parseId(TypeAdapters.STRING.read(in));
    }
}
