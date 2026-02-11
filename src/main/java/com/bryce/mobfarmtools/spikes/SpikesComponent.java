package com.bryce.mobfarmtools.spikes;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.config.MobFarmingToolsConfig;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.Config;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

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
                    .append(new KeyedCodec<>("DamagePlayersEnabled", Codec.BOOLEAN),
                            (component, value) -> component.damagePlayersEnabled = value,
                            component -> component.damagePlayersEnabled)
                    .add()
                    .append(new KeyedCodec<>("DamageNpcsEnabled", Codec.BOOLEAN),
                            (component, value) -> component.damageNPCsEnabled = value,
                            component -> component.damageNPCsEnabled)
                    .add()
                    .append(new KeyedCodec<>("DamageBosses", Codec.BOOLEAN),
                            (component, value) -> component.damageBossesEnabled = value,
                            component -> component.damageBossesEnabled)
                    .add()
                    .append(new KeyedCodec<>("SpikesType", Codec.STRING),
                            (component, value) -> component.spikesType = value,
                            component -> component.spikesType)
                    .add()
                    .afterDecode(SpikesComponent::AfterDecode)
                    .build();

    private double damagePerSecond = 30.0;
    private int ticksLifetime = 0;
    private boolean customSpikes = false;
    private boolean damagePlayersEnabled = false;
    private boolean damageNPCsEnabled = true;
    private boolean damageBossesEnabled = false;
    private String spikesType = "Default";

    public double getDamagePerSecond() { return damagePerSecond; }
    public int getTicksLifetime() { return ticksLifetime; }
    public boolean isDamagePlayersEnabled() { return damagePlayersEnabled; }
    public boolean isDamageNPCsEnabled() { return damageNPCsEnabled; }
    public boolean isDamageBossesEnabled() { return damageBossesEnabled; }
    public boolean isCustomSpikes() { return customSpikes; }

    public void setDamagePerSecond(double value) { damagePerSecond = value; }
    public void setTicksLifetime(int value) { ticksLifetime = value; }

    public void incrementTicksLifetime(int value) { ticksLifetime += value; }

    public SpikesComponent() {

    }

    public static ComponentType<ChunkStore, SpikesComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getSpikesComponentType();
    }

    private void AfterDecode() {
        if (!Objects.equals(spikesType, "Adamantite")) return;
        Config<MobFarmingToolsConfig> config = MobFarmingToolsPlugin.get().getMobFarmingToolsConfig();
        damageBossesEnabled = config.get().isAdamantiteSpikesDamageBossesEnabled();
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        SpikesComponent copy = new SpikesComponent();
        copy.damagePerSecond = damagePerSecond;
        copy.customSpikes = customSpikes;
        copy.damagePlayersEnabled = damagePlayersEnabled;
        copy.damageNPCsEnabled = damageNPCsEnabled;
        copy.ticksLifetime = ticksLifetime;
        return copy;
    }
}
