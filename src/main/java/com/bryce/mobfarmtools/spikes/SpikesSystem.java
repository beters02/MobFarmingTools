package com.bryce.mobfarmtools.spikes;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.config.MobFarmingToolsConfig;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerComponent;
import com.bryce.mobfarmtools.util.MFTBlockUtil;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.util.MFTEntityUtil;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.spatial.SpatialResource;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.server.core.asset.type.model.config.DetailBox;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.collision.CollisionConfig;
import com.hypixel.hytale.server.core.modules.collision.CollisionMath;
import com.hypixel.hytale.server.core.modules.collision.CollisionModule;
import com.hypixel.hytale.server.core.modules.collision.CollisionResult;
import com.hypixel.hytale.server.core.modules.entity.component.BoundingBox;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.Config;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import it.unimi.dsi.fastutil.objects.ObjectList;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SpikesSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, SpikesComponent> spikesComponentType;
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[SpikesSystem]");
    private final Config<MobFarmingToolsConfig> mftConfig;

    public SpikesSystem(ComponentType<ChunkStore, SpikesComponent> spikesComponentType) {
        this.spikesComponentType = spikesComponentType;
        mftConfig = MobFarmingToolsPlugin.get().getMobFarmingToolsConfig();
        debugger.setEnabled(false);
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                this.spikesComponentType,
                BlockModule.BlockStateInfo.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        SpikesComponent spikes = archetypeChunk.getComponent(index, SpikesComponent.getComponentType());
        if (spikes == null) return;

        spikes.incrementTicksLifetime(1);
        if (spikes.getTicksLifetime() < SpikesConstants.TICKS_PER_ACTION) return;
        spikes.setTicksLifetime(0);

        World world = store.getExternalData().getWorld();

        BlockModule.BlockStateInfo info = MFTBlockUtil.GetBlockStateInfoFromArchetype(archetypeChunk, index);
        if (info == null) {
            debugger.atWarning("Block state info is null.");
            return;
        }

        Vector3d posD = MFTBlockUtil.GetWorldPosFromBlockStateInfo(info);
        if (posD == null) {
            debugger.atWarning("Block position is null.");
            return;
        }
        Vector3i pos = posD.toVector3i();

        List<Ref<EntityStore>> collidingEntities = getCollidingEntities(world, pos.x, pos.y, pos.z);
        if (collidingEntities.isEmpty()) {
            debugger.atInfo("No colliding entities found.");
            return;
        }

        MobFarmingToolsConfig config = mftConfig.get();

        for (Ref<EntityStore> ref : collidingEntities) {

            // Player
            Player player = ref.getStore().getComponent(ref, Player.getComponentType());
            if (player != null) {
                if (!spikes.isDamagePlayersEnabled()) {
                    debugger.atWarning("Damaging players is not allowed from component.");
                    continue;
                }
                if (!spikes.isCustomSpikes() && !config.isSpikesDamagePlayersEnabled()) {
                    debugger.atWarning("Damaging players is not allowed from configuration.");
                    continue;
                }

                damageEntity(ref, (float) spikes.getDamagePerSecond());
                continue;
            }

            // NPC
            ComponentType<EntityStore, NPCEntity> npcEntityComponentType = NPCEntity.getComponentType();
            if (npcEntityComponentType == null) {
                debugger.atWarning("NPCEntity component type is null.");
                continue;
            }

            NPCEntity npc = ref.getStore().getComponent(ref, npcEntityComponentType);
            if (npc != null) {
                if (!spikes.isDamageNPCsEnabled()) {
                    debugger.atWarning("Damaging npcs is not allowed from component.");
                    continue;
                }

                if (!config.isSpikesDamageNpcsEnabled()) {
                    debugger.atWarning("Damaging npcs is not allowed from configuration.");
                    continue;
                }

                String entityId = npc.getNPCTypeId();
                if (config.isEntityBlacklistedSpikes(entityId)) {
                    debugger.atWarning("Damaging " + entityId + " disabled by configuration.");
                    continue;
                }

                if(!spikes.isDamageBossesEnabled() && MFTEntityUtil.IsEntityIdBoss(entityId)) {
                    debugger.atWarning("Damaging boss " + entityId + " disabled by component (from configuration)");
                    continue;
                }

                damageEntity(ref, (float) spikes.getDamagePerSecond());
            }
        }
    }


    private List<Ref<EntityStore>> getCollidingEntities(World world, int blockX, int blockY, int blockZ) {
        Store<EntityStore> entityStore = world.getEntityStore().getStore();

        // 1) Resolve block hitboxes via CollisionConfig
        CollisionConfig blockCfg = new CollisionResult().getConfig();
        blockCfg.setWorld(world);
        boolean anyBlockData = blockCfg.canCollide(blockX, blockY, blockZ); // fills cfg regardless of material
        if (!anyBlockData) {
            // no valid chunk/section loaded
            return List.of();
        }

        // Block local hitboxes + offsets -> world-space
        int bx = blockX + blockCfg.getBoundingBoxOffsetX();
        int by = blockY + blockCfg.getBoundingBoxOffsetY();
        int bz = blockZ + blockCfg.getBoundingBoxOffsetZ();

        double margin = SpikesConstants.HITBOX_MARGIN; // tweak

        // Build expanded boxes
        Box[] blockBoxes;
        int detailCount = blockCfg.getDetailCount();
        if (detailCount <= 1) {
            Box b = new Box().assign(blockCfg.getBoundingBox());
            b.expand(margin);
            blockBoxes = new Box[] { b };
        } else {
            blockBoxes = new Box[detailCount];
            for (int i = 0; i < detailCount; i++) {
                Box b = new Box().assign(blockCfg.getBoundingBox(i));
                b.expand(margin);
                blockBoxes[i] = b;
            }
        }

        // Compute bounds using expanded boxes
        Vector3d min = new Vector3d(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
        Vector3d max = new Vector3d(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY);
        for (Box b : blockBoxes) {
            min.setX(Math.min(min.x, bx + b.min.x));
            min.setY(Math.min(min.y, by + b.min.y));
            min.setZ(Math.min(min.z, bz + b.min.z));
            max.setX(Math.max(max.x, bx + b.max.x));
            max.setY(Math.max(max.y, by + b.max.y));
            max.setZ(Math.max(max.z, bz + b.max.z));
        }

        // 2) Collect candidate entities from tangible spatial resource

        SpatialResource<Ref<EntityStore>, EntityStore> spatial =
                entityStore.getResource(CollisionModule.get().getTangibleEntitySpatialResourceType());

        ObjectList<Ref<EntityStore>> candidates = SpatialResource.getThreadLocalReferenceList();
        candidates.clear();
        spatial.getSpatialStructure().collectBox(min, max, candidates);

        // 3) Overlap test
        List<Ref<EntityStore>> colliding = new ArrayList<>();
        for (Ref<EntityStore> ref : candidates) {
            if (!ref.isValid()) continue;

            TransformComponent tc = entityStore.getComponent(ref, TransformComponent.getComponentType());
            BoundingBox bb = entityStore.getComponent(ref, BoundingBox.getComponentType());
            if (tc == null || bb == null) continue;

            Vector3d ep = tc.getPosition();
            Box ebox = bb.getBoundingBox();

            boolean hit = false;
            if (bb.getDetailBoxes() != null && !bb.getDetailBoxes().isEmpty()) {
                // match EntityRefCollisionProvider behavior
                for (Map.Entry<String, DetailBox[]> e : bb.getDetailBoxes().entrySet()) {
                    for (DetailBox d : e.getValue()) {
                        Vector3d dp = new Vector3d(d.getOffset()).rotateY(tc.getRotation().getYaw()).add(ep);
                        Box dbox = d.getBox();
                        for (Box b : blockBoxes) {
                            if (CollisionMath.intersectAABBs(
                                    dp.x, dp.y, dp.z, dbox,
                                    bx, by, bz, b
                            ) != 0) {
                                hit = true;
                                break;
                            }
                        }
                        if (hit) break;
                    }
                    if (hit) break;
                }
            } else {
                for (Box b : blockBoxes) {
                    if (CollisionMath.intersectAABBs(
                            ep.x, ep.y, ep.z, ebox,
                            bx, by, bz, b
                    ) != 0) {
                        hit = true;
                        break;
                    }
                }
            }

            if (hit) colliding.add(ref);
        }

        return colliding;
    }

    private void damageEntity(Ref<EntityStore> ref, float amount) {
        DamageCause cause = DamageCause.getAssetMap().getAsset("Out_Of_World"); // use your actual asset id
        if (cause == null) {
            cause = DamageCause.getAssetMap().getAsset("Physical"); // fallback
        }
        if (cause == null) {
            debugger.atWarning("Cant execute damage; cause is null");
            return;
        }

        Damage dmg = new Damage(new Damage.EnvironmentSource("Spikes"), cause, amount);
        DamageSystems.executeDamage(ref, ref.getStore(), dmg);
    }
}