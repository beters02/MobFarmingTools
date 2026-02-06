package com.bryce.mobfarmtools.mobswab;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

public class MobSwabMetadata {

    public static final String KEY = "SwabbedMob";
    public static final BuilderCodec<MobSwabMetadata> CODEC = BuilderCodec.builder(MobSwabMetadata.class, MobSwabMetadata::new)
            .append(new KeyedCodec<>("MobId", Codec.STRING), (m, v) -> m.mobId = v, m -> m.mobId)
            .add()
            .build();
    public static final KeyedCodec<MobSwabMetadata> KEYED_CODEC = new KeyedCodec<>(KEY, CODEC);

    private String mobId = "None";

    public String getMobId() {
        return this.mobId;
    }

    public void setMobId(String mobId) {
        this.mobId = mobId;
    }

}
