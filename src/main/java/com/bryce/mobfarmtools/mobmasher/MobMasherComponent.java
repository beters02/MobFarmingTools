package com.bryce.mobfarmtools.mobmasher;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.util.MFTVectorUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.util.TargetUtil;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MobMasherComponent implements Component<ChunkStore> {
    public static final BuilderCodec<MobMasherComponent> CODEC =
            BuilderCodec.builder(
                            MobMasherComponent.class,
                            MobMasherComponent::new
                    )
                    .append(new KeyedCodec<>("TicksPerAction", Codec.INTEGER),
                            (component, value) -> component.ticksPerAction = value,
                            component -> component.ticksPerAction)
                    .add()
                    .append(new KeyedCodec<>("DamagePerAction", Codec.INTEGER),
                            (component, value) -> component.damagePerAction = value,
                            component -> component.damagePerAction)
                    .add()
                    .append(new KeyedCodec<>("DamageBosses", Codec.BOOLEAN),
                            (component, value) -> component.damageBosses = value,
                            component -> component.damageBosses)
                    .add()
                    .append(new KeyedCodec<>("Enabled", Codec.BOOLEAN),
                            (component, value) -> component.enabled = value,
                            component -> component.enabled)
                    .add()
                    .append(new KeyedCodec<>("TicksLifetime", Codec.INTEGER),
                            (component, value) -> component.ticksLifetime = value,
                            component -> component.ticksLifetime)
                    .add()
                    .append(new KeyedCodec<>("ChunkLoaded", Codec.BOOLEAN),
                            (component, value) -> component.chunkLoaded = value,
                            component -> component.chunkLoaded)
                    .add()
                    .append(new KeyedCodec<>("NoiseSuppressed", Codec.BOOLEAN),
                            (component, value) -> component.noiseSuppressed = value,
                            component -> component.noiseSuppressed)
                    .add()
                    .build();

    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[MobMasherComponent]", false);

    private boolean enabled = MobMasherConstants.DEF_ENABLED;
    private boolean chunkLoaded = MobMasherConstants.DEF_CHUNK_LOADED;
    private boolean noiseSuppressed = MobMasherConstants.DEF_NOISE_SUPPRESSED;
    private boolean damageBosses = MobMasherConstants.DEF_DAMAGE_BOSSES;
    private int ticksPerAction = MobMasherConstants.DEF_TICKS_PER_ACTION;
    private int damagePerAction = MobMasherConstants.DEF_DAMAGE;
    private int ticksLifetime = 0;

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isNoiseSuppressed() {
        return noiseSuppressed;
    }

    public boolean isChunkLoaded() {
        return chunkLoaded;
    }

    public boolean isDamageBossesEnabled() { return damageBosses; }

    public int getTicksPerAction() { return ticksPerAction; }

    public int getDamagePerAction() { return damagePerAction; }

    public int getTicksLifetime() { return ticksLifetime; }

    public void setNoiseSuppressed(boolean value) {
        noiseSuppressed = value;
    }

    public void setChunkLoaded(boolean value) {
        chunkLoaded = value;
    }

    public void setTicksPerAction(int value) { ticksPerAction = value; }

    public void setDamagePerAction(int value) { damagePerAction = value; }

    public void setTicksLifetime(int value) { ticksLifetime = value; }

    public void incrementTicksLifetime(int value) { ticksLifetime += value; }

    public void setDamageBossesEnabled(boolean value) { damageBosses = value; }

    public void setEnabled(World world, Vector3i blockPos, boolean value) {
        BlockType blockType = world.getBlockType(blockPos);
        if (blockType == null) {
            debugger.atWarning("BlockType returned null. Block interaction state cannot be changed.");
            return;
        }

        enabled = value;
        world.setBlockInteractionState(blockPos, blockType, enabled ? "On" : "Off");
        debugger.atInfo("Interaction state successfully set to " + (enabled ? "On" : "Off"));
    }

    public static ComponentType<ChunkStore, MobMasherComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getMobMasherComponentType();
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        MobMasherComponent copy = new MobMasherComponent();
        copy.ticksPerAction = this.ticksPerAction;
        copy.damagePerAction = this.damagePerAction;
        copy.enabled = this.enabled;
        copy.noiseSuppressed = this.noiseSuppressed;
        copy.chunkLoaded = this.chunkLoaded;
        copy.ticksLifetime = this.ticksLifetime;
        copy.damageBosses = this.damageBosses;
        return copy;
    }
}
