package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.entitydestroyer.EntityDestroyerInteraction;
import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTMathUtil;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.client.SimpleBlockInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class VacuumHopperInteraction extends SimpleBlockInteraction {
    public static final BuilderCodec<VacuumHopperInteraction> CODEC = BuilderCodec.builder(
            VacuumHopperInteraction.class, VacuumHopperInteraction::new, SimpleBlockInteraction.CODEC
    ).build();

    @Override
    protected void interactWithBlock(
            @NonNull World world,
            @NonNull CommandBuffer<EntityStore> commandBuffer,
            @NonNull InteractionType interactionType,
            @NonNull InteractionContext context,
            @Nullable ItemStack itemStack,
            @NonNull Vector3i vector3i,
            @NonNull CooldownHandler cooldownHandler
    ) {

        Ref<ChunkStore> blockEntityRef = MFTBlockUtil.GetBlockEntityRefFromInteractionContext(context);
        if (blockEntityRef == null) return;

        VacuumHopperComponent vacuum = blockEntityRef.getStore().getComponent(blockEntityRef, VacuumHopperComponent.getComponentType());
        if (vacuum == null) return;

        Ref<EntityStore> userRef = context.getEntity();
        Player player = userRef.getStore().getComponent(userRef, Player.getComponentType());
        if (player == null) return;

        vacuum.setHasAvailableContainer(VacuumHopperHelpers.HasAvailableItemContainer(world, vector3i));
        player.sendMessage(Message.raw("Has available container: "+vacuum.hasAvailableContainer()));
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull World world, @NonNull Vector3i vector3i) {

    }
}
