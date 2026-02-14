package com.bryce.mobfarmtools.machineupgrade.ui;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeComponent;
import com.bryce.mobfarmtools.machineupgrade.MachineUpgradeType;
import com.bryce.mobfarmtools.mobspawner.MobSpawnerConstants;
import com.bryce.mobfarmtools.util.MFTDebugUtil;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.entity.entities.Player;
import com.hypixel.hytale.server.core.entity.entities.player.pages.InteractiveCustomUIPage;
import com.hypixel.hytale.server.core.inventory.Inventory;
import com.hypixel.hytale.server.core.inventory.ItemStack;
import com.hypixel.hytale.server.core.inventory.container.ItemContainer;
import com.hypixel.hytale.server.core.inventory.container.SimpleItemContainer;
import com.hypixel.hytale.server.core.ui.builder.EventData;
import com.hypixel.hytale.server.core.ui.builder.UICommandBuilder;
import com.hypixel.hytale.server.core.ui.builder.UIEventBuilder;
import com.hypixel.hytale.server.core.universe.PlayerRef;
import com.hypixel.hytale.server.core.universe.world.World;
import com.hypixel.hytale.server.core.universe.world.chunk.WorldChunk;
import com.hypixel.hytale.server.core.universe.world.storage.ChunkStore;
import com.hypixel.hytale.server.core.universe.world.storage.EntityStore;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class MachineUpgradePage extends InteractiveCustomUIPage<MachineUpgradePage.MachineUpgradeEventData> {
    private static final String PAGE_UI = "Pages/MachineUpgradePage.ui";
    private static final String SLOT_UI = "Pages/MobFanSlot.ui";
    private static final String UPGRADE_SLOT_UI = "Pages/MachineUpgradeSlot.ui";
    private static final Map<UUID, Boolean> PREVIEW_ENABLED_BY_PLAYER = new ConcurrentHashMap<>();
    public static final int MAX_STATS = 10;
    private static final Map<Ref<ChunkStore>, Set<MachineUpgradePage>> OPEN_BY_MACHINE = new ConcurrentHashMap<>();
    private static MFTDebugUtil.Debugger debugger = new MFTDebugUtil.Debugger("[MachineUpgradePage]");

    private final Ref<ChunkStore> machineRef;
    private final MachineUpgradePageConfig config;
    private final Map<Integer, StatisticLine> runtimeStats = new ConcurrentHashMap<>();
    private boolean previewEnabled;

    public MachineUpgradePage(PlayerRef playerRef, Ref<ChunkStore> machineRef, MachineUpgradePageConfig config) {
        super(playerRef, CustomPageLifetime.CanDismiss, MachineUpgradeEventData.CODEC);
        this.machineRef = machineRef;
        this.config = config;
        debugger.setEnabled(false);
    }

    @Override
    public void build(@NonNull Ref<EntityStore> ref, UICommandBuilder commands, @NonNull UIEventBuilder events, Store<EntityStore> store) {
        commands.append(PAGE_UI);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        previewEnabled = config.previewHandler != null && PREVIEW_ENABLED_BY_PLAYER.getOrDefault(playerRef.getUuid(), false);
        commands.set("#UpgradeTitle.Text", config.title);
        commands.set("#PreviewButton.Visible", config.previewHandler != null);
        registerOpenPage();
        initRuntimeStats();

        buildUpgradeSlots(commands, events, true);
        buildStatistics(commands, true);
        buildPlayerInventory(commands, events, player.getInventory(), true);
        updatePreviewButton(commands);

        if (config.beforeOpenHandler != null) {
            config.beforeOpenHandler.onRightBeforePageOpen(new BeforeOpenContext(this.machineRef, this));
        }

        if (config.previewHandler != null) {
            events.addEventBinding(CustomUIEventBindingType.Activating, "#PreviewButton", EventData.of("Action", "preview_toggle"), false);
            if (previewEnabled) {
                config.previewHandler.onPreviewStateChanged(new UpgradeEventContext(playerRef, player, machineRef, config.blockPosition, config.rotationIndex), true);
            }
        }
    }

    @Override
    public void handleDataEvent(@NonNull Ref<EntityStore> ref, @NonNull Store<EntityStore> store, @NonNull MachineUpgradeEventData data) {
        if (data.action == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        MachineUpgradeComponent upgrades = getOrCreateMachineUpgradeComponent();
        if (upgrades == null) {
            return;
        }

        if (data.action.equals("preview_toggle") && config.previewHandler != null) {
            previewEnabled = !previewEnabled;
            PREVIEW_ENABLED_BY_PLAYER.put(playerRef.getUuid(), previewEnabled);
            config.previewHandler.onPreviewStateChanged(
                    new UpgradeEventContext(playerRef, player, machineRef, config.blockPosition, config.rotationIndex),
                    previewEnabled
            );
            UICommandBuilder update = new UICommandBuilder();
            updatePreviewButton(update);
            sendUpdate(update, false);
            return;
        }

        if (data.action.startsWith("inv_left:") || data.action.startsWith("inv_right:")) {
            handleInventoryClick(player, upgrades, data.action);
        } else if (data.action.startsWith("hotbar_left:") || data.action.startsWith("hotbar_right:")) {
            handleHotbarClick(player, upgrades, data.action);
        } else if (data.action.startsWith("upgrade_left:") || data.action.startsWith("upgrade_right:")) {
            handleUpgradeClick(player, upgrades, data.action);
        }

        UICommandBuilder update = new UICommandBuilder();
        buildUpgradeSlots(update, null, false);
        buildPlayerInventory(update, null, player.getInventory(), false);
        if (previewEnabled && config.previewHandler != null) {
            config.previewHandler.onPreviewStateChanged(
                    new UpgradeEventContext(playerRef, player, machineRef, config.blockPosition, config.rotationIndex),
                    true
            );
        }
        sendUpdate(update, false);
    }

    @Override
    public void onDismiss(@NonNull Ref<EntityStore> ref, @NonNull Store<EntityStore> store) {
        unregisterOpenPage();
    }

    private void handleInventoryClick(Player player, MachineUpgradeComponent upgrades, String action) {
        boolean isRight = action.startsWith("inv_right:");
        int slot = parseSlot(action);
        if (slot < 0) {
            return;
        }

        moveFromInventorySlot(player, upgrades, player.getInventory().getStorage(), slot, isRight);
    }

    private void handleHotbarClick(Player player, MachineUpgradeComponent upgrades, String action) {
        boolean isRight = action.startsWith("hotbar_right:");
        int slot = parseSlot(action);
        if (slot < 0) {
            return;
        }

        moveFromInventorySlot(player, upgrades, player.getInventory().getHotbar(), slot, isRight);
    }

    private void moveFromInventorySlot(Player player, MachineUpgradeComponent upgrades, ItemContainer container, int slot, boolean single) {
        ItemStack item = container.getItemStack((short) slot);
        if (ItemStack.isEmpty(item)) {
            return;
        }

        MachineUpgradeType type = MachineUpgradeType.fromItemId(item.getItemId());
        if (type == null || !config.isEnabled(type)) {
            return;
        }

        int current = upgrades.getCount(type);
        int available = config.getLimit(type) - current;
        if (available <= 0) {
            return;
        }

        int toMove = single ? 1 : Math.min(available, item.getQuantity());
        if (toMove <= 0) {
            return;
        }

        ItemStack toRemove = new ItemStack(item.getItemId(), toMove);
        container.removeItemStackFromSlot((short) slot, toRemove, toMove, false, true);

        int newCount = current + toMove;
        upgrades.setCount(type, newCount);
        persistUpgradeComponent(upgrades);
        fireUpgradeChanged(player, type, current, newCount);
        markChunkDirty();
    }

    private void handleUpgradeClick(Player player, MachineUpgradeComponent upgrades, String action) {
        boolean removeAll = action.startsWith("upgrade_left:");
        int slotIndex = parseSlot(action);
        MachineUpgradeType type = MachineUpgradeType.fromIndex(slotIndex);
        if (type == null || !config.isEnabled(type)) {
            return;
        }

        int current = upgrades.getCount(type);
        if (current <= 0) {
            return;
        }

        int toRemove = removeAll ? current : 1;
        int newCount = current - toRemove;
        upgrades.setCount(type, newCount);
        persistUpgradeComponent(upgrades);
        fireUpgradeChanged(player, type, current, newCount);

        Ref<EntityStore> ref = playerRef.getReference();
        if (ref != null) {
            SimpleItemContainer.addOrDropItemStack(
                    ref.getStore(),
                    ref,
                    player.getInventory().getCombinedHotbarFirst(),
                    new ItemStack(type.getItemId(), toRemove)
            );
        }
        markChunkDirty();
    }

    private void fireUpgradeChanged(Player player, MachineUpgradeType type, int oldCount, int newCount) {
        if (config.changeHandler == null || oldCount == newCount) {
            return;
        }
        config.changeHandler.onUpgradeChanged(
                new UpgradeEventContext(playerRef, player, machineRef, config.blockPosition, config.rotationIndex),
                type,
                oldCount,
                newCount
        );
    }

    private void buildUpgradeSlots(UICommandBuilder commands, @Nullable UIEventBuilder events, boolean fullRebuild) {
        MachineUpgradeComponent upgrades = getOrCreateMachineUpgradeComponent();
        if (upgrades == null) {
            return;
        }

        if (fullRebuild) {
            commands.clear("#UpgradeSlotsContainer");
        }

        MachineUpgradeType[] types = MachineUpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            MachineUpgradeType type = types[i];
            String selector = "#UpgradeSlotsContainer[" + i + "]";
            if (fullRebuild) {
                commands.append("#UpgradeSlotsContainer", UPGRADE_SLOT_UI);
            }

            int count = upgrades.getCount(type);
            boolean enabled = config.isEnabled(type);
            int limit = config.getLimit(type);
            commands.set(selector + " #UpgradeLabel.Text", type.getDisplayName());
            commands.set(selector + " #DisabledOverlay.Visible", !enabled);
            if (count > 0) {
                commands.set(selector + " #SlotItem.ItemId", type.getItemId());
                commands.set(selector + " #QuantityLabel.Text", String.valueOf(count));
                commands.set(selector + " #QuantityLabel.Visible", count > 1);
            } else {
                commands.set(selector + " #SlotItem.ItemId", "");
                commands.set(selector + " #QuantityLabel.Visible", false);
            }

            String status = enabled ? (count + "/" + limit) : "Disabled";
            commands.set(selector + ".TooltipTextSpans", Message.raw(type.getDisplayName() + " (" + status + ")"));
            if (events != null && enabled) {
                events.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", "upgrade_left:" + i), false);
                events.addEventBinding(CustomUIEventBindingType.RightClicking, selector, EventData.of("Action", "upgrade_right:" + i), false);
            }
        }
    }

    private void buildPlayerInventory(UICommandBuilder commands, @Nullable UIEventBuilder events, Inventory inventory, boolean fullRebuild) {
        ItemContainer storage = inventory.getStorage();
        ItemContainer hotbar = inventory.getHotbar();
        if (fullRebuild) {
            commands.clear("#PlayerInventory");
        }

        int slotIndex = 0;
        for (short i = 0; i < storage.getCapacity(); i++) {
            String selector = "#PlayerInventory[" + slotIndex + "]";
            if (fullRebuild) {
                commands.append("#PlayerInventory", SLOT_UI);
            }
            applyInventorySlot(commands, selector, storage.getItemStack(i));
            if (events != null) {
                events.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", "inv_left:" + i), false);
                events.addEventBinding(CustomUIEventBindingType.RightClicking, selector, EventData.of("Action", "inv_right:" + i), false);
            }
            slotIndex++;
        }

        for (short i = 0; i < hotbar.getCapacity(); i++) {
            String selector = "#PlayerInventory[" + slotIndex + "]";
            if (fullRebuild) {
                commands.append("#PlayerInventory", SLOT_UI);
            }
            applyInventorySlot(commands, selector, hotbar.getItemStack(i));
            commands.set(selector + " #HotbarNumberBg #HotbarNumber.Text", String.valueOf(i + 1));
            commands.set(selector + " #HotbarNumberBg.Visible", true);
            if (events != null) {
                events.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", "hotbar_left:" + i), false);
                events.addEventBinding(CustomUIEventBindingType.RightClicking, selector, EventData.of("Action", "hotbar_right:" + i), false);
            }
            slotIndex++;
        }
    }

    private void buildStatistics(UICommandBuilder commands, boolean fullRebuild) {
        boolean hasAny = !runtimeStats.isEmpty();
        commands.set("#StatsContainer.Visible", hasAny);

        for (int i = 0; i < MAX_STATS; i++) {
            String selector = getStatLineSelector(i);
            StatisticLine line = runtimeStats.get(i);
            boolean visible = line != null && (line.getDescription() != null || line.getValue() != null);
            if (fullRebuild) {
                commands.set(selector + ".Visible", visible);
            } else if (!visible) {
                commands.set(selector + ".Visible", false);
                continue;
            }

            if (line != null) {
                commands.set(selector + " #StatDesc.Text", line.getDescription() == null ? "" : line.getDescription());
                commands.set(selector + " #StatValue.Text", line.getValue() == null ? "" : line.getValue());
                commands.set(selector + ".Visible", true);
            }
        }
    }

    private void applyInventorySlot(UICommandBuilder commands, String selector, ItemStack item) {
        if (!ItemStack.isEmpty(item)) {
            commands.set(selector + " #SlotItem.ItemId", item.getItemId());
            commands.set(selector + " #QuantityLabel.Text", String.valueOf(item.getQuantity()));
            commands.set(selector + " #QuantityLabel.Visible", item.getQuantity() > 1);
            commands.set(selector + ".TooltipTextSpans", Message.raw(item.getItemId()));
        } else {
            commands.set(selector + " #SlotItem.ItemId", "");
            commands.set(selector + " #QuantityLabel.Visible", false);
            commands.set(selector + ".TooltipTextSpans", Message.empty());
        }
    }

    private void updatePreviewButton(UICommandBuilder commands) {
        if (config.previewHandler == null) {
            return;
        }
        commands.set("#PreviewButtonLabel.Text", previewEnabled ? "Preview: On" : "Preview: Off");
    }

    @Nullable
    private MachineUpgradeComponent getOrCreateMachineUpgradeComponent() {
        if (machineRef == null || !machineRef.isValid()) {
            return null;
        }

        Store<ChunkStore> store = machineRef.getStore();
        MachineUpgradeComponent upgrades = store.getComponent(machineRef, MachineUpgradeComponent.getComponentType());
        if (upgrades == null) {
            upgrades = new MachineUpgradeComponent();
            store.putComponent(machineRef, MachineUpgradeComponent.getComponentType(), upgrades);
        }
        return upgrades;
    }

    private void persistUpgradeComponent(MachineUpgradeComponent upgrades) {
        if (machineRef == null || !machineRef.isValid()) {
            return;
        }

        Store<ChunkStore> store = machineRef.getStore();
        store.putComponent(machineRef, MachineUpgradeComponent.getComponentType(), upgrades);
    }

    private void markChunkDirty() {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        BlockPosition pos = config.blockPosition;
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(pos.x, pos.z));
        if (chunk != null) {
            chunk.markNeedsSaving();
        }
    }

    private int parseSlot(String action) {
        int idx = action.indexOf(':');
        if (idx == -1 || idx + 1 >= action.length()) {
            return -1;
        }
        try {
            return Integer.parseInt(action.substring(idx + 1));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private void initRuntimeStats() {
        runtimeStats.clear();
        for (Map.Entry<Integer, StatisticLine> entry : config.statistics.entrySet()) {
            runtimeStats.put(entry.getKey(), new StatisticLine(entry.getValue().getDescription(), entry.getValue().getValue()));
        }
    }

    private void registerOpenPage() {
        OPEN_BY_MACHINE
                .computeIfAbsent(machineRef, ignored -> Collections.newSetFromMap(new ConcurrentHashMap<>()))
                .add(this);
    }

    private void unregisterOpenPage() {
        Set<MachineUpgradePage> pages = OPEN_BY_MACHINE.get(machineRef);
        if (pages == null) {
            return;
        }
        pages.remove(this);
        if (pages.isEmpty()) {
            OPEN_BY_MACHINE.remove(machineRef);
        }
    }

    private static String getStatLineSelector(int index) {
        return "#Stats #StatLine" + index;
    }

    public static void clearAllPreviews(World world) {
        if (world == null) {
            return;
        }
        PREVIEW_ENABLED_BY_PLAYER.clear();
        ClearDebugShapes packet = new ClearDebugShapes();
        for (PlayerRef ref : world.getPlayerRefs()) {
            ref.getPacketHandler().write(packet);
        }
    }

    public void updateStatisticValue(int index, String value) {
        boolean isEntityId = index == (MobSpawnerConstants.UpgradePageStat.ENTITY_ID.getIndex());

        if (index < 0 || index >= MAX_STATS) {
            if (isEntityId) {
                debugger.atWarning("INDEX < 0 OR INDEX >= MAX_STATS");
            }
            return;
        }
        StatisticLine line = runtimeStats.get(index);
        if (line == null) {
            if (isEntityId) {
                debugger.atWarning("LINE == NULL");
            }
            return;
        }
        line.setValue(value);

        UICommandBuilder update = new UICommandBuilder();
        update.set(getStatLineSelector(index) + " #StatValue.Text", value);
        update.set(getStatLineSelector(index) + ".Visible", true);
        sendUpdate(update, false);

        if (isEntityId) {
            debugger.atWarning("UPDATED TO " + value);
        }
    }

    public void updateStatisticDescription(int index, String description) {
        if (index < 0 || index >= MAX_STATS) {
            return;
        }
        StatisticLine line = runtimeStats.get(index);
        if (line == null) {
            return;
        }
        line.setDescription(description);

        UICommandBuilder update = new UICommandBuilder();
        update.set(getStatLineSelector(index) + " #StatDesc.Text", description);
        update.set(getStatLineSelector(index) + ".Visible", true);
        sendUpdate(update, false);
    }

    public static void pushStatisticValue(Ref<ChunkStore> machineRef, int index, String value) {
        Set<MachineUpgradePage> pages = OPEN_BY_MACHINE.get(machineRef);
        if (pages == null) return;
        for (MachineUpgradePage page : pages) {
            page.updateStatisticValue(index, value);
        }
    }

    public static void pushStatisticDescription(Ref<ChunkStore> machineRef, int index, String description) {
        Set<MachineUpgradePage> pages = OPEN_BY_MACHINE.get(machineRef);
        if (pages == null) return;
        for (MachineUpgradePage page : pages) {
            page.updateStatisticDescription(index, description);
        }
    }

    @FunctionalInterface
    public interface UpgradeChangeHandler {
        void onUpgradeChanged(UpgradeEventContext context, MachineUpgradeType type, int oldCount, int newCount);
    }

    @FunctionalInterface
    public interface PreviewHandler {
        void onPreviewStateChanged(UpgradeEventContext context, boolean enabled);
    }

    @FunctionalInterface
    public interface BeforePageOpenHandler {
        void onRightBeforePageOpen(BeforeOpenContext context);
    }

    public static final class MachineUpgradePageConfig {
        private final String title;
        private final BlockPosition blockPosition;
        private final int rotationIndex;
        private final EnumSet<MachineUpgradeType> enabledUpgrades;
        private final EnumMap<MachineUpgradeType, Integer> upgradeLimits;
        private final @Nullable UpgradeChangeHandler changeHandler;
        private final @Nullable PreviewHandler previewHandler;
        private final Map<Integer, StatisticLine> statistics;
        private final @Nullable BeforePageOpenHandler beforeOpenHandler;

        private MachineUpgradePageConfig(
                String title,
                BlockPosition blockPosition,
                int rotationIndex,
                EnumSet<MachineUpgradeType> enabledUpgrades,
                EnumMap<MachineUpgradeType, Integer> upgradeLimits,
                @Nullable UpgradeChangeHandler changeHandler,
                @Nullable PreviewHandler previewHandler,
                @Nullable BeforePageOpenHandler beforeOpenHandler,
                Map<Integer, StatisticLine> statistics
        ) {
            this.title = title;
            this.blockPosition = blockPosition;
            this.rotationIndex = rotationIndex;
            this.enabledUpgrades = enabledUpgrades;
            this.upgradeLimits = upgradeLimits;
            this.changeHandler = changeHandler;
            this.previewHandler = previewHandler;
            this.statistics = statistics;
            this.beforeOpenHandler = beforeOpenHandler;
        }

        public static Builder builder(String title, BlockPosition blockPosition, int rotationIndex) {
            return new Builder(title, blockPosition, rotationIndex);
        }

        private boolean isEnabled(MachineUpgradeType type) {
            return enabledUpgrades.contains(type);
        }

        private int getLimit(MachineUpgradeType type) {
            return Math.max(0, upgradeLimits.getOrDefault(type, 0));
        }

        public static final class Builder {
            private final String title;
            private final BlockPosition blockPosition;
            private final int rotationIndex;
            private EnumSet<MachineUpgradeType> enabledUpgrades = EnumSet.noneOf(MachineUpgradeType.class);
            private final EnumMap<MachineUpgradeType, Integer> upgradeLimits = new EnumMap<>(MachineUpgradeType.class);
            private @Nullable UpgradeChangeHandler changeHandler;
            private @Nullable PreviewHandler previewHandler;
            private @Nullable BeforePageOpenHandler beforeOpenHandler;
            private final Map<Integer, StatisticLine> statistics;

            private Builder(String title, BlockPosition blockPosition, int rotationIndex) {
                this.title = title;
                this.blockPosition = blockPosition;
                this.rotationIndex = rotationIndex;
                this.statistics = new HashMap<>();
            }

            public Builder enableUpgrade(MachineUpgradeType type, int maxCount) {
                enabledUpgrades.add(type);
                upgradeLimits.put(type, Math.max(0, maxCount));
                return this;
            }

            public Builder enableUpgrades(Map<MachineUpgradeType, Integer> limits) {
                if (limits == null) {
                    return this;
                }
                for (Map.Entry<MachineUpgradeType, Integer> entry : limits.entrySet()) {
                    enableUpgrade(entry.getKey(), entry.getValue());
                }
                return this;
            }

            public Builder onUpgradeChanged(@Nullable UpgradeChangeHandler handler) {
                this.changeHandler = handler;
                return this;
            }

            public Builder onBeforePageOpen(@Nullable BeforePageOpenHandler handler) {
                this.beforeOpenHandler = handler;
                return this;
            }

            public Builder withPreview(@Nullable PreviewHandler handler) {
                this.previewHandler = handler;
                return this;
            }

            public Builder addStatistic(int index, String description) {
                return addStatistic(index, description, "");
            }

            public Builder addStatistic(int index, String description, String initialValue) {
                if (index < 0 || index >= MAX_STATS) {
                    return this;
                }
                statistics.put(index, new StatisticLine(description, initialValue));
                return this;
            }

            public Builder addStatistic(UpgradePageStatDef stat) {
                if (stat == null) return this;
                return addStatistic(stat.getIndex(), stat.getDescription(), stat.getDefaultValue());
            }

            public MachineUpgradePageConfig build() {
                EnumSet<MachineUpgradeType> enabledCopy = enabledUpgrades.isEmpty()
                        ? EnumSet.noneOf(MachineUpgradeType.class)
                        : EnumSet.copyOf(enabledUpgrades);
                Map<Integer, StatisticLine> statsCopy = new HashMap<>();
                for (Map.Entry<Integer, StatisticLine> entry : statistics.entrySet()) {
                    StatisticLine line = entry.getValue();
                    statsCopy.put(entry.getKey(), new StatisticLine(line.getDescription(), line.getValue()));
                }
                return new MachineUpgradePageConfig(
                        title,
                        blockPosition,
                        rotationIndex,
                        enabledCopy,
                        new EnumMap<>(upgradeLimits),
                        changeHandler,
                        previewHandler,
                        beforeOpenHandler,
                        statsCopy
                );
            }
        }
    }

    public static final class UpgradeEventContext {
        private final PlayerRef playerRef;
        private final Player player;
        private final Ref<ChunkStore> machineRef;
        private final BlockPosition blockPosition;
        private final int rotationIndex;

        private UpgradeEventContext(PlayerRef playerRef, Player player, Ref<ChunkStore> machineRef, BlockPosition blockPosition, int rotationIndex) {
            this.playerRef = playerRef;
            this.player = player;
            this.machineRef = machineRef;
            this.blockPosition = blockPosition;
            this.rotationIndex = rotationIndex;
        }

        public PlayerRef getPlayerRef() {
            return playerRef;
        }

        public Player getPlayer() {
            return player;
        }

        public Ref<ChunkStore> getMachineRef() {
            return machineRef;
        }

        public BlockPosition getBlockPosition() {
            return blockPosition;
        }

        public int getRotationIndex() {
            return rotationIndex;
        }
    }

    public static final class BeforeOpenContext {
        private final Ref<ChunkStore> machineRef;
        private final MachineUpgradePage page;

        private BeforeOpenContext(Ref<ChunkStore> machineRef, MachineUpgradePage page) {
            this.machineRef = machineRef;
            this.page = page;
        }

        public Ref<ChunkStore> getMachineRef() { return machineRef; }
        public MachineUpgradePage getPage() { return page; }
    }

    public static final class MachineUpgradeEventData {
        public static final BuilderCodec<MachineUpgradeEventData> CODEC = BuilderCodec.builder(
                MachineUpgradeEventData.class,
                MachineUpgradeEventData::new
        )
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action)
                .add()
                .build();

        private String action;

        private MachineUpgradeEventData() {
        }
    }

    public static final class StatisticLine {
        private String description;
        private String value;

        public StatisticLine(String description, String value) {
            this.description = description;
            this.value = value;
        }

        public String getDescription() { return description; }
        public String getValue() { return value; }
        public void setDescription(String description) { this.description = description; }
        public void setValue(String value) { this.value = value; }
    }
}
