package com.bryce.mobfarmtools.mobspawner;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.NPCPlugin;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
                    .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                            (component, value) -> component.enabled = value,
                            component -> component.enabled)
                    .add()
                    .build();

    private String entityId = "None";
    private boolean enabled = true;
    private int spawnRateMin = 30;
    private int spawnRateMax = 60;
    private int spawnAmountMin = 1;
    private int spawnAmountMax = 2;

    private float lifetime = 0f;
    private float currentSpawnRate = 15f;
    private int failedTries = 0;

    @NonNull
    public static ComponentType<ChunkStore, MobSpawnerComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getMobSpawnerComponentType();
    }

    public void setEnabled(boolean val) {
        enabled = val;
    }

    public void setEntityId(String id) {
        entityId = id;
    }

    public String getEntityId() {
        return entityId;
    }

    public void setFailedTries(int val) {
        failedTries = val;
    }

    public void incrementFailedTries(int val) {
        failedTries += val;
    }

    public int getFailedTries() {
        return failedTries;
    }

    public void setLifetime(float val) {
        lifetime = val;
    }

    public void setRandomCurrentSpawnRate() {
        currentSpawnRate = ThreadLocalRandom.current().nextInt(spawnRateMin, spawnRateMax+1);
    }

    public boolean canTick() {
        if (!enabled) return false;
        return !Objects.equals(entityId, "None");
    }

    public boolean canSpawn() {
        return lifetime >= currentSpawnRate;
    }

    public void incrementLifetime(float amount) {
        lifetime += amount;
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        return new MobSpawnerComponent();
    }
}
