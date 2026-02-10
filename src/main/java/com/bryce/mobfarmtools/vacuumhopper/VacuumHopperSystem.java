package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.util.MFTVectorUtil;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.BlockType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.transaction.ItemStackTransaction;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.EntityModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.item.ItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PickupItemComponent;
import com.hypixel.hytale.server.core.modules.entity.item.PreventPickup;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.meta.state.ItemContainerState;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class VacuumHopperSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, VacuumHopperComponent> vacuumHopperComponentType;
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[VacuumHopper]");

    public VacuumHopperSystem(ComponentType<ChunkStore, VacuumHopperComponent> vacuumHopperComponentType) {
        this.vacuumHopperComponentType = vacuumHopperComponentType;
        debugger.setEnabled(VacuumHopperConstants.DEBUGGER_ENABLED);
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                this.vacuumHopperComponentType,
                BlockModule.BlockStateInfo.getComponentType()
        );
    }

    private boolean disableIfTrue(boolean value, World world, Vector3d pos) {
        if (value) setOnBlockState(world, pos, false);
        return value;
    }

    private <T> T disableIfNull(T value, World world, Vector3d pos) {
        if (value == null) setOnBlockState(world, pos, false);
        return value;
    }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        VacuumHopperComponent vacuum = archetypeChunk.getComponent(index, this.vacuumHopperComponentType);
        if (vacuum == null) {
            debugger.atWarning("VACUUM HOPPER COMPONENT NOT FOUND!");
            return;
        }

        if (!vacuum.isEnabled()) return;

        vacuum.incrementTicksLifetime(1);
        if (vacuum.getTicksLifetime() < VacuumHopperConstants.TICKS_PER_ACTION) {
            return;
        }

        vacuum.setTicksLifetime(0);

        BlockModule.BlockStateInfo info = MFTBlockUtil.GetBlockStateInfoFromArchetype(archetypeChunk, index);
        if (info == null) return;

        Vector3d pos = MFTBlockUtil.GetWorldPosFromBlockStateInfo(info);
        if (pos == null) return;

        World world = store.getExternalData().getWorld();
        List<ItemContainer> containers = VacuumHopperHelpers.GetTouchingItemContainers(world, pos.toVector3i());
        //if (!debugger.requireBool(!containers.isEmpty(), "No valid containers.")) return;
        if (disableIfTrue(containers.isEmpty(), world, pos)) return;

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        List<Ref<EntityStore>> items = getDroppedItemEntitiesInRadius(pos, (float) VacuumHopperConstants.ITEM_SUCK_RADIUS, entityStore);
        //if (!debugger.requireBool(!items.isEmpty(), "No valid dropped items.")) return;
        if (disableIfTrue(items.isEmpty(), world, pos)) return;

        boolean setEnabled = false;

        for (Ref<EntityStore> ref : items) {
            TransformComponent itemTransform =
                    debugger.require(entityStore.getComponent(ref, TransformComponent.getComponentType()),
                            "No Item Transform");
            if (itemTransform == null) continue;

            ItemComponent itemComponent =
                    debugger.require(entityStore.getComponent(ref, ItemComponent.getComponentType()),
                            "No ItemComponent");
            if (itemComponent == null) continue;

            ItemStack itemStack = debugger.require(itemComponent.getItemStack(), "No ItemStack");
            if (itemStack == null) continue;

            Velocity itemVelocity = debugger.require(entityStore.getComponent(ref, Velocity.getComponentType()),
                    "No Velocity");
            if (itemVelocity == null) continue;

            setEnabled = true;

            double distance = itemTransform.getPosition().distanceTo(pos);
            if (distance <= VacuumHopperConstants.ITEM_PICKUP_RADIUS) {
                boolean hasAvailableContainer = addItemStackToAnyValidContainerRecursive(containers, itemComponent, itemStack, ref);
                vacuum.setHasAvailableContainer(hasAvailableContainer);
            } else {
                moveItemStackTowardsHopper(pos, itemTransform, itemVelocity, dt);
            }
        }

        setOnBlockState(world, pos, setEnabled);
    }

    public @NonNull List<Ref<EntityStore>> getDroppedItemEntitiesInRadius(Vector3d pos, float radius, Store<EntityStore> entityStore) {
        SpatialResource<Ref<EntityStore>, EntityStore> itemSpatial =
                entityStore.getResource(EntityModule.get().getItemSpatialResourceType());

        ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        itemSpatial.getSpatialStructure().collect(pos, radius, results);

        List<Ref<EntityStore>> droppedItemEntities = new ArrayList<>();

        for (Ref<EntityStore> ref : results) {
            if (entityStore.getComponent(ref, PickupItemComponent.getComponentType()) != null) continue;
            if (entityStore.getComponent(ref, PreventPickup.getComponentType()) != null) continue;
            droppedItemEntities.add(ref);
        }

        return droppedItemEntities;
    }



    public boolean addItemStackToAnyValidContainerRecursive(List<ItemContainer> containers, ItemComponent itemComponent, ItemStack itemStack, Ref<EntityStore> ref) {
        Integer index = VacuumHopperHelpers.GetValidContainerIndex(containers, itemStack);
        if (index == null) return false;

        ItemContainer container = containers.get(index);
        ItemStackTransaction tx = container.addItemStack(itemStack);

        if (tx.succeeded()) {
            ItemStack remainder = tx.getRemainder();
            if (remainder == null || remainder.isEmpty()) {
                ref.getStore().removeEntity(ref, RemoveReason.REMOVE);
            } else {
                itemComponent.setItemStack(remainder);
                return addItemStackToAnyValidContainerRecursive(containers, itemComponent, itemStack, ref);
            }
        }

        return true;
    }

    public void moveItemStackTowardsHopper(Vector3d hopperPos, TransformComponent itemTransform, Velocity itemVelocity, float dt) {
        debugger.atInfo("Moving item towards hopper!");

        Vector3d toHopper = new Vector3d(hopperPos).subtract(itemTransform.getPosition());
        toHopper.setY(0.0);

        double len = toHopper.length();
        if (len < 1e-6) {
            return; // already centered in XZ, avoid NaNs
        }

        toHopper.scale(1.0 / len);

        Vector3d push = MFTVectorUtil.multiply(toHopper, VacuumHopperConstants.ITEM_SUCK_SPEED * dt);
        push.y = VacuumHopperConstants.ITEM_SUCK_Y;
        itemVelocity.addInstruction(push, null, ChangeVelocityType.Add);
    }

    public void setOnBlockState(World world, Vector3d pos, boolean enabled) {
        BlockType blockType = world.getBlockType(pos.toVector3i());
        if (blockType == null) return;
        world.setBlockInteractionState(pos.toVector3i(), blockType, enabled ? "On" : "Off");
    }
}
