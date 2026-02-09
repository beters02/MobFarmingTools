package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class VacuumHopperHelpers {

    public static boolean CanAddItemStackRemainderOk(ItemContainer container, ItemStack itemStack) {
        ItemContainer test = container.clone();
        ItemStackTransaction tx = test.addItemStack(itemStack, false, false, false);
        ItemStack remainder = tx.getRemainder();
        boolean fitsSome = !ItemStack.isEmpty(remainder) && remainder.getQuantity() < itemStack.getQuantity();
        boolean fitsAll = ItemStack.isEmpty(remainder);
        return fitsAll || fitsSome;
    }

    public static @Nullable Integer GetValidContainerIndex(List<ItemContainer> containers, ItemStack addItemStack) {
        int i = -1;
        for (ItemContainer container : containers) {
            i++;
            if (CanAddItemStackRemainderOk(container, addItemStack)) return i;
        }
        return null;
    }

    public static @NonNull List<ItemContainer> GetTouchingItemContainers(World world, Vector3i blockPos) {
        List<ItemContainer> containers = new ArrayList<>();
        MFTBlockUtil.WithTouchingBlockPositions(blockPos, checkPos -> {
            Ref<ChunkStore> blockEntityRef = BlockModule.getBlockEntity(world, checkPos.x, checkPos.y, checkPos.z);
            if (blockEntityRef == null || !blockEntityRef.isValid()) return true;

            Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
            ComponentType<ChunkStore, ?> containerType =
                    chunkStore.getRegistry().getData().getComponentType("container");
            if (containerType == null) return true;

            ItemContainerState containerState =
                    (ItemContainerState) chunkStore.getComponent(blockEntityRef, containerType);
            if (containerState == null) return true;

            containers.add(containerState.getItemContainer());
            return true;
        });

        return containers;
    }

    public static boolean HasAvailableItemContainer(World world, Vector3i blockPos) {
        AtomicBoolean hasAvailable = new AtomicBoolean(false);
        MFTBlockUtil.WithTouchingBlockPositions(blockPos, checkPos -> {
            Ref<ChunkStore> blockEntityRef = BlockModule.getBlockEntity(world, checkPos.x, checkPos.y, checkPos.z);
            if (blockEntityRef == null || !blockEntityRef.isValid()) return true;

            Store<ChunkStore> chunkStore = world.getChunkStore().getStore();
            ComponentType<ChunkStore, ?> containerType =
                    chunkStore.getRegistry().getData().getComponentType("container");
            if (containerType == null) return true;

            ItemContainerState containerState =
                    (ItemContainerState) chunkStore.getComponent(blockEntityRef, containerType);
            if (containerState == null) return true;

            hasAvailable.set(true);
            return false;
        });

        return hasAvailable.get();
    }

}
