package com.bryce.mobfarmtools.sounds;

import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.SoundCategory;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEventLayer;
import com.hypixel.hytale.server.core.modules.entity.component.AudioComponent;
import com.hypixel.hytale.server.core.modules.entity.component.Intangible;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.tracker.NetworkId;
import com.hypixel.hytale.server.core.universe.world.SoundUtil;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SoundManager {
    private static final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[SoundManager]", SoundManagerConstants.DEBUGGER_ENABLED);
    private static final Map<BlockSoundKey, Map<String, MFTSound>> SOUNDS_BY_BLOCK = new ConcurrentHashMap<>();
    private static final Map<Integer, BlockSoundKey> REF_INDEX_TO_KEY = new ConcurrentHashMap<>();

    private SoundManager() {}

    public static @Nullable MFTSound NewSound(Ref<ChunkStore> blockRef, String soundEventId) {
        if (blockRef == null || !blockRef.isValid()) {
            debugger.atWarning("Cannot create sound, block ref is invalid.");
            return null;
        }

        BlockSoundKey key = resolveKey(blockRef);
        if (key == null) {
            debugger.atWarning("Cannot create sound, failed to resolve block key.");
            return null;
        }

        REF_INDEX_TO_KEY.put(blockRef.getIndex(), key);

        Map<String, MFTSound> soundsById = SOUNDS_BY_BLOCK.computeIfAbsent(key, k -> new ConcurrentHashMap<>());
        MFTSound existing = soundsById.get(soundEventId);
        if (existing != null && !existing.isDestroyed()) {
            return existing;
        }

        MFTSound sound = new MFTSound(key, blockRef, soundEventId);
        soundsById.put(soundEventId, sound);
        return sound;
    }

    public static void DestroyAllForBlock(Ref<ChunkStore> blockRef) {
        BlockSoundKey key = resolveKey(blockRef);
        if (key == null && blockRef != null) {
            key = REF_INDEX_TO_KEY.get(blockRef.getIndex());
        }
        if (key == null) {
            return;
        }

        Map<String, MFTSound> sounds = SOUNDS_BY_BLOCK.remove(key);
        if (sounds != null) {
            for (MFTSound sound : sounds.values().toArray(MFTSound[]::new)) {
                sound.Destroy();
            }
        }

        if (blockRef != null) {
            REF_INDEX_TO_KEY.remove(blockRef.getIndex());
        }
        removeRefMappingsForKey(key);
    }

    public static void StopAllForBlock(Ref<ChunkStore> blockRef) {
        BlockSoundKey key = resolveKey(blockRef);
        if (key == null && blockRef != null) {
            key = REF_INDEX_TO_KEY.get(blockRef.getIndex());
        }
        if (key == null) {
            return;
        }

        Map<String, MFTSound> sounds = SOUNDS_BY_BLOCK.get(key);
        if (sounds == null) {
            return;
        }
        for (MFTSound sound : sounds.values()) {
            if (sound != null) {
                sound.Stop();
            }
        }
    }

    public static @Nullable Set<MFTSound> GetAllForBlock(Ref<ChunkStore> blockRef) {
        Map<String, MFTSound> sounds = GetAllForBlockAsMap(blockRef);
        return sounds == null ? null : Set.copyOf(sounds.values());
    }

    public static @Nullable Map<String, MFTSound> GetAllForBlockAsMap(Ref<ChunkStore> blockRef) {
        BlockSoundKey key = resolveKey(blockRef);
        if (key == null && blockRef != null) {
            key = REF_INDEX_TO_KEY.get(blockRef.getIndex());
        }
        if (key == null) {
            return null;
        }

        Map<String, MFTSound> sounds = SOUNDS_BY_BLOCK.get(key);
        if (sounds == null) {
            return null;
        }
        return new HashMap<>(sounds);
    }

    private static @Nullable BlockSoundKey resolveKey(@Nullable Ref<ChunkStore> blockRef) {
        if (blockRef == null || !blockRef.isValid()) {
            return null;
        }

        Vector3i pos = MFTBlockUtil.GetWorldPosFromBlockRef(blockRef.getStore(), blockRef);
        if (pos == null) {
            return null;
        }

        World world = blockRef.getStore().getExternalData().getWorld();
        return new BlockSoundKey(System.identityHashCode(world), pos.x, pos.y, pos.z);
    }

    private static void removeRefMappingsForKey(BlockSoundKey key) {
        REF_INDEX_TO_KEY.entrySet().removeIf(e -> e.getValue().equals(key));
    }

    public static final class MFTSound {
        private final BlockSoundKey blockKey;
        private final String soundEventId;
        private final int soundEventIndex;
        private final boolean looped;
        private final World world;
        private final Vector3d position;
        private final MFTDebugUtil.Debugger debugger;

        private volatile boolean destroyed;
        private volatile boolean playing;
        private volatile Ref<EntityStore> emitterRef;

        private MFTSound(BlockSoundKey blockKey, Ref<ChunkStore> blockRef, String soundEventId) {
            this.blockKey = blockKey;
            this.soundEventId = soundEventId;
            this.soundEventIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
            this.looped = isLoopedSoundEvent(soundEventIndex);
            this.world = blockRef.getStore().getExternalData().getWorld();
            this.position = new Vector3d(blockKey.x + 0.5, blockKey.y + 0.5, blockKey.z + 0.5);
            this.debugger = new MFTDebugUtil.Debugger("[SoundManagerSound_"+soundEventId+"]", SoundManagerConstants.DEBUGGER_ENABLED);
        }

        public boolean Play() {
            if (destroyed || soundEventIndex == 0) {
                debugger.atWarning("Play failed.");
                debugger.atWarning("Destroyed: " + destroyed);
                debugger.atWarning("SoundEventIndex == 0: " + (soundEventIndex == 0));
                return false;
            }

            if (!looped) {
                SoundUtil.playSoundEvent3d(
                        soundEventIndex,
                        SoundCategory.SFX,
                        position.x,
                        position.y,
                        position.z,
                        world.getEntityStore().getStore()
                );
                playing = false; // fire-and-forget one-shot
                debugger.atInfo("Play success (NON LOOPED).");
                return true;
            }

            if (emitterRef != null && emitterRef.isValid()) {
                playing = true;
                debugger.atInfo("Play success.");
                return true;
            }

            com.hypixel.hytale.component.Store<EntityStore> entityStore = world.getEntityStore().getStore();

            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(position, new Vector3f()));
            holder.addComponent(NetworkId.getComponentType(), new NetworkId(entityStore.getExternalData().takeNextNetworkId()));
            holder.addComponent(Intangible.getComponentType(), Intangible.INSTANCE);

            AudioComponent audio = new AudioComponent();
            audio.addSound(soundEventIndex);
            holder.addComponent(AudioComponent.getComponentType(), audio);
            holder.addComponent(EntityStore.REGISTRY.getNonSerializedComponentType(), NonSerialized.get());

            world.execute(() -> {
                if (destroyed) return;
                if (emitterRef != null && emitterRef.isValid()) return;
                emitterRef = entityStore.addEntity(holder, AddReason.SPAWN);
                playing = true;
            });

            debugger.atInfo("Play success.");
            return true;
        }

        public boolean Stop() {
            Ref<EntityStore> ref = emitterRef;
            if (ref == null || !ref.isValid()) {
                playing = false;
                return true;
            }

            emitterRef = null;
            playing = false;

            com.hypixel.hytale.component.Store<EntityStore> entityStore = world.getEntityStore().getStore();

            world.execute(() -> {
                if (ref.isValid()) {
                    entityStore.removeEntity(ref, RemoveReason.REMOVE);
                }
            });

            return true;
        }

        public void Destroy() {
            if (destroyed) return;
            Stop();
            destroyed = true;

            Map<String, MFTSound> sounds = SOUNDS_BY_BLOCK.get(blockKey);
            if (sounds != null) {
                sounds.remove(soundEventId);
                if (sounds.isEmpty()) {
                    SOUNDS_BY_BLOCK.remove(blockKey);
                    removeRefMappingsForKey(blockKey);
                }
            }
        }

        public boolean isLooped() {
            return looped;
        }

        public boolean isPlaying() {
            return looped && playing && emitterRef != null && emitterRef.isValid();
        }

        public boolean isDestroyed() {
            return destroyed;
        }

        public String getSoundEventId() {
            return soundEventId;
        }

        public int getSoundEventIndex() {
            return soundEventIndex;
        }

        private static boolean isLoopedSoundEvent(int soundEventIndex) {
            if (soundEventIndex == 0) return false;
            SoundEvent event = SoundEvent.getAssetMap().getAsset(soundEventIndex);
            if (event == null || event.getLayers() == null) return false;

            for (SoundEventLayer layer : event.getLayers()) {
                if (layer != null && layer.isLooping()) return true;
            }
            return false;
        }
    }

    private static final class BlockSoundKey {
        private final int worldIdentity;
        private final int x;
        private final int y;
        private final int z;

        private BlockSoundKey(int worldIdentity, int x, int y, int z) {
            this.worldIdentity = worldIdentity;
            this.x = x;
            this.y = y;
            this.z = z;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof BlockSoundKey other)) return false;
            return worldIdentity == other.worldIdentity && x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(worldIdentity, x, y, z);
        }
    }
}
