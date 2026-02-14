package com.bryce.mobfarmtools.machineupgrade;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Component;
import com.hypixel.hytale.component.ComponentType;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MachineUpgradeComponent implements Component<ChunkStore> {
    private static final int SLOT_COUNT = MachineUpgradeType.values().length;

    public static final BuilderCodec<MachineUpgradeComponent> CODEC =
            BuilderCodec.builder(
                    MachineUpgradeComponent.class,
                    MachineUpgradeComponent::new
            )
            .append(new KeyedCodec<>("UpgradeCounts", Codec.INT_ARRAY),
                    MachineUpgradeComponent::setUpgradeCounts,
                    MachineUpgradeComponent::getUpgradeCounts)
            .add()
            .build();

    private int[] upgradeCounts = new int[SLOT_COUNT];

    @NonNull
    public static ComponentType<ChunkStore, MachineUpgradeComponent> getComponentType() {
        return MobFarmingToolsPlugin.get().getMachineUpgradeComponentType();
    }

    public int getCount(MachineUpgradeType type) {
        return upgradeCounts[type.getIndex()];
    }

    public void setCount(MachineUpgradeType type, int count) {
        upgradeCounts[type.getIndex()] = Math.max(0, count);
    }

    public int[] getUpgradeCounts() {
        return Arrays.copyOf(upgradeCounts, upgradeCounts.length);
    }

    public void setUpgradeCounts(int[] value) {
        this.upgradeCounts = new int[SLOT_COUNT];
        if (value == null) {
            return;
        }

        int len = Math.min(value.length, SLOT_COUNT);
        for (int i = 0; i < len; i++) {
            upgradeCounts[i] = Math.max(0, value[i]);
        }
    }

    public List<ItemStack> getUpgradeDrops() {
        List<ItemStack> drops = new ArrayList<>();
        for (MachineUpgradeType type : MachineUpgradeType.values()) {
            int count = getCount(type);
            if (count <= 0) {
                continue;
            }
            drops.add(new ItemStack(type.getItemId(), count));
        }
        return drops;
    }

    @Override
    public @Nullable Component<ChunkStore> clone() {
        MachineUpgradeComponent copy = new MachineUpgradeComponent();
        copy.upgradeCounts = Arrays.copyOf(this.upgradeCounts, this.upgradeCounts.length);
        return copy;
    }
}

