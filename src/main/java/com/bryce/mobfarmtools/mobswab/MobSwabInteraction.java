package com.bryce.mobfarmtools.mobswab;

import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.protocol.InteractionType;
import com.hypixel.hytale.server.core.entity.InteractionContext;
import com.hypixel.hytale.server.core.modules.interaction.interaction.CooldownHandler;
import com.hypixel.hytale.server.core.modules.interaction.interaction.config.SimpleInstantInteraction;
import org.jspecify.annotations.NonNull;

public class MobSwabInteraction extends SimpleInstantInteraction {

    public static final BuilderCodec<MobSwabInteraction> CODEC = BuilderCodec.builder(
            MobSwabInteraction.class, MobSwabInteraction::new, SimpleInstantInteraction.CODEC
    ).build();

    @Override
    protected void firstRun(@NonNull InteractionType interactionType, @NonNull InteractionContext interactionContext, @NonNull CooldownHandler cooldownHandler) {

    }

}
