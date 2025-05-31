package dev.journey.PathSeeker.modules.automation;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.SmithingTableBlock;
import net.minecraft.client.gui.screen.ingame.SmithingScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.trim.ArmorTrim;
import net.minecraft.item.trim.ArmorTrimMaterial;
import net.minecraft.item.trim.ArmorTrimMaterials;
import net.minecraft.item.trim.ArmorTrimPattern;
import net.minecraft.item.trim.ArmorTrimPatterns;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.SmithingScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.LinkedList;
import java.util.Queue;

public class AutoSmithingTable extends Module {
    // Slot indices for Smithing Table
    // Based on net.minecraft.screen.SmithingScreenHandler
    // Player inventory typically starts at slot 4 in this handler
    // Input slots:
    // Slot 0: Smithing Template Slot
    // Slot 1: Base Gear Slot (e.g., Diamond Armor/Tool)
    // Slot 2: Upgrade Material Slot (e.g., Netherite Ingot)
    // Output slot:
    // Slot 3: Result Slot
    final int TEMPLATE_SLOT_INDEX = 0;
    final int BASE_ITEM_SLOT_INDEX = 1;
    final int UPGRADE_MATERIAL_SLOT_INDEX = 2;
    final int RESULT_SLOT_INDEX = 3;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgItems = settings.createGroup("Items to Upgrade");
    private final SettingGroup sgTrims = settings.createGroup("Armor Trims");

    private final Setting<Boolean> useKillAura = sgGeneral.add(new BoolSetting.Builder()
            .name("use-killaura-for-exp") // Though smithing doesn't use EXP, kept for potential future use or consistency
            .description("Enable KillAura for experience gathering (Not directly used by smithing)")
            .defaultValue(Boolean.FALSE)
            .build()
    );

    private final Setting<Boolean> allowHotbar = sgGeneral.add(new BoolSetting.Builder()
            .name("allow-hotbar")
            .description("Allow items in the hotbar to be used")
            .defaultValue(Boolean.TRUE)
            .build()
    );

    private final Setting<Integer> reach = sgGeneral.add(new IntSetting.Builder()
            .name("reach-distance")
            .description("Maximum distance to reach for a smithing table")
            .defaultValue(Integer.valueOf(3))
            .min(1)
            .sliderMax(6)
            .build()
    );

    private final Setting<Integer> actionsPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("actions-per-tick")
            .description("Maximum number of actions to perform per tick")
            .defaultValue(Integer.valueOf(5))
            .min(1)
            .sliderMax(20)
            .build()
    );

    private final Setting<Boolean> useTemplates = sgGeneral.add(new BoolSetting.Builder()
            .name("use-smithing-templates")
            .description("Whether to use smithing templates for upgrades (e.g., Netherite Upgrade Smithing Template). If false, only Netherite Ingots will be used (for 1.19.4-like behavior if applicable, otherwise this might not work as expected).")
            .defaultValue(Boolean.TRUE)
            .build()
    );

    // Item upgrade settings
    private final Setting<Boolean> upgradeSwords = sgItems.add(new BoolSetting.Builder().name("upgrade-swords").defaultValue(Boolean.TRUE).build());
    private final Setting<Boolean> upgradePickaxes = sgItems.add(new BoolSetting.Builder().name("upgrade-pickaxes").defaultValue(Boolean.TRUE).build());
    private final Setting<Boolean> upgradeAxes = sgItems.add(new BoolSetting.Builder().name("upgrade-axes").defaultValue(Boolean.TRUE).build());
    private final Setting<Boolean> upgradeShovels = sgItems.add(new BoolSetting.Builder().name("upgrade-shovels").defaultValue(Boolean.TRUE).build());
    private final Setting<Boolean> upgradeHoes = sgItems.add(new BoolSetting.Builder().name("upgrade-hoes").defaultValue(Boolean.TRUE).build());
    // No bows/crossbows/maces for netherite upgrade

    private final Setting<Boolean> upgradeHelmets = sgItems.add(new BoolSetting.Builder().name("upgrade-helmets").defaultValue(Boolean.TRUE).build());
    private final Setting<Boolean> upgradeChestplates = sgItems.add(new BoolSetting.Builder().name("upgrade-chestplates").defaultValue(Boolean.TRUE).build());
    private final Setting<Boolean> upgradeLeggings = sgItems.add(new BoolSetting.Builder().name("upgrade-leggings").defaultValue(Boolean.TRUE).build());
    private final Setting<Boolean> upgradeBoots = sgItems.add(new BoolSetting.Builder().name("upgrade-boots").defaultValue(Boolean.TRUE).build());
    // No elytra for netherite upgrade via smithing table in this manner

    // Armor Trim Settings
    private final Setting<Boolean> applyTrims = sgTrims.add(new BoolSetting.Builder()
        .name("apply-trims")
        .description("Whether to apply armor trims using the smithing table.")
        .defaultValue(Boolean.FALSE)
        .build()
    );

    private final Setting<Boolean> trimDiamondArmor = sgTrims.add(new BoolSetting.Builder()
        .name("trim-diamond-armor")
        .description("Apply selected trims to diamond armor.")
        .defaultValue(Boolean.TRUE)
        .build()
    );

    private final Setting<Boolean> trimNetheriteArmor = sgTrims.add(new BoolSetting.Builder()
        .name("trim-netherite-armor")
        .description("Apply selected trims to netherite armor.")
        .defaultValue(Boolean.TRUE)
        .build()
    );

    // Helmet Trim Settings
    private final Setting<TrimTemplateType> helmetTrimTemplate = sgTrims.add(new EnumSetting.Builder<TrimTemplateType>()
        .name("helmet-trim-template")
        .description("Smithing template to use for helmet trims.")
        .defaultValue(TrimTemplateType.NONE)
        .build()
    );
    private final Setting<TrimMaterialType> helmetTrimMaterial = sgTrims.add(new EnumSetting.Builder<TrimMaterialType>()
        .name("helmet-trim-material")
        .description("Material to use for helmet trims.")
        .defaultValue(TrimMaterialType.NONE)
        .build()
    );

    // Chestplate Trim Settings
    private final Setting<TrimTemplateType> chestplateTrimTemplate = sgTrims.add(new EnumSetting.Builder<TrimTemplateType>()
        .name("chestplate-trim-template")
        .description("Smithing template to use for chestplate trims.")
        .defaultValue(TrimTemplateType.NONE)
        .build()
    );
    private final Setting<TrimMaterialType> chestplateTrimMaterial = sgTrims.add(new EnumSetting.Builder<TrimMaterialType>()
        .name("chestplate-trim-material")
        .description("Material to use for chestplate trims.")
        .defaultValue(TrimMaterialType.NONE)
        .build()
    );

    // Leggings Trim Settings
    private final Setting<TrimTemplateType> leggingsTrimTemplate = sgTrims.add(new EnumSetting.Builder<TrimTemplateType>()
        .name("leggings-trim-template")
        .description("Smithing template to use for leggings trims.")
        .defaultValue(TrimTemplateType.NONE)
        .build()
    );
    private final Setting<TrimMaterialType> leggingsTrimMaterial = sgTrims.add(new EnumSetting.Builder<TrimMaterialType>()
        .name("leggings-trim-material")
        .description("Material to use for leggings trims.")
        .defaultValue(TrimMaterialType.NONE)
        .build()
    );

    // Boots Trim Settings
    private final Setting<TrimTemplateType> bootsTrimTemplate = sgTrims.add(new EnumSetting.Builder<TrimTemplateType>()
        .name("boots-trim-template")
        .description("Smithing template to use for boots trims.")
        .defaultValue(TrimTemplateType.NONE)
        .build()
    );
    private final Setting<TrimMaterialType> bootsTrimMaterial = sgTrims.add(new EnumSetting.Builder<TrimMaterialType>()
        .name("boots-trim-material")
        .description("Material to use for boots trims.")
        .defaultValue(TrimMaterialType.NONE)
        .build()
    );

    private final Queue<Runnable> actionQueue = new LinkedList<>();
    private PauseReason paused = PauseReason.NONE;
    private int actionsThisTick = 0;

    // New state flags for managing tick delays
    private boolean waitingForSmithingResultAfterInputsPlaced = false;
    private String currentOperationTypeForLogging = ""; // e.g., "[Upgrade]" or "[Trim]"

    private boolean upgradesDone = false;

    public AutoSmithingTable() {
        super(PathSeeker.Automation, "AutoSmithingTable", "Automatically upgrades diamond gear to netherite using a smithing table.");
    }

    @Override
    public void onActivate() {
        paused = PauseReason.NONE;
        actionQueue.clear();
        waitingForSmithingResultAfterInputsPlaced = false;
        currentOperationTypeForLogging = "";
        upgradesDone = false;
        if (mc.player == null || mc.world == null) {
            this.toggle();
            return;
        }
        if (!anyItemEnabled() && !anyTrimEnabled()) {
            ChatUtils.info("AutoSmithingTable: No items enabled for upgrade or trimming. Disabling.");
            this.toggle();
        }
    }

    private boolean anyItemEnabled() {
        return upgradeSwords.get() || upgradePickaxes.get() || upgradeAxes.get() || upgradeShovels.get() || upgradeHoes.get() ||
                upgradeHelmets.get() || upgradeChestplates.get() || upgradeLeggings.get() || upgradeBoots.get();
    }

    private boolean anyTrimEnabled() {
        if (!applyTrims.get()) return false;
        if (!trimDiamondArmor.get() && !trimNetheriteArmor.get()) return false;

        boolean anyTemplate = helmetTrimTemplate.get() != TrimTemplateType.NONE ||
                              chestplateTrimTemplate.get() != TrimTemplateType.NONE ||
                              leggingsTrimTemplate.get() != TrimTemplateType.NONE ||
                              bootsTrimTemplate.get() != TrimTemplateType.NONE;

        boolean anyMaterial = helmetTrimMaterial.get() != TrimMaterialType.NONE ||
                              chestplateTrimMaterial.get() != TrimMaterialType.NONE ||
                              leggingsTrimMaterial.get() != TrimMaterialType.NONE ||
                              bootsTrimMaterial.get() != TrimMaterialType.NONE;

        return anyTemplate && anyMaterial;
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerRespawnS2CPacket) {
            ChatUtils.info("AutoSmithingTable: Player respawned, disabling.");
            this.toggle();
        }
    }

    private void pause(PauseReason reason, String clientMessage) {
        paused = reason;
        ChatUtils.info("AutoSmithingTable: Paused - " + clientMessage);
        actionQueue.clear();
        waitingForSmithingResultAfterInputsPlaced = false;
        currentOperationTypeForLogging = "";
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) {
            this.toggle();
            return;
        }

        if (paused != PauseReason.NONE) {
            if (paused == PauseReason.ERROR) {
                this.toggle();
            }
            return;
        }

        actionsThisTick = 0;
        if (runActions()) return;

        AutoSmithingScreen currentScreen = getCurrentScreen();

        if (currentScreen == AutoSmithingScreen.NONE) {
            if (actionQueue.isEmpty()) {
                BlockPos smithingTablePos = findNearestSmithingTable(reach.get());
                if (smithingTablePos == null) {
                    pause(PauseReason.NO_SMITHING_TABLE, "No smithing table found nearby.");
                    return;
                }
                actionQueue.add(() -> {
                    mc.interactionManager.interactBlock(mc.player, mc.player.getActiveHand(), new BlockHitResult(
                            Vec3d.ofCenter(smithingTablePos), Direction.UP, smithingTablePos, false
                    ));
                });
            }
            if (runActions()) return;
            return;
        }

        if (currentScreen == AutoSmithingScreen.SMITHING) {
            if (!(mc.currentScreen instanceof SmithingScreen)) {
                pause(PauseReason.ERROR, "Expected SmithingScreen but found something else.");
                return;
            }
            SmithingScreenHandler handler = ((SmithingScreen) mc.currentScreen).getScreenHandler();

            if (!actionQueue.isEmpty()) {
                if (runActions()) return;
                return;
            }

            // First try to upgrade items if we haven't finished upgrades
            if (!upgradesDone) {
                int upgradableDiamondItemPlayerSlot = findNextUpgradableItemSlot(handler);
                if (upgradableDiamondItemPlayerSlot != -1) {
                    int netheriteIngotPlayerSlot = findMaterialSlot(handler, Items.NETHERITE_INGOT);
                    if (netheriteIngotPlayerSlot != -1) {
                        int templatePlayerSlot = -1;
                        if (useTemplates.get()) {
                            templatePlayerSlot = findMaterialSlot(handler, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
                            if (templatePlayerSlot == -1) {
                                pause(PauseReason.NO_MATERIALS_FOR_UPGRADE, "No Netherite Upgrade Smithing Template found.");
                                return;
                            }
                        }
                        ChatUtils.info("AutoSmithingTable: Starting Netherite Upgrades.");
                        queueNetheriteUpgradeInputActions(handler, upgradableDiamondItemPlayerSlot, netheriteIngotPlayerSlot, templatePlayerSlot);
                        if (runActions()) return;
                        return;
                    } else {
                        upgradesDone = true;
                    }
                } else {
                    upgradesDone = true;
                }
            }

            // Then try to apply trims if upgrades are done
            if (applyTrims.get() && (trimDiamondArmor.get() || trimNetheriteArmor.get())) {
                TrimmableItemInfo trimmableItemInfo = findNextTrimmableItemSlot(handler);
                if (trimmableItemInfo != null) {
                    Item armorItem = handler.getSlot(trimmableItemInfo.slot).getStack().getItem();
                    TrimTemplateType templateType = getTrimTemplateForArmor(armorItem);
                    TrimMaterialType materialType = getTrimMaterialForArmor(armorItem);

                    if (templateType != null && templateType != TrimTemplateType.NONE && templateType.item != null && templateType.patternKey != null &&
                        materialType != null && materialType != TrimMaterialType.NONE && materialType.item != null && materialType.materialKey != null) {

                        int trimTemplatePlayerSlot = findMaterialSlot(handler, templateType.item);
                        if (trimTemplatePlayerSlot == -1) {
                            pause(PauseReason.NO_MATERIALS_FOR_TRIM, "No Smithing Template found for " + templateType.title + ".");
                            return;
                        }
                        int trimMaterialPlayerSlot = findMaterialSlot(handler, materialType.item);
                        if (trimMaterialPlayerSlot == -1) {
                            pause(PauseReason.NO_MATERIALS_FOR_TRIM, "No Trim Material found for " + materialType.title + ".");
                            return;
                        }
                        ChatUtils.info("AutoSmithingTable: Starting armor trims.");
                        queueTrimInputActions(handler, trimmableItemInfo.slot, trimTemplatePlayerSlot, trimMaterialPlayerSlot);
                        if (runActions()) return;
                        return;
                    }
                }
            }

            // If we get here, check if we're done with everything
            if (actionQueue.isEmpty()) {
                boolean canUpgrade = findNextUpgradableItemSlot(handler) != -1 && 
                                   findMaterialSlot(handler, Items.NETHERITE_INGOT) != -1 && 
                                   (!useTemplates.get() || findMaterialSlot(handler, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE) != -1);
                boolean canTrim = applyTrims.get() && findNextTrimmableItemSlot(handler) != null;

                if (!canUpgrade && !canTrim) {
                    ChatUtils.info("AutoSmithingTable: All operations completed. Turning off.");
                    this.toggle();
                }
            }
        }
    }

    private boolean runActions() {
        while (actionsThisTick < actionsPerTick.get() && !actionQueue.isEmpty()) {
            Runnable action = actionQueue.peek();
            if (action != null) {
                action.run();
                actionQueue.poll();
                actionsThisTick++;
            }
        }
        return !actionQueue.isEmpty();
    }

    private AutoSmithingScreen getCurrentScreen() {
        if (mc.currentScreen == null) return AutoSmithingScreen.NONE;
        if (mc.currentScreen instanceof SmithingScreen) return AutoSmithingScreen.SMITHING;
        return AutoSmithingScreen.UNKNOWN;
    }

    private void queueNetheriteUpgradeInputActions(SmithingScreenHandler handler, int itemSlot, int ingotSlot, int templateSlot) {
        if (useTemplates.get() && templateSlot != -1) {
            final int finalTemplateSlot = templateSlot;
            actionQueue.add(() -> {
                mc.interactionManager.clickSlot(handler.syncId, finalTemplateSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(handler.syncId, TEMPLATE_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
            });
        } else if (useTemplates.get() && templateSlot == -1) {
            actionQueue.add(() -> pause(PauseReason.ERROR, "[Upgrade] Template missing when queueing actions."));
            return;
        }
        final int finalItemSlot = itemSlot;
        actionQueue.add(() -> {
            mc.interactionManager.clickSlot(handler.syncId, finalItemSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(handler.syncId, BASE_ITEM_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
        });
        final int finalIngotSlot = ingotSlot;
        actionQueue.add(() -> {
            mc.interactionManager.clickSlot(handler.syncId, finalIngotSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(handler.syncId, UPGRADE_MATERIAL_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
        });
        // Queue taking the result immediately after placing inputs
        actionQueue.add(() -> {
            if (mc.player == null || mc.interactionManager == null) return;
            ItemStack resultStack = handler.getSlot(RESULT_SLOT_INDEX).getStack();
            if (!resultStack.isEmpty() && resultStack.getItem() != Items.AIR) {
                mc.interactionManager.clickSlot(handler.syncId, RESULT_SLOT_INDEX, 0, SlotActionType.QUICK_MOVE, mc.player);
                ChatUtils.info("AutoSmithingTable: Netherite Upgrade Complete.");
            }
            // Find next item to upgrade and queue it immediately
            int nextItemSlot = findNextUpgradableItemSlot(handler);
            if (nextItemSlot != -1) {
                int nextIngotSlot = findMaterialSlot(handler, Items.NETHERITE_INGOT);
                if (nextIngotSlot != -1) {
                    int nextTemplateSlot = useTemplates.get() ? findMaterialSlot(handler, Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE) : -1;
                    if (!useTemplates.get() || nextTemplateSlot != -1) {
                        queueNetheriteUpgradeInputActions(handler, nextItemSlot, nextIngotSlot, nextTemplateSlot);
                    }
                }
            }
        });
    }

    private void queueTrimInputActions(SmithingScreenHandler handler, int armorPieceSlot, int trimTemplateSlot, int trimMaterialSlot) {
        final int finalArmorPieceSlot = armorPieceSlot;
        actionQueue.add(() -> {
            mc.interactionManager.clickSlot(handler.syncId, finalArmorPieceSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(handler.syncId, BASE_ITEM_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
        });
        final int finalTrimTemplateSlot = trimTemplateSlot;
        actionQueue.add(() -> {
            mc.interactionManager.clickSlot(handler.syncId, finalTrimTemplateSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(handler.syncId, TEMPLATE_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
        });
        final int finalTrimMaterialSlot = trimMaterialSlot;
        actionQueue.add(() -> {
            mc.interactionManager.clickSlot(handler.syncId, finalTrimMaterialSlot, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(handler.syncId, UPGRADE_MATERIAL_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
        });
        // Queue taking the result immediately after placing inputs
        actionQueue.add(() -> {
            if (mc.player == null || mc.interactionManager == null) return;
            ItemStack resultStack = handler.getSlot(RESULT_SLOT_INDEX).getStack();
            if (!resultStack.isEmpty() && resultStack.getItem() != Items.AIR) {
                mc.interactionManager.clickSlot(handler.syncId, RESULT_SLOT_INDEX, 0, SlotActionType.QUICK_MOVE, mc.player);
                ChatUtils.info("AutoSmithingTable: Armor Trim Complete.");
            }
            // Find next item to trim and queue it immediately
            TrimmableItemInfo nextItem = findNextTrimmableItemSlot(handler);
            if (nextItem != null) {
                Item armorItem = handler.getSlot(nextItem.slot).getStack().getItem();
                TrimTemplateType templateType = getTrimTemplateForArmor(armorItem);
                TrimMaterialType materialType = getTrimMaterialForArmor(armorItem);

                if (templateType != null && templateType != TrimTemplateType.NONE && templateType.item != null && templateType.patternKey != null &&
                    materialType != null && materialType != TrimMaterialType.NONE && materialType.item != null && materialType.materialKey != null) {

                    int nextTrimTemplateSlot = findMaterialSlot(handler, templateType.item);
                    int nextTrimMaterialSlot = findMaterialSlot(handler, materialType.item);
                    if (nextTrimTemplateSlot != -1 && nextTrimMaterialSlot != -1) {
                        queueTrimInputActions(handler, nextItem.slot, nextTrimTemplateSlot, nextTrimMaterialSlot);
                    }
                }
            }
        });
    }

    private void queueTakeSmithingResultAction(SmithingScreenHandler handler, String operationType) {
        actionQueue.add(() -> {
            if (mc.player == null || mc.interactionManager == null) return;
            ItemStack resultStack = handler.getSlot(RESULT_SLOT_INDEX).getStack();
            if (resultStack.isEmpty() || resultStack.getItem() == Items.AIR) {
                return;
            }

            if (operationType.equals("[Upgrade]")) {
                ChatUtils.info("AutoSmithingTable: Netherite Upgrade Complete.");
            } else if (operationType.equals("[Trim]")) {
                ChatUtils.info("AutoSmithingTable: Armor Trim Complete.");
            }

            mc.interactionManager.clickSlot(handler.syncId, RESULT_SLOT_INDEX, 0, SlotActionType.QUICK_MOVE, mc.player);

            if (!(mc.currentScreen instanceof SmithingScreen)) {
                actionQueue.clear();
            }
            currentOperationTypeForLogging = ""; 
        });
    }

    private void queueClearSmithingInputsActions(SmithingScreenHandler handler) {
        if (mc.player == null || mc.interactionManager == null) return;
        if (!handler.getSlot(TEMPLATE_SLOT_INDEX).getStack().isEmpty()) {
            actionQueue.add(() -> {
                if (mc.player == null || mc.interactionManager == null) return;
                mc.interactionManager.clickSlot(handler.syncId, TEMPLATE_SLOT_INDEX, 0, SlotActionType.QUICK_MOVE, mc.player);
            });
        }
        if (!handler.getSlot(BASE_ITEM_SLOT_INDEX).getStack().isEmpty()) {
            actionQueue.add(() -> {
                if (mc.player == null || mc.interactionManager == null) return;
                mc.interactionManager.clickSlot(handler.syncId, BASE_ITEM_SLOT_INDEX, 0, SlotActionType.QUICK_MOVE, mc.player);
            });
        }
        if (!handler.getSlot(UPGRADE_MATERIAL_SLOT_INDEX).getStack().isEmpty()) {
            actionQueue.add(() -> {
                if (mc.player == null || mc.interactionManager == null) return;
                mc.interactionManager.clickSlot(handler.syncId, UPGRADE_MATERIAL_SLOT_INDEX, 0, SlotActionType.QUICK_MOVE, mc.player);
            });
        }
    }

    private BlockPos findNearestSmithingTable(int radius) {
        if (mc.player == null || mc.world == null) return null;
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos smithingTablePos = null;
        double minDistanceSq = Double.MAX_VALUE;

        for (int x = -radius; x <= radius; x++) {
            for (int y = -radius; y <= radius; y++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos currentPos = playerPos.add(x, y, z);
                    if (mc.world.getBlockState(currentPos).getBlock() instanceof SmithingTableBlock) {
                        double distanceSq = playerPos.getSquaredDistance(currentPos);
                        if (distanceSq < minDistanceSq) {
                            minDistanceSq = distanceSq;
                            smithingTablePos = currentPos;
                        }
                    }
                }
            }
        }
        if (smithingTablePos != null && Math.sqrt(minDistanceSq) > radius) {
            return null;
        }
        return smithingTablePos;
    }

    private int findNextUpgradableItemSlot(SmithingScreenHandler handler) {
        int playerInvStartIndex = handler.slots.size() - 36;
        int endSlot = handler.slots.size();
        if (!allowHotbar.get()) {
            endSlot -= 9;
        }
        for (int i = playerInvStartIndex; i < endSlot; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (isDiamondUpgradable(stack.getItem())) {
                return i;
            }
        }
        return -1;
    }

    public Setting<Boolean> getUseKillAura() {
        return useKillAura;
    }

    private record TrimmableItemInfo(int slot, Item item) {
    }

    private TrimmableItemInfo findNextTrimmableItemSlot(SmithingScreenHandler handler) {
        if (!applyTrims.get() || mc.world == null) return null;
        int playerInvStartIndex = handler.slots.size() - 36;
        int endSlot = handler.slots.size();
        if (!allowHotbar.get()) {
            endSlot -= 9;
        }
        for (int i = playerInvStartIndex; i < endSlot; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.isEmpty()) continue;
            Item item = stack.getItem();

            boolean isDiamond = item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS;
            boolean isNetherite = item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS;

            if ((trimDiamondArmor.get() && isDiamond) || (trimNetheriteArmor.get() && isNetherite)) {
                TrimTemplateType templateType = getTrimTemplateForArmor(item);
                TrimMaterialType materialType = getTrimMaterialForArmor(item);

                if (templateType != null && templateType != TrimTemplateType.NONE && templateType.item != null && templateType.patternKey != null &&
                    materialType != null && materialType != TrimMaterialType.NONE && materialType.item != null && materialType.materialKey != null) {

                    ArmorTrim existingTrim = stack.get(DataComponentTypes.TRIM);
                    if (existingTrim != null) {
                        RegistryEntry<ArmorTrimPattern> existingPatternEntry = existingTrim.getPattern();
                        RegistryEntry<ArmorTrimMaterial> existingMaterialEntry = existingTrim.getMaterial();

                        RegistryKey<ArmorTrimPattern> configuredPatternKey = templateType.patternKey;
                        RegistryKey<ArmorTrimMaterial> configuredMaterialKey = materialType.materialKey;

                        if (existingPatternEntry.matchesKey(configuredPatternKey) && existingMaterialEntry.matchesKey(configuredMaterialKey)) {
                            continue;
                        }
                    }
                    return new TrimmableItemInfo(i, item);
                }
            }
        }
        return null;
    }

    private TrimTemplateType getTrimTemplateForArmor(Item armorItem) {
        if (armorItem == Items.DIAMOND_HELMET || armorItem == Items.NETHERITE_HELMET) return helmetTrimTemplate.get();
        if (armorItem == Items.DIAMOND_CHESTPLATE || armorItem == Items.NETHERITE_CHESTPLATE) return chestplateTrimTemplate.get();
        if (armorItem == Items.DIAMOND_LEGGINGS || armorItem == Items.NETHERITE_LEGGINGS) return leggingsTrimTemplate.get();
        if (armorItem == Items.DIAMOND_BOOTS || armorItem == Items.NETHERITE_BOOTS) return bootsTrimTemplate.get();
        return TrimTemplateType.NONE;
    }

    private TrimMaterialType getTrimMaterialForArmor(Item armorItem) {
        if (armorItem == Items.DIAMOND_HELMET || armorItem == Items.NETHERITE_HELMET) return helmetTrimMaterial.get();
        if (armorItem == Items.DIAMOND_CHESTPLATE || armorItem == Items.NETHERITE_CHESTPLATE) return chestplateTrimMaterial.get();
        if (armorItem == Items.DIAMOND_LEGGINGS || armorItem == Items.NETHERITE_LEGGINGS) return leggingsTrimMaterial.get();
        if (armorItem == Items.DIAMOND_BOOTS || armorItem == Items.NETHERITE_BOOTS) return bootsTrimMaterial.get();
        return TrimMaterialType.NONE;
    }

    private int findMaterialSlot(SmithingScreenHandler handler, Item material) {
        if (material == null) return -1; // Prevent NPE if a trim template/material is NONE.item
        int playerInvStartIndex = handler.slots.size() - 36;
        int endSlot = handler.slots.size();
        for (int i = playerInvStartIndex; i < endSlot; i++) {
            ItemStack stack = handler.getSlot(i).getStack();
            if (stack.getItem() == material) {
                return i;
            }
        }
        return -1;
    }

    private boolean isDiamondUpgradable(Item item) {
        if (item == Items.DIAMOND_SWORD && upgradeSwords.get()) return true;
        if (item == Items.DIAMOND_PICKAXE && upgradePickaxes.get()) return true;
        if (item == Items.DIAMOND_AXE && upgradeAxes.get()) return true;
        if (item == Items.DIAMOND_SHOVEL && upgradeShovels.get()) return true;
        if (item == Items.DIAMOND_HOE && upgradeHoes.get()) return true;
        if (item == Items.DIAMOND_HELMET && upgradeHelmets.get()) return true;
        if (item == Items.DIAMOND_CHESTPLATE && upgradeChestplates.get()) return true;
        if (item == Items.DIAMOND_LEGGINGS && upgradeLeggings.get()) return true;
        if (item == Items.DIAMOND_BOOTS && upgradeBoots.get()) return true;
        return false;
    }

    private boolean isArmorPiece(Item item) {
        return item == Items.DIAMOND_HELMET || item == Items.DIAMOND_CHESTPLATE || item == Items.DIAMOND_LEGGINGS || item == Items.DIAMOND_BOOTS ||
               item == Items.NETHERITE_HELMET || item == Items.NETHERITE_CHESTPLATE || item == Items.NETHERITE_LEGGINGS || item == Items.NETHERITE_BOOTS;
    }

    private enum AutoSmithingScreen { SMITHING, UNKNOWN, NONE }

    private enum PauseReason {
        NO_SMITHING_TABLE,
        NO_UPGRADABLE_ITEM,
        NO_TRIMMABLE_ITEM,
        NO_MATERIALS_FOR_UPGRADE,
        NO_MATERIALS_FOR_TRIM,
        UNKNOWN_SCREEN,
        ERROR,
        NONE
    }

    public enum TrimTemplateType {
        NONE("None", null, null),
        SENTRY("Sentry", Items.SENTRY_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SENTRY),
        DUNE("Dune", Items.DUNE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.DUNE),
        COAST("Coast", Items.COAST_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.COAST),
        WILD("Wild", Items.WILD_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.WILD),
        WARD("Ward", Items.WARD_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.WARD),
        EYE("Eye", Items.EYE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.EYE),
        VEX("Vex", Items.VEX_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.VEX),
        TIDE("Tide", Items.TIDE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.TIDE),
        SNOUT("Snout", Items.SNOUT_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SNOUT),
        RIB("Rib", Items.RIB_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.RIB),
        SPIRE("Spire", Items.SPIRE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SPIRE),
        SILENCE("Silence", Items.SILENCE_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SILENCE),
        WAYFINDER("Wayfinder", Items.WAYFINDER_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.WAYFINDER),
        RAISER("Raiser", Items.RAISER_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.RAISER),
        SHAPER("Shaper", Items.SHAPER_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.SHAPER),
        HOST("Host", Items.HOST_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.HOST),
        FLOW("Flow", Items.FLOW_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.FLOW),
        BOLT("Bolt", Items.BOLT_ARMOR_TRIM_SMITHING_TEMPLATE, ArmorTrimPatterns.BOLT);

        public final String title;
        public final Item item;
        public final RegistryKey<ArmorTrimPattern> patternKey;

        TrimTemplateType(String title, Item item, RegistryKey<ArmorTrimPattern> patternKey) {
            this.title = title;
            this.item = item;
            this.patternKey = patternKey;
        }

        @Override
        public String toString() {
            return title;
        }
    }

    public enum TrimMaterialType {
        NONE("None", null, null),
        EMERALD("Emerald", Items.EMERALD, ArmorTrimMaterials.EMERALD),
        REDSTONE("Redstone", Items.REDSTONE, ArmorTrimMaterials.REDSTONE),
        LAPIS_LAZULI("Lapis Lazuli", Items.LAPIS_LAZULI, ArmorTrimMaterials.LAPIS),
        AMETHYST_SHARD("Amethyst Shard", Items.AMETHYST_SHARD, ArmorTrimMaterials.AMETHYST),
        QUARTZ("Nether Quartz", Items.QUARTZ, ArmorTrimMaterials.QUARTZ),
        NETHERITE_INGOT("Netherite Ingot (Trim Material)", Items.NETHERITE_INGOT, ArmorTrimMaterials.NETHERITE),
        DIAMOND("Diamond", Items.DIAMOND, ArmorTrimMaterials.DIAMOND),
        GOLD_INGOT("Gold Ingot", Items.GOLD_INGOT, ArmorTrimMaterials.GOLD),
        IRON_INGOT("Iron Ingot", Items.IRON_INGOT, ArmorTrimMaterials.IRON),
        COPPER_INGOT("Copper Ingot", Items.COPPER_INGOT, ArmorTrimMaterials.COPPER);

        public final String title;
        public final Item item;
        public final RegistryKey<ArmorTrimMaterial> materialKey;

        TrimMaterialType(String title, Item item, RegistryKey<ArmorTrimMaterial> materialKey) {
            this.title = title;
            this.item = item;
            this.materialKey = materialKey;
        }

        @Override
        public String toString() {
            return title;
        }
    }
}