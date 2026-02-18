package com.bryce.mobfarmtools.util;

import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEvent;
import com.hypixel.hytale.server.core.asset.type.soundevent.config.SoundEventLayer;
import com.hypixel.hytale.server.core.modules.entity.component.AudioComponent;

public class MFTSoundUtil {

    public static boolean isSoundEventLooping(String soundEventId) {
        int index = SoundEvent.getAssetMap().getIndex(soundEventId);
        if (index == 0) return false;

        SoundEvent sound = SoundEvent.getAssetMap().getAsset(index);
        if (sound == null || sound.getLayers() == null) return false;

        for (SoundEventLayer layer : sound.getLayers()) {
            if (layer != null && layer.isLooping()) {
                return true; // any looping layer means the event can loop
            }
        }
        return false;
    }

}
