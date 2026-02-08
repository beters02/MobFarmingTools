package com.bryce.mobfarmtools.mobspawner;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.util.MFTMathUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.universe.world.ParticleUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;

public class MobSpawnerComponent implements Component<ChunkStore> {
    public static final BuilderCodec<MobSpawnerComponent> CODEC =
            BuilderCodec.builder(
                            MobSpawnerComponent.class,
                            MobSpawnerComponent::new
                    )
                    .append(new KeyedCodec<>("EntityId", Codec.STRING),
                            (component, value) -> component.entityId = value,
                            component -> component.entityId)
                    .add()
                    .append(new KeyedCodec<>("SpawnRateMin", Codec.INTEGER),
                            (component, value) -> component.spawnRateMin = value,
                            component -> component.spawnRateMin)
                    .add()
                    .append(new KeyedCodec<>("SpawnRateMax", Codec.INTEGER),
                            (component, value) -> component.spawnRateMax = value,
                            component -> component.spawnRateMax)
                    .add()
                    .append(new KeyedCodec<>("SpawnAmountMin", Codec.INTEGER),
                            (component, value) -> component.spawnAmountMin = value,
                            component -> component.spawnAmountMin)
                    .add()
                    .append(new KeyedCodec<>("SpawnAmountMax", Codec.INTEGER),
                            (component, value) -> component.spawnAmountMax = value,
                            component -> component.spawnAmountMax)
                    .add()
                    .append(new KeyedCodec<>("MaxEntities", Codec.INTEGER),
                            (component, value) -> component.maxEntities = value,
                            component -> component.maxEntities)
                    .add()
                    .append(new KeyedCodec<>("ChunkLoaded", Codec.BOOLEAN),
                            (component, value) -> component.chunkLoaded = value,
                            component -> component.chunkLoaded)
                    .add()
                    .append(new KeyedCodec<>("EntitySize", Codec.INT_ARRAY),
                            (component, value) -> component.entitySize = value,
                            component -> component.entitySize)
                    .add()
                    .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                            (component, value) -> component.enabled = value,
                            component -> component.enabled)
                    .add()
                    .build();


    // configurable vars
    private int spawnRateMin = MobSpawnerConstants.DEF_SPAWN_RATE_MIN;
    private int spawnRateMax = MobSpawnerConstants.DEF_SPAWN_RATE_MAX;
    private int spawnAmountMin = MobSpawnerConstants.DEF_SPAWN_AMOUNT_MIN;
    private int spawnAmountMax = MobSpawnerConstants.DEF_SPAWN_AMOUNT_MAX;
    private int maxEntities = MobSpawnerConstants.DEF_MAX_ENTITIES;
    private boolean chunkLoaded = MobSpawnerConstants.DEF_CHUNK_LOADED;
    private String entityId = "None";
    private boolean enabled = true;
    private int[] entitySize;

    // local mutable vars
    private float lifetime = 0f;
    private int currentSpawnRate = MobSpawnerConstants.DEF_SPAWN_RATE_MAX;
    private int currentSpawnAmount = MobSpawnerConstants.DEF_SPAWN_AMOUNT_MIN;
    private int failedTries = 0;

    @NonNull
    public static ComponentType<ChunkStore, MobSpawnerComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getMobSpawnerComponentType();
    }

    public String getEntityId() {
        return entityId;
    }
    public float getLifetime() { return lifetime; }
    public int getSpawnRateMin() { return spawnRateMin; }
    public int getSpawnRateMax() { return spawnRateMax; }
    public int getFailedTries() {
        return failedTries;
    }
    public int getMaxEntities() { return maxEntities; }
    public Vector3i getEntitySize() {
        if (entitySize == null || entitySize.length < 2) {
            MobFarmingToolsPlugin.LOGGER.atWarning().log("MobSpawner Entity Size unsuccessful");
            return new Vector3i(1,1,1);
        }

        return new Vector3i(entitySize[0], entitySize[1], entitySize[2]);
    }

    public void setEnabled(boolean val) {
        enabled = val;
    }
    public void setEntityId(String id) {
        entityId = id;
    }
    public void setFailedTries(int val) {
        failedTries = val;
    }
    public void setEntitySize(Vector3i size) {
        entitySize = new int[]{size.x, size.y, size.z};
    }
    public void setLifetime(float val) {
        lifetime = val;
    }
    public void setRandomSpawnRate() { currentSpawnRate = MFTMathUtil.RandomRange(spawnRateMin, spawnRateMax); }
    public void setRandomSpawnAmount() { currentSpawnAmount = MFTMathUtil.RandomRange(spawnAmountMin, spawnAmountMax); }

    public void incrementFailedTries(int val) { failedTries += val; }
    public void incrementLifetime(float amount) { lifetime += amount; }

    public boolean canTick() { return enabled && !Objects.equals(entityId, "None"); }
    public boolean canSpawn() { return lifetime >= currentSpawnRate; }

    public void spawnAction(Store<EntityStore> entityStore, Vector3d spawnPos, Vector3d blockPos) {
        blockPos.y += 1;
        blockPos.x += 0.5;
        blockPos.z += 0.5;

        for (int i = 0; i < currentSpawnAmount; i++) {
            NPCPlugin.get().spawnNPC(entityStore, entityId, null, spawnPos, new Vector3f());
            ParticleUtil.spawnParticleEffect(
                "MFT_Spawner_Fire",
                blockPos,
                entityStore
            );
        }

        MobFarmingToolsPlugin.LOGGER.atInfo().log("Spawned "+currentSpawnAmount+" "+entityId+"s");
    }

    public void sendInfoMessage(Player player) {
        player.sendMessage(Message.raw("Enabled: " + enabled));
        player.sendMessage(Message.raw("Chunk Loaded: " + chunkLoaded));
        player.sendMessage(Message.raw("Spawner Entity: " + entityId));
        player.sendMessage(Message.raw("Spawn Rate: " + spawnRateMin + "-" + spawnRateMax));
        player.sendMessage(Message.raw("Spawn Amount: " + spawnAmountMin + "-" + spawnAmountMax));
    }

    public void printDebug(Player player) {
        player.sendMessage(Message.raw("[MobSpawner] CanTick: " + canTick()));
        player.sendMessage(Message.raw("[MobSpawner] Current Spawn Amnt: " + currentSpawnAmount));
        player.sendMessage(Message.raw("[MobSpawner] Current Spawn Rate: " + currentSpawnRate));
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        MobSpawnerComponent copy = new MobSpawnerComponent();
        copy.spawnRateMin = this.spawnRateMin;
        copy.spawnRateMax = this.spawnRateMax;
        copy.spawnAmountMin = this.spawnAmountMin;
        copy.spawnAmountMax = this.spawnAmountMax;
        copy.maxEntities = this.maxEntities;
        copy.chunkLoaded = this.chunkLoaded;
        copy.entityId = this.entityId;
        copy.enabled = this.enabled;
        copy.entitySize = this.entitySize;
        return copy;
    }
}
