package com.bryce.mobfarmtools.mobfan;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.util.*;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.codec.codecs.EnumCodec;
import com.hypixel.hytale.codec.validation.Validators;
import com.hypixel.hytale.component.*;
import com.hypixel.hytale.component.query.Query;
import com.hypixel.hytale.component.system.tick.EntityTickingSystem;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.ChangeVelocityType;
import com.hypixel.hytale.protocol.VelocityThresholdStyle;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
import com.hypixel.hytale.server.core.io.NetworkSerializable;
import com.hypixel.hytale.server.core.modules.block.BlockModule;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.physics.component.PhysicsValues;
import com.hypixel.hytale.server.core.modules.physics.component.Velocity;
import com.hypixel.hytale.server.core.modules.splitvelocity.VelocityConfig;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.chunk.section.BlockSection;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import com.hypixel.hytale.server.npc.movement.controllers.MotionController;
import com.hypixel.hytale.server.npc.role.Role;
import org.bouncycastle.crypto.engines.EthereumIESEngine;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.annotation.Nonnull;
import javax.swing.text.html.parser.Entity;
import java.util.List;

public class MobFanSystem extends EntityTickingSystem<ChunkStore> {
    private final ComponentType<ChunkStore, MobFanComponent> mobFanComponentType;
    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[MobFanSystem]", false);

    public MobFanSystem(ComponentType<ChunkStore, MobFanComponent> mobFanComponentType) {
        this.mobFanComponentType = mobFanComponentType;
    }

    @Override
    public @Nullable Query<ChunkStore> getQuery() {
        return Query.and(
                this.mobFanComponentType,
                BlockModule.BlockStateInfo.getComponentType()
        );
    }

    @Override
    public void tick(float dt, int index, @NonNull ArchetypeChunk<ChunkStore> archetypeChunk, @NonNull Store<ChunkStore> store, @NonNull CommandBuffer<ChunkStore> commandBuffer) {
        MobFanComponent fan = archetypeChunk.getComponent(index, this.mobFanComponentType);
        if (fan == null) return;

        BlockModule.BlockStateInfo info = MFTBlockUtil.GetBlockStateInfoFromArchetype(archetypeChunk, index);
        if (info == null) return;

        Vector3d worldPos = MFTBlockUtil.GetWorldPosFromBlockStateInfo(info);
        if (worldPos == null) return;

        World world = info.getChunkRef().getStore().getExternalData().getWorld();
        Vector3i worldPosI = worldPos.toVector3i();
        if (MFTChunkUtil.IsChunkLoaded(world, worldPosI.x, worldPosI.z) == null) return;

        int rotationIndex = world.getBlockRotationIndex(worldPosI.x, worldPosI.y, worldPosI.z);
        tryFanPushing(dt, worldPosI, rotationIndex, world, fan);
    }

    public void tryFanPushing(float dt, Vector3i pos, int rotationIndex, World world, MobFanComponent fan) {
        Store<EntityStore> store = world.getEntityStore().getStore();

        Vector3d blockCenter = pos.toVector3d().add(0.5);
        Box box = MFTMathUtil.GetBoxInFrontOf(blockCenter, fan.getFanSize(), fan.getBaseForward(), rotationIndex);
        List<Ref<EntityStore>> hits = TargetUtil.getAllEntitiesInBox(box.min, box.max, store);

        hits.forEach(ref -> {
            if (ref == null || !ref.isValid()) return;

            NPCEntity npc = MFTEntityUtil.GetNPCComponent(ref);
            TransformComponent transformComponent = store.getComponent(ref, TransformComponent.getComponentType());
            if (transformComponent == null) return;

            Velocity velocityComponent = store.getComponent(ref, Velocity.getComponentType());
            Vector3d forwardDir = MFTMathUtil.GetForwardDirection(fan.getBaseForward(), rotationIndex);

            if (velocityComponent == null) {
                pushPosition(transformComponent, forwardDir);
            } else {
                pushVelocity(dt, forwardDir, ref, npc, velocityComponent, transformComponent);
            }
        });
    }

    public void pushPosition(TransformComponent transformComponent, Vector3d forwardDir) {
        Vector3d dir = forwardDir.clone();
        dir.y = 0.0;
        dir.normalize();
        double step = MobFanConstants.FAN_SPEED_POS; // tune
        transformComponent.getPosition().add(dir.x * step, 0.0, dir.z * step);
    }

    public void pushVelocity(
            float dt,
            Vector3d forwardDir,
            Ref<EntityStore> ref,
            NPCEntity npc,
            Velocity velocityComponent,
            TransformComponent transform
    ){
        if (npc != null && npc.getRole() != null) {
            pushNpcIgnoringDamping(ref, npc, npc.getRole(), transform, velocityComponent, forwardDir, dt);
        } else {
            Vector3d push = MFTVectorUtil.multiply(forwardDir.clone(), MobFanConstants.FAN_SPEED_VEL * dt);
            velocityComponent.addInstruction(push, new VelocityConfig(), ChangeVelocityType.Add);
        }
    }

    private Vector3d getNpcPushScaledWithMass(
            Ref<EntityStore> ref,
            Vector3d forwardDir,
            float dt
    ) {
        double massScale = 1.0 / Math.sqrt(Math.max(0.1, MFTEntityUtil.GetEntityMass(ref.getStore(), ref)));
        return forwardDir.clone().scale(MobFanConstants.FAN_SPEED_VEL * dt * massScale);
    }

    private void pushNpcIgnoringDamping(
            Ref<EntityStore> ref,
            NPCEntity npc,
            @NonNull Role role,
            TransformComponent transform,
            Velocity vel,
            Vector3d forwardDir,
            float dt
    ) {
        Vector3d push = getNpcPushScaledWithMass(ref, forwardDir, dt);
        MotionController controller = role.getActiveMotionController();

        if (MFTEntityUtil.IsNpcAquatic(npc)) {
            fixDiveNpcTransformAndPush(ref, controller, transform, forwardDir, push);
        }

        Vector3d target = vel.getVelocity().clone().add(push);
        controller.forceVelocity(
                target,
                new VelocityConfig(),
                true
        );
    }

    public void fixDiveNpcTransformAndPush(
            Ref<EntityStore> ref,
            MotionController controller,
            TransformComponent transform,
            Vector3d forwardDir,
            Vector3d push
    ){
        Vector3d dir = forwardDir.clone();
        boolean mostlyVertical = Math.abs(dir.y) > 0.6;

        // Only flatten Y for horizontal-ish fans.
        if (!mostlyVertical) dir.y = 0.0;
        if (!dir.equals(Vector3d.ZERO)) dir.normalize();

        // Unstick npc
        // add more y to position if fan is pointing upwards
        double addY = (mostlyVertical || controller.onGround()) ? 0.2 : 0;
        Vector3d delta = new Vector3d(dir.x * 0.03, addY, dir.z * 0.03);
        Vector3d candidate = transform.getPosition().clone().add(delta);

        // only add unstick if it's not colliding
        World world = ref.getStore().getExternalData().getWorld();
        if (MFTEntityUtil.CanEntityMoveTo(world, ref.getStore(), ref, candidate)) {
            transform.getPosition().add(dir.x * 0.03, addY, dir.z * 0.03);
        }

        // Keep upward fans continuously lifting, not only on first contact.
        if (mostlyVertical && dir.y > 0.0) {
            push.y += 0.1;
        }
    }
}
