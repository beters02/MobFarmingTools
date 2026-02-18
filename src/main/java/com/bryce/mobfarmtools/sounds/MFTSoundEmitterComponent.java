package com.bryce.mobfarmtools.sounds;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperComponent;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.Nullable;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class MFTSoundEmitterComponent implements Component<ChunkStore> {
    public static final BuilderCodec<MFTSoundEmitterComponent> CODEC =
            BuilderCodec.builder(MFTSoundEmitterComponent.class, MFTSoundEmitterComponent::new)
                    .append(new KeyedCodec<>("SoundIds", Codec.STRING_ARRAY),
                            (component, value) -> component.soundIds = value,
                            component -> component.soundIds)
                    .add()
                    .append(new KeyedCodec<>("NoiseSuppressed", Codec.BOOLEAN),
                            (component, value) -> component.suppressed = value,
                            component -> component.suppressed)
                    .add()
                    .build();

    private String[] soundIds;
    private final Set<String> pendingPlay = new HashSet<>();
    private final Set<String> pendingStop = new HashSet<>();
    private Ref<ChunkStore> storedBlockRef;

    private boolean initialized = false;
    private boolean suppressed = false;
    private MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[SoundEmitterComponent]", true);

    public MFTSoundEmitterComponent() {}
    public MFTSoundEmitterComponent(String[] soundIds) {
        this.soundIds = soundIds;
    }

    public SoundManager.@Nullable MFTSound GetSound(Ref<ChunkStore> blockRef, String soundId) {
        if (storedBlockRef == null) storedBlockRef = blockRef;
        if (!initialized) return null;
        Map<String, SoundManager.MFTSound> sounds = SoundManager.GetAllForBlockAsMap(blockRef);
        if (sounds == null) return null;
        return sounds.get(soundId);
    }

    public boolean PlaySound(Ref<ChunkStore> blockRef, SoundManager.MFTSound sound) {
        if (storedBlockRef == null) storedBlockRef = blockRef;
        if (suppressed) return false;
        if (sound == null) {
            debugger.atWarning("Tried to play sound but sound is null.");
            return false;
        }
        if (!initialized) {
            pendingPlay.add(sound.getSoundEventId());
            return false;
        }

        return sound.Play();
    }

    public boolean StopSound(Ref<ChunkStore> blockRef, SoundManager.MFTSound sound) {
        if (storedBlockRef == null) storedBlockRef = blockRef;
        if (sound == null) {
            debugger.atWarning("Tried to stop sound but sound is null.");
            return false;
        }
        if (!initialized) {
            pendingStop.add(sound.getSoundEventId());
            return false;
        }

        return sound.Stop();
    }

    public boolean PlaySound(Ref<ChunkStore> blockRef, String soundId) {
        if (storedBlockRef == null) storedBlockRef = blockRef;
        if (suppressed) return false;
        if (!initialized) {
            pendingPlay.add(soundId);
            return false;
        }
        return PlaySound(blockRef, GetSound(blockRef, soundId));
    }

    public boolean StopSound(Ref<ChunkStore> blockRef, String soundId) {
        if (storedBlockRef == null) storedBlockRef = blockRef;
        if (!initialized) {
            pendingStop.add(soundId);
            return false;
        }
        return StopSound(blockRef, GetSound(blockRef, soundId));
    }

    public String[] getSoundIds() { return soundIds; }
    public Set<String> getPendingPlay() { return pendingPlay; }
    public Set<String> getPendingStop() { return pendingStop; }
    public boolean isPendingPlayEmpty() { return pendingPlay.isEmpty(); }
    public boolean isPendingStopEmpty() { return pendingStop.isEmpty(); }
    public boolean isSuppressed() { return suppressed; }
    public boolean isInitialized() { return initialized; }
    public Ref<ChunkStore> getStoredBlockRef() { return storedBlockRef; }

    public void setInitialized(boolean value) { initialized = value; }
    public void clearPendingPlay() { pendingPlay.clear(); }
    public void clearPendingStop() { pendingStop.clear(); }
    public void setSuppressed(boolean value, Ref<ChunkStore> blockRef) {
        suppressed = value;
        if (!suppressed) {
            SoundManager.StopAllForBlock(blockRef);
        }
    }

    public static ComponentType<ChunkStore, MFTSoundEmitterComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getSoundEmitterComponentType();
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        MFTSoundEmitterComponent copy = new MFTSoundEmitterComponent();
        copy.soundIds = soundIds;
        copy.suppressed = suppressed;
        return copy;
    }
}
