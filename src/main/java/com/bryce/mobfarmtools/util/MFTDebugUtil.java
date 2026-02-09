package com.bryce.mobfarmtools.util;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class MFTDebugUtil {

    public static class Debugger {

        private final String prefix;
        private boolean enabled = true;

        public Debugger(String prefix) { this.prefix = prefix; }

        public void atInfo(String msg) {
            if (!enabled) return;
            MobFarmingToolsPlugin.LOGGER.atInfo().log(prefix+" "+msg);
        }

        public void atWarning(String msg) {
            if (!enabled) return;
            MobFarmingToolsPlugin.LOGGER.atWarning().log(prefix+" "+msg);
        }

        public void atSevere(String msg) {
            if (!enabled) return;
            MobFarmingToolsPlugin.LOGGER.atSevere().log(prefix+" "+msg);
        }

        public void setEnabled(boolean enabled) { this.enabled = enabled; }

        @Nullable
        public <T> T require(@Nullable T value, String msg) {
            if (value == null) this.atWarning(msg);
            return value;
        }

        public boolean requireBool(boolean value, String msg) {
            if (!value) this.atWarning(msg);
            return value;
        }

    }

    public static void PrintList(List<?> list) {
        list.forEach(v -> MobFarmingToolsPlugin.LOGGER.atWarning().log(String.valueOf(v)));
    }

}
