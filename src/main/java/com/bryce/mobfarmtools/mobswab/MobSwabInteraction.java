package com.bryce.mobfarmtools.mobswab;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.protocol.InteractionState;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
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

        Ref<EntityStore> targetRef = context.getTargetEntity();
        if (targetRef == null) {
            player.sendMessage(Message.raw("Stored mob: " + meta.getMobId()));
            return;
        }

        if (!Objects.equals(meta.getMobId(), "None")) {
            player.sendMessage(Message.raw(
                    "This swab is already holding "
                    + meta.getMobId()
                    + " . Hold shift while right clicking to clear stored mob."
            ));
            return;
        }

        Ref<EntityStore> targetEntity = context.getTargetEntity();
        if (targetEntity == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        NPCEntity npc = targetEntity.getStore().getComponent(targetEntity, Objects.requireNonNull(NPCEntity.getComponentType()));
        if (npc == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        String entityId = EntityModule.get().getIdentifier(npc.getClass());
        if (entityId == null) {
            context.getState().state = InteractionState.Failed;
            return;
        }

        Inventory inventory = player.getInventory();
        byte slot = inventory.getActiveHotbarSlot();

        meta.setMobId(entityId);

        ItemStack updated = item.withMetadata(MobSwabMetadata.KEYED_CODEC, meta);
        inventory.getHotbar().replaceItemStackInSlot(slot, item, updated);

        player.sendMessage(Message.raw("Swabbed " + entityId));
    }
}
