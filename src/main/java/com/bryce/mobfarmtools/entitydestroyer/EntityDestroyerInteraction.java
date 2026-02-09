package com.bryce.mobfarmtools.entitydestroyer;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.bryce.mobfarmtools.util.MFTMathUtil;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.CommandBuffer;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.shape.Box;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.entity.component.TransformComponent;
import com.hypixel.hytale.server.core.modules.entity.damage.Damage;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageCause;
import com.hypixel.hytale.server.core.modules.entity.damage.DamageSystems;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import com.hypixel.hytale.server.core.util.TargetUtil;
import com.hypixel.hytale.server.npc.entities.NPCEntity;
import org.jspecify.annotations.NonNull;

import java.util.List;

public class EntityDestroyerInteraction extends SimpleInstantInteraction {
    public static final BuilderCodec<EntityDestroyerInteraction> CODEC = BuilderCodec.builder(
            EntityDestroyerInteraction.class, EntityDestroyerInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    private final MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[EntityDestroyerInteraction]");

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext context, @NonNull CooldownHandler cooldownHandler) {
        debugger.setEnabled(false);

        Ref<EntityStore> userRef = context.getEntity();
        Store<EntityStore> store = userRef.getStore();
        CommandBuffer<EntityStore> commandBuffer = context.getCommandBuffer();
        if (commandBuffer == null) return;

        TransformComponent playerTransform = store.getComponent(userRef, TransformComponent.getComponentType());
        if (playerTransform == null) return;

        Vector3d pos = playerTransform.getPosition();
        Box box = MFTMathUtil.GetBoxFromPosition(pos, 4);

        EntityStore entityStore = store.getExternalData().getWorld().getEntityStore();
        List<Ref<EntityStore>> entities = TargetUtil.getAllEntitiesInBox(box.min, box.max, entityStore.getStore());
        if (entities.isEmpty()) return;

        int causeIndex = DamageCause.getAssetMap().getIndex("Fire");
        if (causeIndex == Integer.MIN_VALUE) {
            debugger.atWarning("CANNOT DAMAGE ENTITY; MIN VALUE MET");
            return;
        }

        if (NPCEntity.getComponentType() == null) return;

        for (Ref<EntityStore> ref : entities) {
            NPCEntity npc = store.getComponent(ref, NPCEntity.getComponentType());
            if (npc == null) continue;

            Damage dmg = new Damage(new Damage.EntitySource(userRef), causeIndex, 1000f);
            DamageSystems.executeDamage(ref, commandBuffer, dmg);
        }
    }
}
