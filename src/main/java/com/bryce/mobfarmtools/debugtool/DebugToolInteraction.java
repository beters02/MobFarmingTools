package com.bryce.mobfarmtools.debugtool;

import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;

public class DebugToolInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<DebugToolInteraction> CODEC = BuilderCodec.builder(
            DebugToolInteraction.class, DebugToolInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext ctx, @NonNull CooldownHandler cooldownHandler) {
        BlockPosition targetBlock = ctx.getTargetBlock();
        if (targetBlock == null) return;

        Ref<EntityStore> playerRef = ctx.getEntity();
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
        if (spawnerComponent != null) {
            Player player = playerRef.getStore().getComponent(playerRef, Player.getComponentType());
            if (player != null) {
                spawnerComponent.printDebug(player);
            }
            return;
        }

        MobFanComponent fanComponent = chunkStore.getComponent(blockRef, MobFanComponent.getComponentType());
        if (fanComponent != null) {
            //TODO: add debug info for mobfan
            return;
        }
    }

}
