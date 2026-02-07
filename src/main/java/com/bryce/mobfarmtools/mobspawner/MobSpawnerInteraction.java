package com.bryce.mobfarmtools.mobspawner;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MobSpawnerInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<MobSpawnerInteraction> CODEC =
            BuilderCodec.builder(
                    MobSpawnerInteraction.class,
                    MobSpawnerInteraction::new
            ).build();

    @Override
    protected void interactWithBlock(@NonNull World world, @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull Vector3i vector3i, @NonNull CooldownHandler cooldownHandler) {
        MobFarmingToolsPlugin.LOGGER.atInfo().log("PLAYER INTERACTED WITH BLOCK");

        BlockPosition targetBlock = interactionContext.getTargetBlock();
        if (targetBlock == null) { return; }

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z));
        if (chunk == null) { return; }

        Ref<ChunkStore> blockEntityRef = chunk.getBlockComponentEntity(targetBlock.x, targetBlock.y, targetBlock.z);
        if (blockEntityRef == null) { return; }

        MobSpawnerComponent spawnerComponent = blockEntityRef.getStore().getComponent(blockEntityRef, MobSpawnerComponent.getComponentType());
        if (spawnerComponent == null) { return; }

        Ref<EntityStore> playerRefEntityStore = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(playerRefEntityStore, Player.getComponentType());
        if (player == null) { return; }

        spawnerComponent.sendInfoMessage(player);
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull World world, @NonNull Vector3i vector3i) {

    }
}
