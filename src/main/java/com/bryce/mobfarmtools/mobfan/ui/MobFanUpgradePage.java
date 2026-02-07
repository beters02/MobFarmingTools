package com.bryce.mobfarmtools.mobfan.ui;

import com.bryce.mobfarmtools.MobFarmingToolsPlugin;
import com.bryce.mobfarmtools.mobfan.MobFanComponent;
import com.bryce.mobfarmtools.mobfan.MobFanConstants;
import com.hypixel.hytale.codec.Codec;
import com.hypixel.hytale.codec.KeyedCodec;
import com.hypixel.hytale.codec.builder.BuilderCodec;
import com.hypixel.hytale.component.Ref;
import com.hypixel.hytale.component.Store;
import com.hypixel.hytale.math.matrix.Matrix4d;
import com.hypixel.hytale.math.util.ChunkUtil;
import com.hypixel.hytale.math.vector.Vector3d;
import com.hypixel.hytale.math.vector.Vector3i;
import com.hypixel.hytale.protocol.BlockPosition;
import com.hypixel.hytale.protocol.DebugShape;
import com.hypixel.hytale.protocol.Vector3f;
import com.hypixel.hytale.protocol.packets.interface_.CustomPageLifetime;
import com.hypixel.hytale.protocol.packets.interface_.CustomUIEventBindingType;
import com.hypixel.hytale.protocol.packets.player.ClearDebugShapes;
import com.hypixel.hytale.protocol.packets.player.DisplayDebug;
import com.hypixel.hytale.server.core.Message;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.Rotation;
import com.hypixel.hytale.server.core.asset.type.blocktype.config.RotationTuple;
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
import org.jspecify.annotations.Nullable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class MobFanUpgradePage extends InteractiveCustomUIPage<MobFanUpgradePage.MobFanEventData> {
    private static final String PAGE_UI = "Pages/MobFanUpgradePage.ui";
    private static final String SLOT_UI = "Pages/MobFanSlot.ui";
    private static final String UPGRADE_SLOT_UI = "Pages/MobFanUpgradeSlot.ui";
    private static final Vector3f PREVIEW_COLOR = new Vector3f(0.1f, 1.0f, 0.9f);
    private static final float PREVIEW_DURATION_SECONDS = 86400.0f;
    private static final Map<UUID, Boolean> PREVIEW_ENABLED_BY_PLAYER = new ConcurrentHashMap<>();

    private final Ref<ChunkStore> mobFanRef;
    private final Vector3i blockPosition;
    private final int rotationIndex;
    private boolean previewEnabled;

    public MobFanUpgradePage(PlayerRef playerRef, Ref<ChunkStore> mobFanRef, BlockPosition blockPosition, int rotationIndex) {
        super(playerRef, CustomPageLifetime.CanDismiss, MobFanEventData.CODEC);
        this.mobFanRef = mobFanRef;
        this.blockPosition = new Vector3i(blockPosition.x, blockPosition.y, blockPosition.z);
        this.rotationIndex = rotationIndex;
    }

    @Override
    public void build(Ref<EntityStore> ref, UICommandBuilder commands, UIEventBuilder events, Store<EntityStore> store) {
        commands.append(PAGE_UI);
        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        Inventory inventory = player.getInventory();
        previewEnabled = PREVIEW_ENABLED_BY_PLAYER.getOrDefault(playerRef.getUuid(), false);
        buildUpgradeSlots(commands, events, true);
        buildPlayerInventory(commands, events, inventory, true);
        updatePreviewButton(commands);
        events.addEventBinding(CustomUIEventBindingType.Activating, "#PreviewButton", EventData.of("Action", "preview_toggle"), false);
        if (previewEnabled) {
            MobFanComponent mobFan = getMobFanComponent();
            if (mobFan != null) {
                showPreview(mobFan);
            }
        }
    }

    @Override
    public void handleDataEvent(Ref<EntityStore> ref, Store<EntityStore> store, MobFanEventData data) {
        if (data == null || data.action == null) {
            return;
        }

        Player player = store.getComponent(ref, Player.getComponentType());
        if (player == null) {
            return;
        }

        MobFanComponent mobFan = getMobFanComponent();
        if (mobFan == null) {
            return;
        }

        if (data.action.equals("preview_toggle")) {
            previewEnabled = !previewEnabled;
            PREVIEW_ENABLED_BY_PLAYER.put(playerRef.getUuid(), previewEnabled);
            if (previewEnabled) {
                showPreview(mobFan);
            } else {
                clearPreview();
            }
            UICommandBuilder update = new UICommandBuilder();
            updatePreviewButton(update);
            sendUpdate(update, false);
            return;
        }

        if (data.action.startsWith("inv_left:") || data.action.startsWith("inv_right:")) {
            handleInventoryClick(player, mobFan, data.action);
        } else if (data.action.startsWith("hotbar_left:") || data.action.startsWith("hotbar_right:")) {
            handleHotbarClick(player, mobFan, data.action);
        } else if (data.action.startsWith("upgrade_left:") || data.action.startsWith("upgrade_right:")) {
            handleUpgradeClick(player, mobFan, data.action);
        }

        UICommandBuilder update = new UICommandBuilder();
        buildUpgradeSlots(update, null, false);
        buildPlayerInventory(update, null, player.getInventory(), false);
        if (previewEnabled) {
            showPreview(mobFan);
        }
        sendUpdate(update, false);
    }

    @Override
    public void onDismiss(Ref<EntityStore> ref, Store<EntityStore> store) {
    }

    private void handleInventoryClick(Player player, MobFanComponent mobFan, String action) {
        boolean isRight = action.startsWith("inv_right:");
        int slot = parseSlot(action);
        if (slot < 0) {
            return;
        }

        ItemContainer inventory = player.getInventory().getStorage();
        moveFromInventorySlot(player, mobFan, inventory, slot, isRight);
    }

    private void handleHotbarClick(Player player, MobFanComponent mobFan, String action) {
        boolean isRight = action.startsWith("hotbar_right:");
        int slot = parseSlot(action);
        if (slot < 0) {
            return;
        }

        ItemContainer hotbar = player.getInventory().getHotbar();
        moveFromInventorySlot(player, mobFan, hotbar, slot, isRight);
    }

    private void moveFromInventorySlot(Player player, MobFanComponent mobFan, ItemContainer container, int slot, boolean single) {
        ItemStack item = container.getItemStack((short) slot);
        if (ItemStack.isEmpty(item)) {
            return;
        }

        UpgradeType type = UpgradeType.fromItemId(item.getItemId());
        if (type == null) {
            return;
        }

        int current = getUpgradeCount(mobFan, type);
        int available = MobFanConstants.FAN_UPGRADE_MAX - current;
        if (available <= 0) {
            return;
        }

        int toMove = single ? 1 : Math.min(available, item.getQuantity());
        if (toMove <= 0) {
            return;
        }

        ItemStack toRemove = new ItemStack(item.getItemId(), toMove);
        container.removeItemStackFromSlot((short) slot, toRemove, toMove, false, true);
        setUpgradeCount(mobFan, type, current + toMove);
        markChunkDirty();
    }

    private void handleUpgradeClick(Player player, MobFanComponent mobFan, String action) {
        boolean removeAll = action.startsWith("upgrade_left:");
        int slotIndex = parseSlot(action);
        UpgradeType type = UpgradeType.fromIndex(slotIndex);
        if (type == null) {
            return;
        }

        int current = getUpgradeCount(mobFan, type);
        if (current <= 0) {
            return;
        }

        int toRemove = removeAll ? current : 1;
        setUpgradeCount(mobFan, type, current - toRemove);
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }
        SimpleItemContainer.addOrDropItemStack(ref.getStore(), ref, player.getInventory().getCombinedHotbarFirst(), new ItemStack(type.itemId, toRemove));
        markChunkDirty();
    }

    private void buildUpgradeSlots(UICommandBuilder commands, UIEventBuilder events, boolean fullRebuild) {
        MobFanComponent mobFan = getMobFanComponent();
        if (mobFan == null) {
            return;
        }

        if (fullRebuild) {
            commands.clear("#UpgradeSlotsContainer");
        }

        UpgradeType[] types = UpgradeType.values();
        for (int i = 0; i < types.length; i++) {
            UpgradeType type = types[i];
            String selector = "#UpgradeSlotsContainer[" + i + "]";
            if (fullRebuild) {
                commands.append("#UpgradeSlotsContainer", UPGRADE_SLOT_UI);
            }

            int count = getUpgradeCount(mobFan, type);
            commands.set(selector + " #UpgradeLabel.Text", type.displayName);
            if (count > 0) {
                commands.set(selector + " #SlotItem.ItemId", type.itemId);
                commands.set(selector + " #QuantityLabel.Text", String.valueOf(count));
                commands.set(selector + " #QuantityLabel.Visible", count > 1);
            } else {
                commands.set(selector + " #SlotItem.ItemId", "");
                commands.set(selector + " #QuantityLabel.Visible", false);
            }
            commands.set(selector + ".TooltipTextSpans", Message.raw(type.displayName + " (" + count + "/" + MobFanConstants.FAN_UPGRADE_MAX + ")"));

            if (events != null) {
                events.addEventBinding(CustomUIEventBindingType.Activating, selector, EventData.of("Action", "upgrade_left:" + i), false);
                events.addEventBinding(CustomUIEventBindingType.RightClicking, selector, EventData.of("Action", "upgrade_right:" + i), false);
            }
        }
    }

    private void buildPlayerInventory(UICommandBuilder commands, UIEventBuilder events, Inventory inventory, boolean fullRebuild) {
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
        commands.set("#PreviewButtonLabel.Text", previewEnabled ? "Preview: On" : "Preview: Off");
    }

    private void showPreview(MobFanComponent mobFan) {
        clearPreview();
        RotationTuple rot = RotationTuple.get(rotationIndex);
        Vector3d forward = Rotation.rotate(mobFan.getBaseForward(), rot.yaw(), rot.pitch(), rot.roll()).normalize();
        Vector3d blockCenter = new Vector3d(blockPosition.x + 0.5, blockPosition.y + 0.5, blockPosition.z + 0.5);

        double length = mobFan.getFanLength();
        double width = mobFan.getFanWidth();
        double height = mobFan.getFanHeight();
        double start = 0.5;
        Vector3d boxCenter = blockCenter.clone().add(forward.clone().scale(start + length * 0.5));

        Matrix4d matrix = new Matrix4d().identity();
        Matrix4d tmp = new Matrix4d();
        matrix.translate(boxCenter);
        matrix.rotateEuler(rot.pitch().getRadians(), rot.yaw().getRadians(), rot.roll().getRadians(), tmp);
        matrix.scale(width, height, length);

        DisplayDebug packet = new DisplayDebug(DebugShape.Cube, matrix.asFloatData(), PREVIEW_COLOR, PREVIEW_DURATION_SECONDS, true, null);
        playerRef.getPacketHandler().write(packet);
    }

    private void clearPreview() {
        playerRef.getPacketHandler().write(new ClearDebugShapes());
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

    private int getUpgradeCount(MobFanComponent mobFan, UpgradeType type) {
        return switch (type) {
            case LENGTH -> mobFan.getLengthUpgrades();
            case WIDTH -> mobFan.getWidthUpgrades();
            case HEIGHT -> mobFan.getHeightUpgrades();
        };
    }

    private void setUpgradeCount(MobFanComponent mobFan, UpgradeType type, int count) {
        switch (type) {
            case LENGTH -> mobFan.setLengthUpgrades(count);
            case WIDTH -> mobFan.setWidthUpgrades(count);
            case HEIGHT -> mobFan.setHeightUpgrades(count);
        }
        persistMobFan(mobFan);
    }

    private void markChunkDirty() {
        Ref<EntityStore> ref = playerRef.getReference();
        if (ref == null) {
            return;
        }

        Store<EntityStore> store = ref.getStore();
        World world = store.getExternalData().getWorld();
        WorldChunk chunk = world.getChunkIfInMemory(ChunkUtil.indexChunkFromBlock(blockPosition.x, blockPosition.z));
        if (chunk != null) {
            chunk.markNeedsSaving();
        }
    }

    private void persistMobFan(MobFanComponent mobFan) {
        if (mobFanRef == null || !mobFanRef.isValid()) {
            return;
        }

        Store<ChunkStore> store = mobFanRef.getStore();
        if (store == null) {
            return;
        }

        store.putComponent(mobFanRef, MobFanComponent.getComponentType(), mobFan);
    }

    @Nullable
    private MobFanComponent getMobFanComponent() {
        if (mobFanRef == null || !mobFanRef.isValid()) {
            return null;
        }

        Store<ChunkStore> chunkStore = mobFanRef.getStore();
        if (chunkStore == null) {
            return null;
        }

        return chunkStore.getComponent(mobFanRef, MobFanComponent.getComponentType());
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

    private enum UpgradeType {
        LENGTH(0, "Length", MobFanConstants.UPGRADE_LENGTH_ITEM_ID),
        WIDTH(1, "Width", MobFanConstants.UPGRADE_WIDTH_ITEM_ID),
        HEIGHT(2, "Height", MobFanConstants.UPGRADE_HEIGHT_ITEM_ID);

        private final int index;
        private final String displayName;
        private final String itemId;

        UpgradeType(int index, String displayName, String itemId) {
            this.index = index;
            this.displayName = displayName;
            this.itemId = itemId;
        }

        @Nullable
        private static UpgradeType fromIndex(int index) {
            for (UpgradeType type : values()) {
                if (type.index == index) {
                    return type;
                }
            }
            return null;
        }

        @Nullable
        private static UpgradeType fromItemId(String itemId) {
            for (UpgradeType type : values()) {
                if (type.itemId.equals(itemId)) {
                    return type;
                }
            }
            return null;
        }
    }

    public static final class MobFanEventData {
        public static final BuilderCodec<MobFanEventData> CODEC = BuilderCodec.builder(
                MobFanEventData.class,
                MobFanEventData::new
        )
                .append(new KeyedCodec<>("Action", Codec.STRING), (o, v) -> o.action = v, o -> o.action)
                .add()
                .build();

        private String action;

        private MobFanEventData() {
        }
    }
}
