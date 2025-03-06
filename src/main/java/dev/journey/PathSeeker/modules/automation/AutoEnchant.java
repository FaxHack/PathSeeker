package dev.journey.PathSeeker.modules.automation;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.KillAura;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.AnvilBlock;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.*;
import java.util.stream.Collectors;

public class AutoEnchant extends Module {
    final int ENCHANTEE_SLOT_INDEX = 0;
    final int BOOK_SLOT_INDEX = 1;
    final int RESULT_SLOT_INDEX = 2;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgWeapons = settings.createGroup("Weapons");
    private final SettingGroup sgArmor = settings.createGroup("Armor");
    private final Setting<Boolean> useKillAura = sgGeneral.add(new BoolSetting.Builder()
            .name("use-killaura")
            .description("Enable KillAura for experience gathering")
            .defaultValue(false)
            .build()
    );
    private final Setting<Boolean> allowHotbar = sgGeneral.add(new BoolSetting.Builder()
            .name("allow-hotbar")
            .description("Allow items in the hotbar to be used")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> enchantDiamond = sgGeneral.add(new BoolSetting.Builder()
            .name("allow-diamond")
            .description("Allow netherite items to be enchanted")
            .defaultValue(true)
            .build()
    );
    private final Setting<Boolean> enchantNetherite = sgGeneral.add(new BoolSetting.Builder()
            .name("allow-netherite")
            .description("Allow diamond items to be enchanted")
            .defaultValue(true)
            .build()
    );

    //.defaultValue(new HashSet<>(Arrays.asList(Enchantments.EFFICIENCY)))
    private final Setting<Integer> reach = sgGeneral.add(new IntSetting.Builder()
            .name("reach-distance")
            .description("Maximum distance to reach for an anvil")
            .defaultValue(3)
            .min(1)
            .sliderMax(6)
            .build()
    );
    private final Setting<Integer> actionsPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("actions-per-tick")
            .description("Maximum number of actions to perform per tick")
            .defaultValue(5)
            .min(1)
            .sliderMax(20)
            .build()
    );
    // Weapons
    private final Setting<Boolean> doSwords = sgWeapons.add(new BoolSetting.Builder().name("enchant-swords").description("Enchant swords").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> swordEnchants = sgWeapons.add(new EnchantmentListSetting.Builder().name("sword-enchants").description("Enchantments to apply to swords").defaultValue().build());
    private final Setting<Boolean> doPicks = sgWeapons.add(new BoolSetting.Builder().name("enchant-picks").description("Enchant Pickaxes").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> pickEnchants = sgWeapons.add(new EnchantmentListSetting.Builder().name("pick-enchants").description("Enchantments to apply to pickaxes").defaultValue().defaultValue().build());
    private final Setting<Boolean> doAxe = sgWeapons.add(new BoolSetting.Builder().name("enchant-axes").description("Enchant Axes").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> axeEnchants = sgWeapons.add(new EnchantmentListSetting.Builder().name("axe-enchants").description("Enchantments to apply to axes").defaultValue().defaultValue().build());
    private final Setting<Boolean> doShovel = sgWeapons.add(new BoolSetting.Builder().name("enchant-shovels").description("Enchant Shovels").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> shovelEnchants = sgWeapons.add(new EnchantmentListSetting.Builder().name("shove-enchants").description("Enchantments to apply to shoves").defaultValue().defaultValue().build());
    private final Setting<Boolean> doHoe = sgWeapons.add(new BoolSetting.Builder().name("enchant-hoes").description("Enchant Hoes").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> hoeEnchants = sgWeapons.add(new EnchantmentListSetting.Builder().name("hoe-enchants").description("Enchantments to apply to hoes").defaultValue().defaultValue().build());
    private final Setting<Boolean> doBow = sgWeapons.add(new BoolSetting.Builder().name("enchant-bows").description("Enchant Bows").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> bowEnchants = sgWeapons.add(new EnchantmentListSetting.Builder().name("bow-enchants").description("Enchantments to apply to bows").defaultValue().defaultValue().build());
    private final Setting<Boolean> doCBow = sgWeapons.add(new BoolSetting.Builder().name("enchant-crossbows").description("Enchant Crossbows").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> crossbowEnchants = sgWeapons.add(new EnchantmentListSetting.Builder().name("crossbow-enchants").description("Enchantments to apply to crossbows").defaultValue().defaultValue().build());
    private final Setting<Boolean> doMace = sgWeapons.add(new BoolSetting.Builder().name("enchant-mace").description("Enchant Maces").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> maceEnchants = sgWeapons.add(new EnchantmentListSetting.Builder().name("mace-enchants").description("Enchantments to apply to maces").defaultValue().defaultValue().build());
    // Armor
    private final Setting<Boolean> doHelmets = sgArmor.add(new BoolSetting.Builder().name("enchant-helmet").description("Enchant Helmets").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> helmetEnchants = sgArmor.add(new EnchantmentListSetting.Builder().name("helmet-enchants").description("Enchantments to apply to helmets").defaultValue().defaultValue().build());
    private final Setting<Boolean> doChestplate = sgArmor.add(new BoolSetting.Builder().name("enchant-chestplate").description("Enchant Chestplate").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> chestplateEnchants = sgArmor.add(new EnchantmentListSetting.Builder().name("chestplate-enchants").description("Enchantments to apply to chestplate").defaultValue().defaultValue().build());
    private final Setting<Boolean> doLeggings = sgArmor.add(new BoolSetting.Builder().name("enchant-leggings").description("Enchant Leggings").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> leggingsEnchants = sgArmor.add(new EnchantmentListSetting.Builder().name("legging-enchants").description("Enchantments to apply to leggings").defaultValue().defaultValue().build());
    private final Setting<Boolean> doBoots = sgArmor.add(new BoolSetting.Builder().name("enchant-boots").description("Enchant Boots").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> bootEnchants = sgArmor.add(new EnchantmentListSetting.Builder().name("boot-enchants").description("Enchantments to apply to boots").defaultValue().defaultValue().build());
    private final Setting<Boolean> doElytra = sgArmor.add(new BoolSetting.Builder().name("enchant-elytra").description("Enchant Elytra").defaultValue(false).build());
    private final Setting<Set<RegistryKey<Enchantment>>> elytraEnchants = sgArmor.add(new EnchantmentListSetting.Builder().name("elytra-enchants").description("Enchantments to apply to elytras").defaultValue().defaultValue().build());
    private final Queue<Runnable> actionQueue = new LinkedList<>();
    private int PAUSE_NEEDED_XP = Integer.MAX_VALUE;
    private PauseReason paused = PauseReason.NONE;
    private int actionsThisTick = 0;

    public AutoEnchant() {
        super(PathSeeker.Automation, "AutoEnchanter", "Automatically enchants items at an anvil.");
    }

    @Override
    public void onActivate() {
        paused = PauseReason.NONE;
        if (mc.player == null || mc.world == null) {
            this.toggle();
            return;
        }

        if (!enchantDiamond.get() && !enchantNetherite.get()) {
            ChatUtils.info("Both diamond and netherite enchanting are disabled so there is nothing to do");
            this.toggle();
        }
    }

    @EventHandler
    private void onPacket(PacketEvent.Receive event) {
        // I think this will catch when a player rejoins so we don't immediately start trying to enchant
        if (event.packet instanceof PlayerRespawnS2CPacket) {
            this.toggle();
        }
    }

    private void pause(PauseReason reason, String message) {
        paused = reason;
        ChatUtils.info(message);
        actionQueue.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Not sure if this can be reached but might as well check so the linter doesn't complain
        if (mc.player == null || mc.world == null) {
            this.toggle();
            return;
        }

        // In the future this could recover from some
        if (paused != PauseReason.NONE) {
            if (paused == PauseReason.NO_EXP) {
                if (mc.player.experienceLevel < PAUSE_NEEDED_XP) {
                    // Check if we should turn on KA
                    if (useKillAura.get() && !Modules.get().get(KillAura.class).isActive()) {
                        ChatUtils.sendMsg("auto-enchant", Text.literal("Turning on KillAura"));
                        Modules.get().get(KillAura.class).toggle();
                    }
                } else {
                    paused = PauseReason.NONE;

                    // If we turned KA on then turn it off
                    if (useKillAura.get()) {
                        if (Modules.get().get(KillAura.class).isActive()) {
                            ChatUtils.sendMsg("auto-enchant", Text.literal("Turning off KillAura"));
                            Modules.get().get(KillAura.class).toggle();
                        }
                    }

                }
            } else {
                // We can't recover from this pause so just stop
                this.toggle();
            }
            return;
        }

        actionsThisTick = 0;
        if (!runActions()) return;

        if (getCurrentScreen() == AutoEnchantScreens.NONE) {
            var anvilPos = findNearestAnvil(reach.get());
            if (anvilPos == null) {
                paused = PauseReason.NO_ANVIL;
                ChatUtils.info("No anvil found nearby.");
                return;
            }
            actionQueue.add(() -> {
                // Open anvil
                mc.interactionManager.interactBlock(mc.player, mc.player.getActiveHand(), new BlockHitResult(
                        new Vec3d(anvilPos.getX(), anvilPos.getY(), anvilPos.getZ()),
                        Direction.DOWN,
                        anvilPos,
                        false
                ));
            });


            if (!runActions()) return;
        }

        if (getCurrentScreen() == AutoEnchantScreens.ANVIL) {
            var handler = ((AnvilScreen) mc.currentScreen).getScreenHandler();
            var enchanteeSlot = handler.getSlot(ENCHANTEE_SLOT_INDEX).getStack();
            var bookSlot = handler.getSlot(BOOK_SLOT_INDEX).getStack();


            // Action sets should never end in the middle of a cycle
            if (!enchanteeSlot.isEmpty() || !bookSlot.isEmpty()) {
                pause(PauseReason.ERROR, "Unexpected items in anvil slots.");
                return;
            }

            // Find next item to be enchanted
            var enchantableSlotIndex = getNextEnchantableItem(handler);
            if (enchantableSlotIndex == -1) {
                pause(PauseReason.NO_ENCHANTABLE_ITEM, "No enchantable item found in inventory.");
                return;
            }

            // The prior step actually checks this so the error should never happen
            var neededEnchants = getNeededEnchantments(handler.getSlot(enchantableSlotIndex).getStack());
            if (neededEnchants.isEmpty()) {
                pause(PauseReason.ERROR, "Trying to enchant an item but it doesn't need any more enchantments.");
                return;
            }


            var bookSlots = new int[neededEnchants.size()];
            for (int i = 0; i < neededEnchants.size(); i++) {
                var ebookSlot = getBookForEnchantment(handler, neededEnchants.get(i));
                if (ebookSlot == -1) {
                    paused = PauseReason.NO_BOOK;
                    ChatUtils.info("No book found for enchantment " + neededEnchants.get(i).getValue() + ". Pausing.");
                    this.toggle();
                    return;
                }
                bookSlots[i] = ebookSlot;
            }

            for (int i = 0; i < bookSlots.length; i++) {
                // The first and last runs are special as the item needs to be retrieved from/returned to the inventory
                final int enchanteeIndex = (i == 0) ? enchantableSlotIndex : ENCHANTEE_SLOT_INDEX;
                final int bookIndex = bookSlots[i];
                final int returnIndex = (i == bookSlots.length - 1) ? enchantableSlotIndex : ENCHANTEE_SLOT_INDEX;

                enchant(handler, enchanteeIndex, bookIndex, returnIndex);
            }

            if (!runActions()) {
            }
        }
    }

    /**
     * Runs up to `actionsPerTick` actions from the action queue
     *
     * @return true there are no more actions queued up, false if there are more actions to run
     */
    private boolean runActions() {
        while (actionsThisTick < actionsPerTick.get() && !actionQueue.isEmpty()) {
            actionsThisTick++;
            actionQueue.poll().run();
        }
        return actionQueue.isEmpty() && actionsThisTick < actionsPerTick.get();
    }

    /**
     * Simple wrapper to convert the current screen into a enum value. Was going to be useful in treating all types of
     * storage as a Storage screen, though ultimately I didn't go that route for the implementation.
     *
     * @return The current screen as an AutoEnchantScreens enum
     */
    private AutoEnchantScreens getCurrentScreen() {
        if (mc.currentScreen == null) return AutoEnchantScreens.NONE;
        if (mc.currentScreen instanceof AnvilScreen) return AutoEnchantScreens.ANVIL;
        // if(mc.currentScreen instanceof GenericContainerScreen) return AutoEnchantScreens.CONTAINER;
        // if(mc.currentScreen instanceof ShulkerBoxScreen) return AutoEnchantScreens.CONTAINER;
        return AutoEnchantScreens.UNKNOWN;
    }

    /**
     * Adds actions to perform the requested enchantment on the anvil
     *
     * @param hndl           the Anvil Screen Handler
     * @param enchanteeIndex the current index of the item to be enchanted.
     * @param bookIndex      then index of the book to be used for enchantment
     * @param returnIndex    the index to return the item to after enchantment, if the anvil breaks this will not be respected
     */
    private void enchant(AnvilScreenHandler hndl, int enchanteeIndex, int bookIndex, int returnIndex) {
        if (mc.interactionManager == null) return; // keep the linter happy

        // Move item to be enchanted to the enchantee slot unless its already there
        if (enchanteeIndex != ENCHANTEE_SLOT_INDEX) {
            actionQueue.add(() -> {
                mc.interactionManager.clickSlot(hndl.syncId, enchanteeIndex, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(hndl.syncId, ENCHANTEE_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
            });
        }

        // Move book into book slot
        actionQueue.add(() -> {
            mc.interactionManager.clickSlot(hndl.syncId, bookIndex, 0, SlotActionType.PICKUP, mc.player);
            mc.interactionManager.clickSlot(hndl.syncId, BOOK_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);
        });

        // Pick up newly enchanted item
        actionQueue.add(() -> {
            // Check if we have enough experience levels to do this enchantment
            if (hndl.getLevelCost() > mc.player.experienceLevel) {
                PAUSE_NEEDED_XP = hndl.getLevelCost();

                if (mc.currentScreen != null) mc.currentScreen.close();
                pause(PauseReason.NO_EXP, "Need atleast " + PAUSE_NEEDED_XP + " experience to enchant. Pausing.");
                return;
            }

            // Check if air is in result slot
            if (hndl.getSlot(RESULT_SLOT_INDEX).getStack().getItem() == Items.AIR) {
                pause(PauseReason.ERROR, "Attempted illegal enchant!");
                return;
            }

            // Otherwise we can just grab it
            mc.interactionManager.clickSlot(hndl.syncId, RESULT_SLOT_INDEX, 0, SlotActionType.PICKUP, mc.player);

            // That might have broken the anvil so check if the screen changed
            if (getCurrentScreen() != AutoEnchantScreens.ANVIL) {
                actionQueue.clear();
                return;
            }

            // Place the item in the return slot
            mc.interactionManager.clickSlot(hndl.syncId, returnIndex, 0, SlotActionType.PICKUP, mc.player);
        });
    }

    /**
     * Finds the near anvil to the player within the given radius
     *
     * @return the BlockPos of the nearest anvil or null if none are found
     */
    private BlockPos findNearestAnvil(int radius) {
        var playerPos = mc.player.getBlockPos();
        BlockPos anvilPos = null;
        int anvilDistance = Integer.MAX_VALUE;

        for (int x = -radius; x < radius; x++) {
            for (int y = -radius; y < radius; y++) {
                for (int z = -radius; z < radius; z++) {
                    var block = mc.world.getBlockState(playerPos.add(x, y, z)).getBlock();
                    if (block instanceof AnvilBlock) {
                        var distance = getChebyshevDistance(playerPos, playerPos.add(x, y, z));
                        if (distance < anvilDistance) {
                            anvilDistance = distance;
                            anvilPos = playerPos.add(x, y, z);
                        }
                    }
                }
            }
        }
        return anvilPos;
    }

    private int getChebyshevDistance(BlockPos pos1, BlockPos pos2) {
        return Math.max(Math.max(Math.abs(pos1.getX() - pos2.getX()), Math.abs(pos1.getY() - pos2.getY())), Math.abs(pos1.getZ() - pos2.getZ()));
    }

    /**
     * Find the next item in the inventory that should be enchanted
     *
     * @param handler the AnvilScreenHandler
     * @return the index of the next item to be enchanted or -1 if none are found
     */
    private int getNextEnchantableItem(AnvilScreenHandler handler) {
        var slotsEnd = handler.slots.size() - (allowHotbar.get() ? 0 : 9);
        for (int i = RESULT_SLOT_INDEX + 1; i < slotsEnd; i++) {
            var stack = handler.getSlot(i).getStack();
            if (isEnchantable(stack.getItem())) {
                var needs = getNeededEnchantments(stack);
                if (!needs.isEmpty()) return i;
            }
        }
        return -1;
    }

    /**
     * Wrapper get a list of enchantments applicable to the item as a HashSet. This will grab both applied enchantments
     * and stored enchantments (books) without any distinction between the two types.
     *
     * @param stack the item stack to check
     * @return a HashSet of contained enchantments
     */
    private HashSet<RegistryKey<Enchantment>> getEnchantments(ItemStack stack) {
        var components = new ComponentType[]{
                DataComponentTypes.ENCHANTMENTS,
                DataComponentTypes.STORED_ENCHANTMENTS,
        };

        HashSet<RegistryKey<Enchantment>> out = new HashSet<>();

        for (var component : components) {
            var enchantments = stack.get(component);
            if (enchantments instanceof ItemEnchantmentsComponent itemEnchants) {
                itemEnchants.getEnchantments().stream()
                        .map(enchant -> enchant.getKey().get())
                        .forEach(out::add);
            }
        }

        return out;
    }

    /**
     * Finds the index of the first book in the inventory that contains the requested enchantment
     *
     * @param handler     the AnvilScreenHandler
     * @param enchantment the enchantment to search for
     * @return the index of the book or -1 if none are found
     */
    private int getBookForEnchantment(AnvilScreenHandler handler, RegistryKey<Enchantment> enchantment) {
        var slotsEnd = handler.slots.size() - (allowHotbar.get() ? 0 : 9);
        for (int i = RESULT_SLOT_INDEX + 1; i < slotsEnd; i++) {
            var stack = handler.getSlot(i).getStack();
            if (stack.getItem() == Items.ENCHANTED_BOOK) {
                var enchants = getEnchantments(stack);
                if (enchants.contains(enchantment)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Determins if the current item is an item that should recieve enchantments
     *
     * @param item the item to check
     * @return true if the item should be enchanted, false otherwise
     */
    private boolean isEnchantable(Item item) {
        if (doSwords.get()) {
            if (item == Items.DIAMOND_SWORD) return enchantDiamond.get();
            if (item == Items.NETHERITE_SWORD) return enchantNetherite.get();
        }

        if (doPicks.get()) {
            if (item == Items.DIAMOND_PICKAXE) return enchantDiamond.get();
            if (item == Items.NETHERITE_PICKAXE) return enchantNetherite.get();
        }

        if (doAxe.get()) {
            if (item == Items.DIAMOND_AXE) return enchantDiamond.get();
            if (item == Items.NETHERITE_AXE) return enchantNetherite.get();
        }

        if (doShovel.get()) {
            if (item == Items.DIAMOND_SHOVEL) return enchantDiamond.get();
            if (item == Items.NETHERITE_SHOVEL) return enchantNetherite.get();
        }

        if (doHoe.get()) {
            if (item == Items.DIAMOND_HOE) return enchantDiamond.get();
            if (item == Items.NETHERITE_HOE) return enchantNetherite.get();
        }

        if (doBow.get()) {
            if (item == Items.BOW) return true;
        }

        if (doCBow.get()) {
            if (item == Items.CROSSBOW) return true;
        }

        if (doMace.get()) {
            if (item == Items.MACE) return true;
        }

        // Armor

        if (doHelmets.get()) {
            if (item == Items.DIAMOND_HELMET) return enchantDiamond.get();
            if (item == Items.NETHERITE_HELMET) return enchantNetherite.get();
        }

        if (doChestplate.get()) {
            if (item == Items.DIAMOND_CHESTPLATE) return enchantDiamond.get();
            if (item == Items.NETHERITE_CHESTPLATE) return enchantNetherite.get();
        }

        if (doLeggings.get()) {
            if (item == Items.DIAMOND_LEGGINGS) return enchantDiamond.get();
            if (item == Items.NETHERITE_LEGGINGS) return enchantNetherite.get();
        }

        if (doBoots.get()) {
            if (item == Items.DIAMOND_BOOTS) return enchantDiamond.get();
            if (item == Items.NETHERITE_BOOTS) return enchantNetherite.get();
        }

        if (doElytra.get()) {
            return item == Items.ELYTRA;
        }

        return false;
    }

    /**
     * Determines a list of Enchantments the item still needs to recieve
     *
     * @param stack the item stack to check
     * @return a list of enchantments
     */
    private List<RegistryKey<Enchantment>> getNeededEnchantments(ItemStack stack) {
        var currentEnchants = stack.getEnchantments().getEnchantments().stream()
                .map(enchant -> enchant.getKey().get())
                .collect(Collectors.toSet());
        var item = stack.getItem();

        if (item == Items.DIAMOND_SWORD || item == Items.NETHERITE_SWORD) {
            var wanted = swordEnchants.get();
            var needed = wanted.stream().filter(wantedEnchant -> {
                return !currentEnchants.contains(wantedEnchant);
            }).toList();
            return needed;
        }

        if (item == Items.DIAMOND_PICKAXE || item == Items.NETHERITE_PICKAXE) {
            return pickEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.DIAMOND_AXE || item == Items.NETHERITE_AXE) {
            return axeEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.DIAMOND_SHOVEL || item == Items.NETHERITE_SHOVEL) {
            return shovelEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.DIAMOND_HOE || item == Items.NETHERITE_HOE) {
            return hoeEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.BOW) {
            return bowEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.CROSSBOW) {
            return crossbowEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.MACE) {
            return maceEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.DIAMOND_HELMET || item == Items.NETHERITE_HELMET) {
            return helmetEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.DIAMOND_CHESTPLATE || item == Items.NETHERITE_CHESTPLATE) {
            return chestplateEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.DIAMOND_LEGGINGS || item == Items.NETHERITE_LEGGINGS) {
            return leggingsEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.DIAMOND_BOOTS || item == Items.NETHERITE_BOOTS) {
            return bootEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        if (item == Items.ELYTRA) {
            return elytraEnchants.get().stream().filter(e -> {
                return !currentEnchants.contains(e);
            }).toList();
        }

        return Collections.emptyList();
    }

    private enum AutoEnchantScreens {
        ANVIL,
        CONTAINER,
        UNKNOWN,
        NONE
    }

    private enum PauseReason {
        NO_ANVIL,
        NO_ENCHANTABLE_ITEM,
        NO_BOOK,
        NO_EXP,
        UNKNOWN_SCREEN,
        ERROR,
        NONE
    }

}