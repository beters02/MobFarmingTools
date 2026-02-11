package com.bryce.mobfarmtools.dropper;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

public class DropperComponent implements Component<ChunkStore> {
    public static final BuilderCodec<DropperComponent> CODEC =
            BuilderCodec.builder(
                            DropperComponent.class,
                            DropperComponent::new
                    )
                    .build();

    private int ticksLifetime = 0;

    public int getTicksLifetime() {
        return ticksLifetime;
    }

    public void setTicksLifetime(int value) {
        ticksLifetime = value;
    }

    public void incrementTicksLifetime(int value) {
        ticksLifetime += value;
    }

    public static ComponentType<ChunkStore, DropperComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getDropperComponentType();
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        return new DropperComponent();
    }
}
