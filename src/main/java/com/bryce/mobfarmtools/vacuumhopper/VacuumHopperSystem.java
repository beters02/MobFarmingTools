package com.bryce.mobfarmtools.vacuumhopper;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.sounds.MFTSoundEmitterComponent;
import com.bryce.mobfarmtools.util.*;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3f;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
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
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
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
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[VacuumHopperSystem]");

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

    private boolean disableIfTrue(boolean value, Ref<ChunkStore> blockRef, World world, Vector3d pos) {
        if (value) setOnBlockState(blockRef, world, pos, false);
        return value;
    }

    private <T> T disableIfNull(T value, Ref<ChunkStore> blockRef, World world, Vector3d pos) {
        if (value == null) setOnBlockState(blockRef, world, pos, false);
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

        Ref<ChunkStore> blockRef = archetypeChunk.getReferenceTo(index);

        WorldChunk worldChunk = disableIfNull(MFTChunkUtil.IsChunkLoaded(world, (int) pos.x, (int) pos.z), blockRef, world, pos);
        if (worldChunk == null) return;

        int rotationIndex = world.getBlockRotationIndex((int) pos.x, (int) pos.y, (int) pos.z);

        List<ItemContainer> containers = VacuumHopperHelpers.GetTouchingItemContainers(world, pos.toVector3i());
        //if (!debugger.requireBool(!containers.isEmpty(), "No valid containers.")) return;
        if (disableIfTrue(containers.isEmpty(), blockRef, world, pos)) return;

        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        List<Ref<EntityStore>> items = getDroppedItemEntitiesInRadius(vacuum, pos, rotationIndex, entityStore);
        //if (!debugger.requireBool(!items.isEmpty(), "No valid dropped items.")) return;
        /*if (items.isEmpty()) {
            Box box = getItemsBox(vacuum, pos, world.getBlockRotationIndex((int) pos.x, (int) pos.y, (int) pos.z));
            debugger.atWarning("No items in " + box.min + " to " + box.max);
        }*/
        if (disableIfTrue(items.isEmpty(), blockRef, world, pos)) {
            checkIsChunkLoaded(vacuum, world, pos);
            return;
        }

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


        checkIsChunkLoaded(vacuum, world, pos);
        setOnBlockState(blockRef, world, pos, setEnabled);
    }

    public void checkIsChunkLoaded(VacuumHopperComponent vacuum, World world, Vector3d blockPosition) {
        if (!vacuum.isChunkLoaded()) return;
        long chunkIndex = ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z);
        WorldChunk worldChunk = world.getChunkIfLoaded(chunkIndex);
        if (worldChunk != null) {
            debugger.atInfo("Chunk is loaded for " + vacuum.getId());
        } else {
            debugger.atInfo("Chunk is not loaded for " + vacuum.getId());
        }
    }

    public void vacuumHopperTester(World world, Vector3d pos) {
        Store<EntityStore> entityStore = world.getEntityStore().getStore();
        ItemStack droppedStack = new ItemStack("Ingredient_Fibre", 1);
        Vector3d dropPos = new Vector3d(pos.x + 2, pos.y + 1, pos.z);

        Holder<EntityStore> holder = ItemComponent.generateItemDrop(
                entityStore,               // ComponentAccessor<EntityStore>
                droppedStack,
                dropPos,
                new Vector3f(),
                0f, 0f, 0f          // velocity
        );

        world.execute(() -> entityStore.addEntity(holder, AddReason.SPAWN));
    }

    public Box getItemsBox(VacuumHopperComponent hopper, Vector3d pos, int rotationIndex) {
        return MFTMathUtil.GetBoxFromPosition(pos, hopper.getLength(), hopper.getWidth(), hopper.getHeight(), new Vector3d(0, 0, -1), rotationIndex);
    }

    public @NonNull List<Ref<EntityStore>> getDroppedItemEntitiesInRadius(VacuumHopperComponent hopper, Vector3d pos, int rotationIndex, Store<EntityStore> entityStore) {
        SpatialResource<Ref<EntityStore>, EntityStore> itemSpatial =
                entityStore.getResource(EntityModule.get().getItemSpatialResourceType());

        Box box = MFTMathUtil.GetBoxFromPosition(pos, hopper.getLength(), hopper.getWidth(), hopper.getHeight(), new Vector3d(0, 0, -1), rotationIndex);
        ObjectList<Ref<EntityStore>> results = SpatialResource.getThreadLocalReferenceList();
        itemSpatial.getSpatialStructure().collectBox(box.min, box.max, results);

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

    public void setOnBlockState(Ref<ChunkStore> blockRef, World world, Vector3d pos, boolean enabled) {
        BlockType blockType = world.getBlockType(pos.toVector3i());
        if (blockType == null) return;
        world.setBlockInteractionState(pos.toVector3i(), blockType, enabled ? "On" : "Off");

        MFTSoundEmitterComponent emitter = blockRef.getStore().getComponent(blockRef, MFTSoundEmitterComponent.getComponentType());
        if (emitter != null) {
            if (enabled) {
                boolean soundPlayed = emitter.PlaySound(blockRef, "SFX_MFT_Vacuum_Hum_Steady");
            } else {
                boolean soundStopped = emitter.StopSound(blockRef, "SFX_MFT_Vacuum_Hum_Steady");
            }
        }
    }
}
