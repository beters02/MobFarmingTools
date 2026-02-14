package com.bryce.mobfarmtools.util;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.math.block.BlockUtil;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.BlockChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Predicate;

public class MFTBlockUtil {

    public static @Nullable Vector3i GetWorldPosFromBlockRef(ComponentAccessor<ChunkStore> accessor, Ref<ChunkStore> blockRef) {
        BlockModule.BlockStateInfo info = accessor.getComponent(blockRef, BlockModule.BlockStateInfo.getComponentType());
        if (info == null) return null;

        Ref<ChunkStore> chunkRef = info.getChunkRef();
        if (!chunkRef.isValid()) return null;

        WorldChunk chunk = accessor.getComponent(chunkRef, WorldChunk.getComponentType());
        if (chunk == null) return null;

        int index = info.getIndex();
        int localX = ChunkUtil.xFromBlockInColumn(index);
        int y      = ChunkUtil.yFromBlockInColumn(index); // already world Y in this encoding
        int localZ = ChunkUtil.zFromBlockInColumn(index);

        int x = ChunkUtil.worldCoordFromLocalCoord(chunk.getX(), localX);
        int z = ChunkUtil.worldCoordFromLocalCoord(chunk.getZ(), localZ);

        return new Vector3i(x, y, z);
    }

    public static @Nullable Vector3d GetWorldPosFromBlockStateInfo(BlockModule.BlockStateInfo info) {
        int blockIndex = info.getIndex();

        int localX = ChunkUtil.xFromBlockInColumn(blockIndex);
        int worldY = ChunkUtil.yFromBlockInColumn(blockIndex);
        int localZ = ChunkUtil.zFromBlockInColumn(blockIndex);

        Store<ChunkStore> chunkStore = info.getChunkRef().getStore();
        WorldChunk worldChunk = chunkStore.getComponent(info.getChunkRef(), WorldChunk.getComponentType());

        if (worldChunk == null) {
            return null;
        }

        int chunkX = worldChunk.getX();
        int chunkZ = worldChunk.getZ();

        int worldX = ChunkUtil.worldCoordFromLocalCoord(chunkX, localX);
        int worldZ = ChunkUtil.worldCoordFromLocalCoord(chunkZ, localZ);

        return new Vector3d(worldX, worldY, worldZ);
    }

    public static BlockModule.@Nullable BlockStateInfo GetBlockStateInfoFromArchetype(
            ArchetypeChunk<ChunkStore> archetypeChunk,
            int blockIndex
    ) {
        return archetypeChunk.getComponent(blockIndex, BlockModule.BlockStateInfo.getComponentType());
    }

    public static boolean PositionIsEmpty(Store<ChunkStore> chunkStore, Vector3i pos) {
        World world = chunkStore.getExternalData().getWorld();

        int chunkX = ChunkUtil.chunkCoordinate(pos.x);
        int chunkY = ChunkUtil.chunkCoordinate(pos.y);
        int chunkZ = ChunkUtil.chunkCoordinate(pos.z);

        Ref<ChunkStore> sectionRef = world.getChunkStore().getChunkSectionReference(chunkX, chunkY, chunkZ);
        if (sectionRef == null || !sectionRef.isValid()) {
            // section not loaded => treat as blocked (or empty, depending on your logic)
            return false;
        }

        BlockSection section = chunkStore.getComponent(sectionRef, BlockSection.getComponentType());
        if (section == null) {
            return true; // no block section => air
        }

        int blockId = section.get(pos.x, pos.y, pos.z);
        BlockType type = BlockType.getAssetMap().getAsset(blockId);
        return BlockTypeIsEmpty(type);
    }


    @Contract("null -> true")
    public static boolean BlockTypeIsEmpty(BlockType blockType) {
        return blockType == null
                || blockType == BlockType.EMPTY
                || blockType.getMaterial() == BlockMaterial.Empty;
    }

    public static void WithTouchingBlockPositions(Vector3i pos, Predicate<Vector3i> callback) {
        Vector3i[] facePositions = {
                new Vector3i(pos.x + 1, pos.y, pos.z),
                new Vector3i(pos.x - 1, pos.y, pos.z),
                new Vector3i(pos.x, pos.y + 1, pos.z),
                new Vector3i(pos.x, pos.y - 1, pos.z),
                new Vector3i(pos.x, pos.y, pos.z + 1),
                new Vector3i(pos.x, pos.y, pos.z - 1),
        };


        for (Vector3i facePos : facePositions) {
            if(!callback.test(facePos)) {
                break;
            }
        }
    }

    public static @Nullable Ref<ChunkStore> GetBlockEntityRefFromInteractionContext(InteractionContext ctx) {
        Ref<EntityStore> entityStoreRef = ctx.getEntity();
        World world = entityStoreRef.getStore().getExternalData().getWorld();
        BlockPosition targetBlock = ctx.getTargetBlock();
        if (targetBlock == null) return null;

        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(targetBlock.x, targetBlock.z));
        if (chunk == null) return null;

        return chunk.getBlockComponentEntity(targetBlock.x, targetBlock.y, targetBlock.z);
    }

}
