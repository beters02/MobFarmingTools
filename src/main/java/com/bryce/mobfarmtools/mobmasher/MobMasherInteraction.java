package com.bryce.mobfarmtools.mobmasher;

import com.bryce.mobfarmtools.mobmasher.ui.MobMasherUpgradePage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

public class MobMasherInteraction extends SimpleBlockInteraction {

    public static final BuilderCodec<MobMasherInteraction> CODEC =
            BuilderCodec.builder(
                    MobMasherInteraction.class,
                    MobMasherInteraction::new
            ).build();

    @Override
    protected void interactWithBlock(@NonNull World world, @NonNull CommandBuffer<EntityStore> commandBuffer, @NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull Vector3i vector3i, @NonNull CooldownHandler cooldownHandler) {
        Ref<EntityStore> playerRefEntityStore = interactionContext.getEntity();
        Player player = commandBuffer.getComponent(playerRefEntityStore, Player.getComponentType());
        if (player == null) {
            return;
        }

        PlayerRef playerRef = commandBuffer.getComponent(playerRefEntityStore, PlayerRef.getComponentType());
        if (playerRef == null) {
            return;
        }

        BlockPosition targetBlock = interactionContext.getTargetBlock();
        if (targetBlock == null) {
            return;
        }

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z));
        if (chunk == null) {
            return;
        }

        Ref<ChunkStore> blockEntityRef = chunk.getBlockComponentEntity(targetBlock.x, targetBlock.y, targetBlock.z);
        if (blockEntityRef == null) {
            return;
        }

        int rotationIndex = world.getBlockRotationIndex(targetBlock.x, targetBlock.y, targetBlock.z);
        MobMasherUpgradePage page = new MobMasherUpgradePage(playerRef, blockEntityRef, targetBlock, rotationIndex);
        player.getPageManager().openCustomPage(playerRefEntityStore, playerRefEntityStore.getStore(), page);
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull World world, @NonNull Vector3i vector3i) {

    }
}
