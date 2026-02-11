package com.bryce.mobfarmtools.config;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;

import java.util.Arrays;
import java.util.List;

public class MobFarmingToolsConfig {

    public static final BuilderCodec<MobFarmingToolsConfig> CODEC = BuilderCodec.builder(MobFarmingToolsConfig.class, MobFarmingToolsConfig::new)
            .append(new KeyedCodec<>("SpawnerMobsBlacklist", Codec.STRING_ARRAY),
                    (config, value) -> config.spawnerMobsBlacklist = value,
                    (config) -> config.spawnerMobsBlacklist)
            .add()
            .append(new KeyedCodec<>("SpawnerMobsBlacklistRegex", Codec.STRING_ARRAY),
                    (config, value) -> config.spawnerMobsBlacklistRegex = value,
                    (config) -> config.spawnerMobsBlacklistRegex)
            .documentation("If the EntityId contains a string from this array it will be blacklisted from the spawner.")
            .add()
            .append(new KeyedCodec<>("SpikesMobsBlacklist", Codec.STRING_ARRAY),
                    (config, value) -> config.spikesMobsBlacklist = value,
                    (config) -> config.spikesMobsBlacklist)
            .add()
            .append(new KeyedCodec<>("SpikesMobsBlacklistRegex", Codec.STRING_ARRAY),
                    (config, value) -> config.spikesMobsBlacklistRegex = value,
                    (config) -> config.spikesMobsBlacklistRegex)
            .documentation("If the EntityId contains a string from this array it will be blacklisted from the spikes.")
            .add()
            .append(new KeyedCodec<>("SpikesDamagePlayers", Codec.BOOLEAN),
                    (config, value) -> config.spikesDamagePlayersEnabled = value,
                    (config) -> config.spikesDamagePlayersEnabled)
            .add()
            .append(new KeyedCodec<>("SpikesDamageNpcs", Codec.BOOLEAN),
                    (config, value) -> config.spikesDamageNpcsEnabled = value,
                    (config) -> config.spikesDamageNpcsEnabled)
            .add()
            .append(new KeyedCodec<>("AdamantiteSpikesDamageBosses", Codec.BOOLEAN),
                    (config, value) -> config.adamantiteSpikesDamageBosses = value,
                    (config) -> config.adamantiteSpikesDamageBosses)
            .add()
            .build();

    private String[] spawnerMobsBlacklist = {
            "Dungeon_Scarak_Broodmother",
            "Rex_Cave"
    };

    private String[] spawnerMobsBlacklistRegex = {
        "Golem"
    };

    private String[] spikesMobsBlacklist = {};
    private String[] spikesMobsBlacklistRegex = {};

    private boolean spikesDamagePlayersEnabled = false;
    private boolean spikesDamageNpcsEnabled = true;

    private boolean adamantiteSpikesDamageBosses = true;

    public boolean isSpikesDamagePlayersEnabled() { return spikesDamagePlayersEnabled; }
    public boolean isSpikesDamageNpcsEnabled() { return spikesDamageNpcsEnabled; }
    public boolean isAdamantiteSpikesDamageBossesEnabled() { return adamantiteSpikesDamageBosses; }

    public boolean isEntityBlacklistedSpawner(String entityId) {
        List<String> list = Arrays.asList(this.spawnerMobsBlacklist);

        if (list.contains(entityId)) return true;

        for ( String regexId : spawnerMobsBlacklistRegex ) {
            if (entityId.contains(regexId)) {
                return true;
            }
        }

        return false;
    }

    public boolean isEntityBlacklistedSpikes(String entityId) {
        List<String> list = Arrays.asList(this.spikesMobsBlacklist);

        if (list.contains(entityId)) return true;

        for ( String regexId : spikesMobsBlacklistRegex ) {
            if (entityId.contains(regexId)) {
                return true;
            }
        }

        return false;
    }

}
