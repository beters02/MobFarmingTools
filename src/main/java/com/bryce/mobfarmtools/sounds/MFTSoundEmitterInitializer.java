package com.bryce.mobfarmtools.sounds;

import com.bryce.mobfarmtools.chunks.ForcedChunkPersistence;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeComponent;
import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperComponent;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperConstants;
import com.bryce.mobfarmtools.vacuumhopper.ui.VacuumHopperUpgradePage;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.RefSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MFTSoundEmitterInitializer extends RefSystem<ChunkStore> {
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[SoundEmitterInitializer]");
    public MFTSoundEmitterInitializer() {
        debugger.setEnabled(true);
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                BlockModule.BlockStateInfo.getComponentType(),
                MFTSoundEmitterComponent.getComponentType()
        );
    }

    @Override
    public void onEntityAdded(@NonNull Ref<ChunkStore> ref, @NonNull AddReason addReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        if (!ref.isValid()) return;

        MFTSoundEmitterComponent emitter = store.getComponent(ref, MFTSoundEmitterComponent.getComponentType());
        if (emitter == null) return;

        String[] soundIds = emitter.getSoundIds();
        if (soundIds == null || soundIds.length == 0) {
            emitter.setInitialized(true);
            return;
        }

        Map<String, SoundManager.MFTSound> existing = SoundManager.GetAllForBlockAsMap(ref);
        if (existing == null) existing = new java.util.HashMap<>();

        for (String soundId : soundIds) {
            SoundManager.MFTSound sound = existing.get(soundId);
            if (sound == null || sound.isDestroyed()) {
                SoundManager.NewSound(ref, soundId);
            }
        }

        emitter.setInitialized(true);
    }

    @Override
    public void onEntityRemove(@NonNull Ref<ChunkStore> ref, @NonNull RemoveReason removeReason, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        SoundManager.DestroyAllForBlock(ref); // critical
    }
}