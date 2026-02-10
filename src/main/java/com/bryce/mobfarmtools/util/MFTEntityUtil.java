package com.bryce.mobfarmtools.util;

import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.Nullable;

public class MFTEntityUtil {

    public static boolean WillEntityFit(World world, Vector3i pos, Vector3i entitySize) {
        // create a box size of the entity
        // check if the box is empty
        Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
        for (int dy = 0; dy <= entitySize.y+1; dy++) {
            for (int dz = -entitySize.z; dz <= entitySize.z+1; dz++) {
                for (int dx = -entitySize.x; dx <= entitySize.x+1; dx++) {
                    Vector3i checkPos = new Vector3i(pos.x + dx, pos.y + dy, pos.z + dz);
                    if (!MFTBlockUtil.PositionIsEmpty(chunkStore, checkPos)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public static @Nullable Vector3d GetNPCEntitySize(NPCEntity entity) {
        Ref<EntityStore> entityRef = entity.getReference();
        if (entityRef == null) return null;

        Store<EntityStore> entityStore = entity.getReference().getStore();

        BoundingBox boundingBox = entityStore.getComponent(entityRef, BoundingBox.getComponentType());
        if (boundingBox == null) return null;

        Box box = boundingBox.getBoundingBox();
        return new Vector3d(box.width(), box.height(), box.depth());
    }

}
