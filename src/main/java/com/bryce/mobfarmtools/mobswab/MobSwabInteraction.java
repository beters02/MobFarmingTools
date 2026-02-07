package com.bryce.mobfarmtools.mobswab;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.bryce.mobfarmtools.util.MFTSpawnerUtil;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.item.config.Item;
import com.hypixel.hytale.server.core.entity.Entity;
import com.hypixel.hytale.server.core.entity.EntityUtils;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.LivingEntity;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.Objects;

public class MobSwabInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<MobSwabInteraction> CODEC = BuilderCodec.builder(
            MobSwabInteraction.class, MobSwabInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext context, @NonNull CooldownHandler cooldownHandler) {
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        ItemStack item = context.getHeldItem();
        if (item == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Ref<EntityStore> userRef = context.getEntity();
        Player player = commandBuffer.getComponent(userRef, Player.getComponentType());
        if (player == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        MobSwabMetadata meta = item.getFromMetadataOrDefault(MobSwabMetadata.KEY, MobSwabMetadata.CODEC);

        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock != null) {
            onTargetBlock(context, targetBlock, meta, player, userRef);
            return;
        }

        Ref<EntityStore> targetEntity = context.getTargetEntity();
        if (targetEntity == null) {
            player.sendMessage(Message.raw("Stored mob: " + meta.getMobId()));
            return;
        }

        if (!Objects.equals(meta.getMobId(), "None")) {
            player.sendMessage(Message.raw(
                    "This swab is already holding "
                    + meta.getMobId()
            ));
            return;
        }

        NPCEntity npc = targetEntity.getStore().getComponent(targetEntity, Objects.requireNonNull(NPCEntity.getComponentType()));
        if (npc == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        String entityId = npc.getNPCTypeId();

        Vector3d size = MFTSpawnerUtil.GetNPCEntitySize(npc);
        if (size == null) {
            MobFarmingToolsPlugin.LOGGER.atWarning().log("Could not get entity size on mob swab. Setting to 1");
            size = new Vector3d(1,1,1);
        }

        meta.setEntitySize(size);

        Inventory inventory = player.getInventory();
        byte slot = inventory.getActiveHotbarSlot();

        meta.setMobId(entityId);

        ItemStack updated = item.withMetadata(MobSwabMetadata.KEYED_CODEC, meta);
        inventory.getHotbar().replaceItemStackInSlot(slot, item, updated);

        player.sendMessage(Message.raw("Swabbed " + entityId));
    }

    private void onTargetBlock(
            InteractionContext context,
            BlockPosition targetBlock,
            MobSwabMetadata meta,
            Player player,
            Ref<EntityStore> playerRef
    ) {

        World world = playerRef.getStore().getExternalData().getWorld();
        WorldChunk chunk = world.getChunk(ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z));

        if (chunk == null) {
            return;
        }

        Ref<ChunkStore> blockRef = chunk.getBlockComponentEntity(targetBlock.x, targetBlock.y, targetBlock.z);
        if (blockRef == null) {
            return;
        }

        Store<ChunkStore> chunkStore = blockRef.getStore();
        MobSpawnerComponent spawnerComponent = chunkStore.getComponent(blockRef, MobSpawnerComponent.getComponentType());
        if (spawnerComponent == null) {
            return;
        }

        String entityId = meta.getMobId();
        if (Objects.equals(entityId, "None")) {
            return;
        }

        Vector3i entitySize = meta.getFixedEntitySize();

        spawnerComponent.setEntityId(entityId);
        spawnerComponent.setEntitySize(entitySize);

        Inventory inventory = player.getInventory();
        byte slot = inventory.getActiveHotbarSlot();
        inventory.getHotbar().removeItemStackFromSlot(slot);

        player.sendMessage(Message.raw("Changed spawner entity to " + entityId));
    }
}