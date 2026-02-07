package com.bryce.mobfarmtools.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockMaterial;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class MFTSpawnerUtil {

    public static @Nullable Vector3d FindEntitySpawnLocation(World world, Vector3d blockWorldPos, Vector3d entitySize) {
        int baseX = (int) blockWorldPos.x;
        int baseY = (int) blockWorldPos.y + 1; // spawn one above spawner
        int baseZ = (int) blockWorldPos.z;

        List<Vector3d> availableLocs = new ArrayList<>();

        int radius = 3; // 7x7 around spawner
        for (int dy = 0; dy <= entitySize.y+1; dy++) { // try same height and +entitySize.y
            for (int dz = -radius; dz <= radius+1; dz++) {
                for (int dx = -radius; dx <= radius+1; dx++) {
                    Vector3i vec = new Vector3i(baseX + dx, baseY + dy, baseZ + dz);

                    if (CanEntityFit(world, vec, entitySize.toVector3i())) {
                        availableLocs.add(vec.toVector3d());
                    }
                }
            }
        }

        if (availableLocs.isEmpty()) {
            return null;
        }

        int locIndex = ThreadLocalRandom.current().nextInt(0, availableLocs.size()-1);
        return availableLocs.get(locIndex);
    }

    public static boolean CanEntityFit(World world, Vector3i pos, Vector3i entitySize) {
        // create a box size of the entity
        // check if the box is empty
        for (int dy = 0; dy <= entitySize.y+1; dy++) {
            for (int dz = -entitySize.z; dz <= entitySize.z+1; dz++) {
                for (int dx = -entitySize.x; dx <= entitySize.x+1; dx++) {
                    int x = pos.x + dx;
                    int y = pos.y + dy;
                    int z = pos.z + dz;

                    BlockType bt = world.getBlockType(x, y, z);
                    if (!IsBlockTypeEmpty(bt)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static boolean IsBlockTypeEmpty(@Nullable BlockType blockType) {
        return blockType == null
                || blockType == BlockType.EMPTY
                || blockType.getMaterial() == BlockMaterial.Empty;
    }

    public static  @Nullable Vector3d GetNPCEntitySize(NPCEntity entity) {
        Ref<EntityStore> entityRef = entity.getReference();
        if (entityRef == null) return null;

        Store<EntityStore> entityStore = entity.getReference().getStore();

        BoundingBox boundingBox = entityStore.getComponent(entityRef, BoundingBox.getComponentType());
        if (boundingBox == null) return null;

        Box box = boundingBox.getBoundingBox();
        return new Vector3d(box.width(), box.height(), box.depth());
    }

}
