package com.bryce.mobfarmtools.sounds;

import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.util.MFTSoundUtil;
import com.hypixel.hytale.component.AddReason;
import com.hypixel.hytale.component.Holder;
import com.hypixel.hytale.component.NonSerialized;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.RemoveReason;
import com.hypixel.hytale.component.Store;
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
import io.sentry.internal.debugmeta.IDebugMetaLoader;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class SoundManager {
    private static final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[SoundManager]", SoundManagerConstants.DEBUGGER_ENABLED);
    private static final Map<Integer, Set<MFTSound>> SOUNDS_BY_BLOCK = new ConcurrentHashMap<>();

    private SoundManager() {}

    public static @Nullable MFTSound NewSound(Ref<ChunkStore> blockRef, String soundEventId) {
        if (blockRef == null || !blockRef.isValid()) {
            debugger.atWarning("BLOCK REF IS FUCKING INVALID.");
            return null;
        }

        MFTSound sound = new MFTSound(blockRef, soundEventId);
        SOUNDS_BY_BLOCK
                .computeIfAbsent(blockRef.getIndex(), k -> ConcurrentHashMap.newKeySet())
                .add(sound);
        return sound;
    }

    public static void DestroyAllForBlock(Ref<ChunkStore> blockRef) {
        Set<MFTSound> sounds = SOUNDS_BY_BLOCK.remove(blockRef.getIndex());
        if (sounds == null) return;
        for (MFTSound sound : sounds) {
            sound.Destroy();
        }
    }

    public static void StopAllForBlock(Ref<ChunkStore> blockRef) {
        Set<MFTSound> sounds = SOUNDS_BY_BLOCK.get(blockRef.getIndex());
        if (sounds == null) return;
        for (MFTSound sound : sounds) {
            sound.Stop();
        }
    }

    public static @Nullable Set<MFTSound> GetAllForBlock(Ref<ChunkStore> blockRef) {
        return SOUNDS_BY_BLOCK.get(blockRef.getIndex());
    }

    public static @Nullable Map<String, MFTSound> GetAllForBlockAsMap(Ref<ChunkStore> blockRef) {
        Set<MFTSound> set = GetAllForBlock(blockRef);
        if (set == null) return null;
        Map<String, MFTSound> soundsMap = new HashMap<>();
        set.forEach(sound -> {
            soundsMap.put(sound.getSoundEventId(), sound);
        });
        return soundsMap;
    }

    public static final class MFTSound {
        private final Ref<ChunkStore> blockRef;
        private final String soundEventId;
        private final int soundEventIndex;
        private final boolean looped;
        private final MFTDebugUtil.Debugger debugger;

        private volatile boolean destroyed;
        private volatile boolean playing;
        private volatile Ref<EntityStore> emitterRef;

        private MFTSound(Ref<ChunkStore> blockRef, String soundEventId) {
            this.blockRef = blockRef;
            this.soundEventId = soundEventId;
            this.soundEventIndex = SoundEvent.getAssetMap().getIndex(soundEventId);
            this.looped = isLoopedSoundEvent(soundEventIndex);
            this.debugger = new MFTDebugUtil.Debugger("[SoundManagerSound_"+soundEventId+"]", SoundManagerConstants.DEBUGGER_ENABLED);
        }

        public boolean Play() {
            if (destroyed || soundEventIndex == 0 || !blockRef.isValid()) {
                debugger.atWarning("Play failed.");
                debugger.atWarning("Destroyed: " + destroyed);
                debugger.atWarning("SoundEventIndex == 0: " + (soundEventIndex == 0));
                debugger.atWarning("BlockRefIsValid: " + blockRef.isValid());
                return false;
            }

            Store<ChunkStore> chunkStore = blockRef.getStore();
            World world = chunkStore.getExternalData().getWorld();
            Vector3d pos = getCenterWorldPos(chunkStore, blockRef);
            if (pos == null) {
                debugger.atWarning("Play failed because pos == null.");
                return false;
            }

            if (!looped) {
                SoundUtil.playSoundEvent3d(
                        soundEventIndex,
                        SoundCategory.SFX,
                        pos.x,
                        pos.y,
                        pos.z,
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

            Store<EntityStore> entityStore = world.getEntityStore().getStore();

            Holder<EntityStore> holder = EntityStore.REGISTRY.newHolder();
            holder.addComponent(TransformComponent.getComponentType(), new TransformComponent(pos, new Vector3f()));
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
            if (destroyed) return false;

            Ref<EntityStore> ref = emitterRef;
            if (ref == null || !ref.isValid()) return true;

            emitterRef = null;
            playing = false;

            Store<ChunkStore> chunkStore = blockRef.getStore();
            World world = chunkStore.getExternalData().getWorld();
            Store<EntityStore> entityStore = world.getEntityStore().getStore();

            world.execute(() -> {
                if (ref.isValid()) {
                    entityStore.removeEntity(ref, RemoveReason.REMOVE);
                }
            });

            debugger.atInfo("Stop success.");
            return true;
        }

        public void Destroy() {
            Stop();
            if (destroyed) return;
            destroyed = true;

            Set<MFTSound> set = SOUNDS_BY_BLOCK.get(blockRef.getIndex());
            if (set != null) {
                set.remove(this);
                if (set.isEmpty()) {
                    SOUNDS_BY_BLOCK.remove(blockRef.getIndex());
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

        private static @Nullable Vector3d getCenterWorldPos(Store<ChunkStore> chunkStore, Ref<ChunkStore> blockRef) {
            Vector3i blockPos = MFTBlockUtil.GetWorldPosFromBlockRef(chunkStore, blockRef);
            if (blockPos == null) return null;
            return blockPos.toVector3d().add(0.5, 0.5, 0.5);
        }
    }
}
