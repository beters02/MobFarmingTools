package com.bryce.mobfarmtools.util;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.google.protobuf.Any;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.List;

public class MFTDebugUtil {

    private static class DebuggerStat<T> {
        private T value;

        public DebuggerStat(T defaultValue) {
            this.value = defaultValue;
        }

        public T getValue() { return value; }
        public void setValue(T newValue) { value = newValue; }
    }

    public static class Debugger {

        private final String prefix;
        private boolean enabled = true;
        private HashMap<String, DebuggerStat<Object>> stats = new HashMap<>();

        public Debugger(String prefix) { this.prefix = prefix; }
        public Debugger(String prefix, boolean isEnabled) {
            this.prefix = prefix;
            this.enabled = isEnabled;
        }

        public void atInfo(String msg) {
            if (!enabled) return;
            MobFarmingToolsPlugin.LOGGER.atInfo().log(prefix+" "+msg);
        }

        public void atInfo(List<?> list) {
            list.forEach(v -> atInfo(String.valueOf(v)));
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

        public <T> void addStat(String name, T value) {
            if (stats.containsKey(name)) {
                return;
            }

            stats.put(name, new DebuggerStat<>(value));
        }

        public Object getStat(String name) {
            if (!stats.containsKey(name)) {
                return null;
            }

            return stats.get(name).getValue();
        }

        public void setStat(String name, Object value) {
            if (!stats.containsKey(name)) {
                return;
            }

            stats.get(name).setValue(value);
        }

    }

    public static void PrintList(List<?> list) {
        list.forEach(v -> MobFarmingToolsPlugin.LOGGER.atInfo().log(String.valueOf(v)));
    }

}
