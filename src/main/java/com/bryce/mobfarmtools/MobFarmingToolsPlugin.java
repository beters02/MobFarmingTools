package com.bryce.mobfarmtools;

import com.bryce.mobfarmtools.debugtool.DebugToolInteraction;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanInitializer;
import com.bryce.mobfarmtools.mobfan.MobFanOpenInteraction;
import com.bryce.mobfarmtools.mobfan.MobFanSystem;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerInitializer;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerSystem;
import com.bryce.mobfarmtools.mobswab.MobSwabInteraction;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.component.ComponentRegistryProxy;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.registry.CodecMapRegistry;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;

public class MobFarmingToolsPlugin extends JavaPlugin {
    protected static MobFarmingToolsPlugin instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final String modScopeId = "Mob_Farming_Tools";

    private ComponentType<ChunkStore, MobFanComponent> mobFanComponentType;
    private ComponentType<ChunkStore, MobSpawnerComponent> mobSpawnerComponentType;

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
        registerInteractions(this.getCodecRegistry(Interaction.CODEC));
        registerComponents(this.getChunkStoreRegistry());
        registerSystems(this.getChunkStoreRegistry());
    }

    private void registerCommands(CommandRegistry registry) {
    }

    private void registerComponents(ComponentRegistryProxy<ChunkStore> registry) {
        this.mobFanComponentType = registry.registerComponent(
                MobFanComponent.class, modScopeId+":Mob_Fan_Component", MobFanComponent.CODEC
        );
        this.mobSpawnerComponentType = registry.registerComponent(
                MobSpawnerComponent.class, modScopeId+":Mob_Spawner_Component", MobSpawnerComponent.CODEC
        );
    }

    private void registerSystems(ComponentRegistryProxy<ChunkStore> registry) {
        registry.registerSystem(new MobFanSystem(this.mobFanComponentType));
        registry.registerSystem(new MobFanInitializer());
        registry.registerSystem(new MobSpawnerSystem(this.mobSpawnerComponentType));
        registry.registerSystem(new MobSpawnerInitializer());
    }

    void registerInteractions(
            CodecMapRegistry.Assets<
                    Interaction,
                    ? extends Codec<? extends Interaction>
                    > registry
    ) {
        registry.register(modScopeId+":Open_Mob_Fan_Interaction", MobFanOpenInteraction.class, MobFanOpenInteraction.CODEC);
        registry.register(modScopeId+":Mob_Swab_Interaction", MobSwabInteraction.class, MobSwabInteraction.CODEC);
        registry.register(modScopeId+":Debug_Tool_Interaction", DebugToolInteraction.class, DebugToolInteraction.CODEC);
    }

    public ComponentType<ChunkStore, MobFanComponent> getMobFanComponentType() {
        return this.mobFanComponentType;
    }

    public ComponentType<ChunkStore, MobSpawnerComponent> getMobSpawnerComponentType() {
        return this.mobSpawnerComponentType;
    }
}
