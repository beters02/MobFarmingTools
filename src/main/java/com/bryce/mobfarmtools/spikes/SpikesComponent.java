package com.bryce.mobfarmtools.spikes;

import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

public class SpikesComponent implements Component<ChunkStore> {
    public static final BuilderCodec<SpikesComponent> CODEC =
            BuilderCodec.builder(
                            SpikesComponent.class,
                            SpikesComponent::new
                    )
                    .append(new KeyedCodec<>("DamagePerSecond", Codec.DOUBLE),
                            (component, value) -> component.damagePerSecond = value,
                            component -> component.damagePerSecond)
                    .add()
                    .build();

    private double damagePerSecond = 30.0;

    public double getDamagePerSecond() { return damagePerSecond; }
    public void setDamagePerSecond(double value) { damagePerSecond = value; }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        SpikesComponent copy = new SpikesComponent();
        copy.damagePerSecond = damagePerSecond;
        return copy;
    }
}
