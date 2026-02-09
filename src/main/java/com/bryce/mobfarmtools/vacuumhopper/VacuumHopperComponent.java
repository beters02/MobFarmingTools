package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class VacuumHopperComponent implements Component<ChunkStore> {
    public static final BuilderCodec<VacuumHopperComponent> CODEC =
            BuilderCodec.builder(VacuumHopperComponent.class, VacuumHopperComponent::new).build();

    private float lifetime = 0f;
    private int ticksLifetime = 0;
    private boolean enabled = true;
    private boolean hasAvailableContainer = false;

    public float getLifetime() {
        return lifetime;
    }
    public float getTicksLifetime() {
        return ticksLifetime;
    }
    public boolean isEnabled() {
        return enabled;
    }
    public boolean hasAvailableContainer() { return hasAvailableContainer; }

    public void setLifetime(float value) {
        lifetime = value;
    }
    public void setTicksLifetime(int value) {
        ticksLifetime = value;
    }
    public void setEnabled(boolean value) {
        enabled = value;
    }
    public void setHasAvailableContainer(boolean value) { hasAvailableContainer = value; }

    public void incrementLifetime(float amount) {
        lifetime += amount;
    }
    public void incrementTicksLifetime(int amount) {
        ticksLifetime += amount;
    }

    @NonNull
    public static ComponentType<ChunkStore, VacuumHopperComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getVacuumHopperComponentType();
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        VacuumHopperComponent copy = new VacuumHopperComponent();
        copy.enabled = enabled;
        copy.lifetime = lifetime;
        copy.ticksLifetime = ticksLifetime;
        copy.hasAvailableContainer = hasAvailableContainer;
        return copy;
    }
}