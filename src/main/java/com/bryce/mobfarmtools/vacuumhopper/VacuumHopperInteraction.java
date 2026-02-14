package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.machineupgrade.ui.MachineUpgradePage;
import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.vacuumhopper.ui.VacuumHopperUpgradePage;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
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
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.EnumMap;

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
        Player player = commandBuffer.getComponent(userRef, Player.getComponentType());
        if (player == null) return;

        PlayerRef playerRef = commandBuffer.getComponent(userRef, PlayerRef.getComponentType());
        if (playerRef == null) return;

        BlockPosition targetBlock = context.getTargetBlock();
        if (targetBlock == null) return;

        int rotationIndex = world.getBlockRotationIndex(targetBlock.x, targetBlock.y, targetBlock.z);
        VacuumHopperUpgradePage page = new VacuumHopperUpgradePage(
                playerRef,
                blockEntityRef,
                targetBlock,
                rotationIndex
        );
        player.getPageManager().openCustomPage(userRef, userRef.getStore(), page);
    }

    @Override
    protected void simulateInteractWithBlock(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @Nullable ItemStack itemStack, @NonNull World world, @NonNull Vector3i vector3i) {

    }
}
