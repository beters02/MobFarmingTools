package com.bryce.mobfarmtools;

import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanInitializer;
import com.bryce.mobfarmtools.mobfan.MobFanSystem;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class MobFarmingToolsPlugin extends JavaPlugin {
    protected static MobFarmingToolsPlugin instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private ComponentType<ChunkStore, MobFanComponent> mobFanComponentType;

    public static MobFarmingToolsPlugin get() {
        return instance;
    }

    public MobFarmingToolsPlugin(JavaPluginInit init) {
        super(init);
        LOGGER.atInfo().log("Initialized %s version %s", this.getName(), this.getManifest().getVersion().toString());
    }

    @Override
    protected void setup() {
        instance = this;

        registerCommands(this.getCommandRegistry());
        registerComponents(this.getChunkStoreRegistry());
        registerSystems(this.getChunkStoreRegistry());
    }

    private void registerCommands(CommandRegistry registry) {
    }

    private void registerComponents(ComponentRegistryProxy<ChunkStore> registry) {
        this.mobFanComponentType = registry.registerComponent(
                MobFanComponent.class, "Mob_Farming_Tools:Mob_Fan_Component", MobFanComponent.CODEC);
    }

    private void registerSystems(ComponentRegistryProxy<ChunkStore> registry) {
        registry.registerSystem(new MobFanSystem(this.mobFanComponentType));
        registry.registerSystem(new MobFanInitializer());
    }

    public ComponentType<ChunkStore, MobFanComponent> getMobFanComponentType() {
        return this.mobFanComponentType;
    }
}
