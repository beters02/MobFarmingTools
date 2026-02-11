package com.bryce.mobfarmtools.dropper;

import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.vacuumhopper.VacuumHopperHelpers;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class DropperSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, DropperComponent> dropperComponentType;
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[DropperSystem]");

    public DropperSystem(ComponentType<ChunkStore, DropperComponent> dropperComponentType) {
        this.dropperComponentType = dropperComponentType;
        //debugger.setEnabled(false);
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                this.dropperComponentType,
                BlockModule.BlockStateInfo.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        DropperComponent dropper = archetypeChunk.getComponent(index, DropperComponent.getComponentType());
        if (dropper == null) return;

        dropper.incrementTicksLifetime(1);
        if (dropper.getTicksLifetime() < 10) return;
        dropper.setTicksLifetime(0);

        World world = store.getExternalData().getWorld();

        BlockModule.BlockStateInfo info = MFTBlockUtil.GetBlockStateInfoFromArchetype(archetypeChunk, index);
        if (info == null) {
            debugger.atWarning("Block state info null.");
            return;
        }

        Vector3d pos = MFTBlockUtil.GetWorldPosFromBlockStateInfo(info);
        if (pos == null) {
            debugger.atWarning("Pos is null (chunk not loaded?)");
            return;
        }

        List<ItemContainer> containers = VacuumHopperHelpers.GetTouchingItemContainers(world, pos.toVector3i());
        if (containers.isEmpty()) {
            debugger.atWarning("No available containers.");
            return;
        }

        int containerIndex = -1;
        int itemStackToDropSlot = -1;
        AtomicReference<ItemStack> itemStackToDrop = new AtomicReference<>();
        for (ItemContainer container : containers) {
            containerIndex++;
            for (short i = 0; i < container.getCapacity(); i++) {
                ItemStack item = container.getItemStack(i);
                if (!ItemStack.isEmpty(item)) {
                    itemStackToDrop.set(item);
                    itemStackToDropSlot = i;
                    break;
                }
            }
            if (itemStackToDrop.get() != null) {
                break;
            }
        }

        if (containerIndex == -1) {
            debugger.atWarning("Container index = -1");
            return;
        }

        if (itemStackToDrop.get() == null) {
            debugger.atWarning("No available items to drop.");
            return;
        }

        int amount = itemStackToDrop.get().getQuantity();
        ItemStack droppedStack = new ItemStack(itemStackToDrop.get().getItemId(), 1);
        ItemStack newStoredStack = itemStackToDrop.get().withQuantity(amount - 1);

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        Holder<EntityStore> holder = ItemComponent.generateItemDrop(
                entityStore,               // ComponentAccessor<EntityStore>
                droppedStack,
                pos,
                new Vector3f(),
                0f, 0f, 0f          // velocity
        );

        if (holder == null) {
            debugger.atWarning("Could not create new itemstack; holder is null");
            return;
        }

        world.execute(() -> entityStore.addEntity(holder, AddReason.SPAWN));

        containers.get(containerIndex).setItemStackForSlot((short) itemStackToDropSlot, newStoredStack);

        debugger.atInfo("Item dropped");
    }
}
