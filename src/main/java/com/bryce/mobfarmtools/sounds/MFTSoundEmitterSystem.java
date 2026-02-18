package com.bryce.mobfarmtools.sounds;

import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTChunkUtil;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperComponent;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperConstants;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperHelpers;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class MFTSoundEmitterSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, MFTSoundEmitterComponent> soundEmitterComponentType;
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[SoundEmitterSystem]");

    public MFTSoundEmitterSystem(ComponentType<ChunkStore, MFTSoundEmitterComponent> soundEmitterComponentType) {
        this.soundEmitterComponentType = soundEmitterComponentType;
        debugger.setEnabled(false);
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                this.soundEmitterComponentType,
                BlockModule.BlockStateInfo.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        MFTSoundEmitterComponent emitter = archetypeChunk.getComponent(index, this.soundEmitterComponentType);
        if (emitter == null) {
            debugger.atWarning("EMITTER COMPONENT NOT FOUND!");
            return;
        }

        if (!emitter.isInitialized()) return;

        if (!emitter.isPendingPlayEmpty() || !emitter.isPendingStopEmpty()) {
            Ref<ChunkStore> ref = emitter.getStoredBlockRef();
            if (ref == null) {
                debugger.atWarning("Attempted to play or stop pending sounds but block ref is null.");
                return;
            }
            Map<String, SoundManager.MFTSound> sounds = SoundManager.GetAllForBlockAsMap(ref);
            if (sounds == null) {
                debugger.atWarning("Attempted to play or stop pending sounds but sounds map is null.");
            } else {
                emitter.getPendingPlay().forEach(soundId -> {
                    SoundManager.MFTSound sound = sounds.get(soundId);
                    if (sound == null) {
                        debugger.atWarning("Attempted to play pending sound " + soundId + " but it is null.");
                    } else {
                        sound.Play();
                        debugger.atInfo("Played pending sound " + soundId);
                    }
                });
                emitter.getPendingStop().forEach(soundId -> {
                    SoundManager.MFTSound sound = sounds.get(soundId);
                    if (sound == null) {
                        debugger.atWarning("Attempted to stop pending sound " + soundId + " but it is null.");
                    } else {
                        sound.Stop();
                        debugger.atInfo("Stopped pending sound " + soundId);
                    }
                });
            }
            emitter.clearPendingPlay();
            emitter.clearPendingStop();
        }
    }
}
