package com.bryce.mobfarmtools.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.Arrays;
import java.util.List;

public class MobFarmingToolsConfig {

    public static final BuilderCodec<MobFarmingToolsConfig> CODEC = BuilderCodec.builder(MobFarmingToolsConfig.class, MobFarmingToolsConfig::new)
            .append(new KeyedCodec<>("MobsBlacklist", Codec.STRING_ARRAY),
                    (config, value) -> config.mobsBlacklist = value,
                    (config) -> config.mobsBlacklist)
            .add()
            .append(new KeyedCodec<>("MobsBlacklistRegex", Codec.STRING_ARRAY),
                    (config, value) -> config.mobsBlacklistRegex = value,
                    (config) -> config.mobsBlacklistRegex)
            .documentation("If the EntityId contains a string from this array it will be blacklisted.")
            .add()
            .build();

    private String[] mobsBlacklist = {
            "Dungeon_Scarak_Broodmother",
            "Rex_Cave"
    };

    private String[] mobsBlacklistRegex = {
        "Golem"
    };

    public boolean isEntityBlacklisted(String entityId) {
        List<String> list = Arrays.asList(this.mobsBlacklist);

        if (list.contains(entityId)) return true;

        for ( String regexId : mobsBlacklistRegex ) {
            if (entityId.contains(regexId)) {
                return true;
            }
        }

        return false;
    }

}
