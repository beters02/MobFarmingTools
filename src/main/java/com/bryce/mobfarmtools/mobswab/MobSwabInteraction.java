package com.bryce.mobfarmtools.mobswab;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.config.MobFarmingToolsConfig;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.util.MFTEntityUtil;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.*;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

public class MobSwabInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<MobSwabInteraction> CODEC = BuilderCodec.builder(
            MobSwabInteraction.class, MobSwabInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[MobSwabInteraction]");

    public MobSwabInteraction() {
        debugger.setEnabled(false);
    }

    @Nullable
    private static <T> T requireOrFail(@Nullable T value, InteractionContext context) {
        if (value == null) context.getState().state = InteractionState.Failed;
        return value;
    }

    @Nullable
    private <T> T getOrInfo(@Nullable T value, Player player, MobSwabMetadata meta) {
        if (value == null || value == "None") sendInfoMessage(player, meta);
        return value;
    }

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext context, @NonNull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = requireOrFail(context.getCommandBuffer(), context);
        if (commandBuffer == null) return;

        ItemStack held = requireOrFail(context.getHeldItem(), context);
        if (held == null) return;

        Ref<EntityStore> userRef = context.getEntity();
        Player player = requireOrFail(commandBuffer.getComponent(userRef, Player.getComponentType()), context);
        if (player == null) return;

        MobSwabMetadata meta = getMetaData(held);

        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock != null) {
            handleBlockTargetOrInfo(targetBlock, meta, player, userRef);
            return;
        }

        handleEntityTargetOrInfo(context, player, meta, held);
    }

    private void handleBlockTargetOrInfo(
            BlockPosition block,
            MobSwabMetadata meta,
            Player player,
            Ref<EntityStore> playerRef
    ) {

        World world = playerRef.getStore().getExternalData().getWorld();
        WorldChunk chunk = getOrInfo(world.getChunk(ChunkUtil.indexChunkFromBlock(block.x, block.z)), player, meta);
        if (chunk == null) return;

        Ref<ChunkStore> blockRef = getOrInfo(chunk.getBlockComponentEntity(block.x, block.y, block.z), player, meta);
        if (blockRef == null) return;

        Store<ChunkStore> chunkStore = blockRef.getStore();
        MobSpawnerComponent spawner = getOrInfo(
                chunkStore.getComponent(blockRef, MobSpawnerComponent.getComponentType()),
                player,
                meta
        );
        if (spawner == null) return;

        String entityId = getOrInfo(meta.getMobId(), player, meta);
        if (Objects.equals(entityId, "None")) return;

        swabSpawnerAction(spawner, player, meta, entityId);
    }

    private void swabSpawnerAction(MobSpawnerComponent spawnerComponent, Player player, MobSwabMetadata meta, String entityId) {
        if (isEntityIdBlacklisted(entityId)) {
            player.sendMessage(Message.raw("Applying " + entityId + " to spawner has been disabled."));
            return;
        }

        spawnerComponent.setEntityId(entityId);
        spawnerComponent.setEntitySize(meta.getFixedEntitySize());

        if (player.getGameMode() != GameMode.Creative) {
            Inventory inventory = player.getInventory();
            byte slot = inventory.getActiveHotbarSlot();
            inventory.getHotbar().removeItemStackFromSlot(slot);
        }

        player.sendMessage(Message.raw("Changed spawner entity to " + entityId));
    }

    private void handleEntityTargetOrInfo(
            InteractionContext context,
            Player player,
            MobSwabMetadata meta,
            ItemStack heldItem
    ) {
        Ref<EntityStore> targetEntity = getOrInfo(context.getTargetEntity(), player, meta);
        if (targetEntity == null) return;

        if (!Objects.equals(meta.getMobId(), "None")) {
            player.sendMessage(Message.raw("This swab is already holding " + meta.getMobId()));
            return;
        }

        ComponentType<EntityStore, NPCEntity> npcEntityComponentType = getOrInfo(
                NPCEntity.getComponentType(),
                player,
                meta
        );
        if (npcEntityComponentType == null) return;

        Store<EntityStore> entityStore = targetEntity.getStore();
        NPCEntity npc = getOrInfo(entityStore.getComponent(targetEntity, npcEntityComponentType), player, meta);
        if (npc == null) return;

        swabEntityAction(heldItem, player, meta, npc);
    }

    private void swabEntityAction(ItemStack heldItem, Player player, MobSwabMetadata meta, NPCEntity npc) {
        String entityId = npc.getNPCTypeId();

        if (isEntityIdBlacklisted(entityId)) {
            player.sendMessage(Message.raw("Swabbing " + entityId + " has been disabled."));
            return;
        }

        // append new values to metadata
        meta.setMobId(entityId);
        meta.setEntitySize(getEntitySize(npc));

        // update the item stack
        updateSwabInHandWithMetadata(player, new ItemStack("Mob_Swab_Used", 1), heldItem, meta);

        player.sendMessage(Message.raw("Swabbed " + entityId));
    }

    // HELPERS
    private void sendInfoMessage(Player player, MobSwabMetadata meta) {
        player.sendMessage(Message.raw("Stored mob: " + meta.getMobId()));
    }

    private @NonNull Vector3d getEntitySize(NPCEntity npc) {
        Vector3d size = MFTEntityUtil.GetNPCEntitySize(npc);
        if (size == null) {
            debugger.atWarning("Could not get entity size on mob swab. Setting to 1");
            size = new Vector3d(1, 1, 1);
        }
        return size;
    }

    private @NonNull MobSwabMetadata getMetaData(ItemStack item) {
        return item.getFromMetadataOrDefault(MobSwabMetadata.KEY, MobSwabMetadata.CODEC);
    }

    private void updateSwabInHandWithMetadata(Player player, ItemStack itemToAdd, ItemStack itemToRemove, MobSwabMetadata meta) {
        Inventory inventory = player.getInventory();
        byte slot = inventory.getActiveHotbarSlot();
        ItemStack updatedItem = itemToAdd.withMetadata(MobSwabMetadata.KEYED_CODEC, meta);
        inventory.getHotbar().replaceItemStackInSlot(slot, itemToRemove, updatedItem);
    }

    private boolean isEntityIdBlacklisted(String entityId) {
        MobFarmingToolsConfig config = MobFarmingToolsPlugin.get().getMobFarmingToolsConfig().get();
        return config.isEntityBlacklistedSpawner(entityId);
    }
}