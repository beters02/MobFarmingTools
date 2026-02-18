package com.bryce.mobfarmtools;

import com.bryce.mobfarmtools.chunks.ChunkForceTickSystem;
import com.bryce.mobfarmtools.chunks.ForcedChunkRefCountResource;
import com.bryce.mobfarmtools.config.MobFarmingToolsConfig;
import com.bryce.mobfarmtools.debugtool.DebugToolInteraction;
import com.bryce.mobfarmtools.dropper.DropperComponent;
import com.bryce.mobfarmtools.dropper.DropperSystem;
import com.bryce.mobfarmtools.entitydestroyer.EntityDestroyerInteraction;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeComponent;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanInitializer;
import com.bryce.mobfarmtools.mobfan.MobFanOpenInteraction;
import com.bryce.mobfarmtools.mobfan.MobFanSystem;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerInitializer;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerInteraction;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerSystem;
import com.bryce.mobfarmtools.mobswab.MobSwabInteraction;
import com.bryce.mobfarmtools.sounds.MFTSoundEmitterComponent;
import com.bryce.mobfarmtools.sounds.MFTSoundEmitterInitializer;
import com.bryce.mobfarmtools.sounds.MFTSoundEmitterSystem;
import com.bryce.mobfarmtools.spikes.SpikesComponent;
import com.bryce.mobfarmtools.spikes.SpikesSystem;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperComponent;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperInitializer;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperInteraction;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperSystem;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.logger.HytaleLogger;
import com.hypixel.hytale.server.core.command.system.CommandRegistry;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.Interaction;
import com.hypixel.hytale.server.core.plugin.JavaPlugin;
import com.hypixel.hytale.server.core.plugin.JavaPluginInit;
import com.hypixel.hytale.server.core.plugin.registry.CodecMapRegistry;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.util.Config;

import javax.annotation.Nonnull;

public class MobFarmingToolsPlugin extends JavaPlugin {
    protected static MobFarmingToolsPlugin instance;
    public static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private final String modScopeId = "Mob_Farming_Tools";

    private ComponentType<ChunkStore, MobFanComponent> mobFanComponentType;
    private ComponentType<ChunkStore, MobSpawnerComponent> mobSpawnerComponentType;
    private ComponentType<ChunkStore, VacuumHopperComponent> vacuumHopperComponentType;
    private ComponentType<ChunkStore, MachineUpgradeComponent> machineUpgradeComponentType;
    private ComponentType<ChunkStore, SpikesComponent> spikesComponentType;
    private ComponentType<ChunkStore, DropperComponent> dropperComponentType;
    private ComponentType<ChunkStore, MFTSoundEmitterComponent> soundEmitterComponentType;

    private ResourceType<ChunkStore, ForcedChunkRefCountResource> forcedChunkRefCountResourceType;

    private final Config<MobFarmingToolsConfig> mftConfig = this.withConfig("MobFarmingToolsConfig", MobFarmingToolsConfig.CODEC);

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
        registerResources(this.getChunkStoreRegistry());
        registerSystems(this.getChunkStoreRegistry());
        registerConfigs();
    }

    private void registerCommands(CommandRegistry registry) {
    }

    private void registerComponents(ComponentRegistryProxy<ChunkStore> registry) {
        this.mobFanComponentType = registerComponent(registry, MobFanComponent.class, "Mob_Fan_Component", MobFanComponent.CODEC);
        this.mobSpawnerComponentType = registerComponent(registry, MobSpawnerComponent.class, "Mob_Spawner_Component", MobSpawnerComponent.CODEC);
        this.vacuumHopperComponentType = registerComponent(registry, VacuumHopperComponent.class, "Vacuum_Hopper_Component", VacuumHopperComponent.CODEC);
        this.machineUpgradeComponentType = registerComponent(registry, MachineUpgradeComponent.class, "Machine_Upgrade_Component", MachineUpgradeComponent.CODEC);
        this.spikesComponentType = registerComponent(registry, SpikesComponent.class, "Spikes_Component", SpikesComponent.CODEC);
        this.dropperComponentType = registerComponent(registry, DropperComponent.class, "Dropper_Component", DropperComponent.CODEC);
        this.soundEmitterComponentType = registerComponent(registry, MFTSoundEmitterComponent.class, "Sound_Emitter_Component", MFTSoundEmitterComponent.CODEC);
    }

    private void registerResources(ComponentRegistryProxy<ChunkStore> registry) {
        this.forcedChunkRefCountResourceType = registerResource(registry, ForcedChunkRefCountResource.class, "Forced_Chunk_Ref_Count_Resource", ForcedChunkRefCountResource.CODEC);
    }

    private void registerSystems(ComponentRegistryProxy<ChunkStore> registry) {
        registry.registerSystem(new MobFanSystem(this.mobFanComponentType));
        registry.registerSystem(new MobFanInitializer());
        registry.registerSystem(new MobSpawnerSystem(this.mobSpawnerComponentType));
        registry.registerSystem(new MobSpawnerInitializer());
        registry.registerSystem(new VacuumHopperSystem(this.vacuumHopperComponentType));
        registry.registerSystem(new VacuumHopperInitializer());
        registry.registerSystem(new SpikesSystem(this.spikesComponentType));
        registry.registerSystem(new DropperSystem(this.dropperComponentType));
        registry.registerSystem(new ChunkForceTickSystem());
        registry.registerSystem(new MFTSoundEmitterSystem(this.soundEmitterComponentType));
        registry.registerSystem(new MFTSoundEmitterInitializer());
    }

    private void registerInteractions(
            CodecMapRegistry.Assets<
                    Interaction,
                    ? extends Codec<? extends Interaction>
                    > registry
    ) {
        registerInteraction(registry, "Open_Mob_Fan_Interaction", MobFanOpenInteraction.class, MobFanOpenInteraction.CODEC);
        registerInteraction(registry, "Mob_Swab_Interaction", MobSwabInteraction.class, MobSwabInteraction.CODEC);
        registerInteraction(registry, "Debug_Tool_Interaction", DebugToolInteraction.class, DebugToolInteraction.CODEC);
        registerInteraction(registry, "Mob_Spawner_Interaction", MobSpawnerInteraction.class, MobSpawnerInteraction.CODEC);
        registerInteraction(registry, "Entity_Destroyer_Interaction", EntityDestroyerInteraction.class, EntityDestroyerInteraction.CODEC);
        registerInteraction(registry, "Vacuum_Hopper_Interaction", VacuumHopperInteraction.class, VacuumHopperInteraction.CODEC);
    }

    private void registerConfigs() {
        mftConfig.save();
    }

    private void registerInteraction(
            CodecMapRegistry.Assets<
                    Interaction,
                    ? extends Codec<? extends Interaction>
                    > registry,
            String id,
            Class<? extends Interaction> interactionClass,
            BuilderCodec<? extends Interaction> codec
    ) {
        registry.register(modScopeId + ":" + id, interactionClass, codec);
    }

    private <T extends Component<ChunkStore>> ComponentType<ChunkStore, T> registerComponent(
            ComponentRegistryProxy<ChunkStore> registry,
            @Nonnull Class<? super T> componentClass,
            @Nonnull String id,
            @Nonnull BuilderCodec<T> codec
    ) {
        return registry.registerComponent(componentClass,modScopeId + ":" + id, codec);
    }

    private <T extends Resource<ChunkStore>> ResourceType<ChunkStore, T> registerResource(
            ComponentRegistryProxy<ChunkStore> registry,
            @Nonnull Class<? super T> resourceClass,
            @Nonnull String id,
            @Nonnull BuilderCodec<T> codec
    ) {
        return registry.registerResource(resourceClass,modScopeId + "_" + id, codec);
    }

    public ComponentType<ChunkStore, MobFanComponent> getMobFanComponentType() {
        return this.mobFanComponentType;
    }
    public ComponentType<ChunkStore, MobSpawnerComponent> getMobSpawnerComponentType() { return this.mobSpawnerComponentType; }
    public ComponentType<ChunkStore, VacuumHopperComponent> getVacuumHopperComponentType() { return this.vacuumHopperComponentType; }
    public ComponentType<ChunkStore, MachineUpgradeComponent> getMachineUpgradeComponentType() { return this.machineUpgradeComponentType; }
    public ComponentType<ChunkStore, SpikesComponent> getSpikesComponentType() {
        return this.spikesComponentType;
    }
    public ComponentType<ChunkStore, DropperComponent> getDropperComponentType() {
        return this.dropperComponentType;
    }
    public ComponentType<ChunkStore, MFTSoundEmitterComponent> getSoundEmitterComponentType() { return this.soundEmitterComponentType; }

    public ResourceType<ChunkStore, ForcedChunkRefCountResource> getForcedChunkRefCountResourceType() { return this.forcedChunkRefCountResourceType; }

    public Config<MobFarmingToolsConfig> getMobFarmingToolsConfig() {
        return mftConfig;
    }
}
