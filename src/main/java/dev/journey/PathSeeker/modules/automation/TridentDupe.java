// Credits to Killet, Laztec, and Ionar for contributing the source code for this module.
// https://github.com/Killetx/TridentDupe
// Credits to Journey For porting dupe to 1.21.1

package dev.journey.PathSeeker.modules.automation;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.systems.modules.Module;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.OpenScreenEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.client.gui.screen.DisconnectedScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

public class TridentDupe extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Double> delay = sgGeneral.add(new DoubleSetting.Builder()
            .name("dupe-delay")
            .description("Raise this if it isn't working. This is how fast you'll dupe. 5 is good for most.")
            .defaultValue(5)
            .build()
    );

    private final Setting<Boolean> dropTridents = sgGeneral.add(new BoolSetting.Builder()
            .name("dropTridents")
            .description("Drops tridents in your last hotbar slot.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> durabilityManagement = sgGeneral.add(new BoolSetting.Builder()
            .name("durabilityManagement")
            .description("(More AFKable) Attempts to dupe the highest durability trident in your hotbar.")
            .defaultValue(true)
            .build()
    );

    private final Queue<Packet<?>> delayedPackets = new LinkedList<>();
    private boolean cancel = true;
    private final List<TimedTask> scheduledTasks = new ArrayList<>();
    private final List<TimedTask> scheduledTasks2 = new ArrayList<>();

    public TridentDupe() {
        super(PathSeeker.Utility, "trident-dupe", "Dupes tridents in first hotbar slot.");
    }

    private static class TimedTask {
        private final long executeTime;
        private final Runnable task;

        TimedTask(long executeTime, Runnable task) {
            this.executeTime = executeTime;
            this.task = task;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST + 1)
    private void onSendPacket(PacketEvent.Send event) {
        if (event.packet instanceof ClientCommandC2SPacket
                || event.packet instanceof PlayerMoveC2SPacket
                || event.packet instanceof CloseHandledScreenC2SPacket)
            return;

        if (!(event.packet instanceof ClickSlotC2SPacket)
                && !(event.packet instanceof PlayerActionC2SPacket))
            return;

        if (!cancel) return;

        event.cancel();
    }

    @Override
    public void onActivate() {
        if (mc.player == null) return;

        Int2ObjectMap<ItemStack> modifiedStacks = new Int2ObjectOpenHashMap<>();
        modifiedStacks.put(3, mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot));
        modifiedStacks.put(36, mc.player.getInventory().getStack(mc.player.getInventory().selectedSlot));

        scheduledTasks.clear();
        scheduledTasks2.clear();
        dupe();
    }

    private void dupe() {
        int delayInt = (int)(delay.get() * 100);

        int lowestHotbarSlot = 0;
        int lowestHotbarDamage = 1000;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isOf(Items.TRIDENT)) {
                int currentHotbarDamage = mc.player.getInventory().getStack(i).getDamage();
                if (lowestHotbarDamage > currentHotbarDamage) {
                    lowestHotbarSlot = i;
                    lowestHotbarDamage = currentHotbarDamage;
                }
            }
        }

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        cancel = true;

        int finalLowestHotbarSlot = lowestHotbarSlot;
        scheduleTask(() -> {
            cancel = false;

            if (durabilityManagement.get() && finalLowestHotbarSlot != 0) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.SWAP, mc.player);
                if (dropTridents.get()) {
                    mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.THROW, mc.player);
                }
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 36 + finalLowestHotbarSlot, 0, SlotActionType.SWAP, mc.player);
            }

            mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 3, 0, SlotActionType.SWAP, mc.player);

            PlayerActionC2SPacket packet = new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN, 0);
            mc.getNetworkHandler().sendPacket(packet);

            if (dropTridents.get()) {
                mc.interactionManager.clickSlot(mc.player.currentScreenHandler.syncId, 44, 0, SlotActionType.THROW, mc.player);
            }

            cancel = true;
            scheduleTask2(() -> dupe(), delayInt);
        }, delayInt);
    }

    private void scheduleTask(Runnable task, long delayMillis) {
        scheduledTasks.add(new TimedTask(System.currentTimeMillis() + delayMillis, task));
    }

    private void scheduleTask2(Runnable task, long delayMillis) {
        scheduledTasks2.add(new TimedTask(System.currentTimeMillis() + delayMillis, task));
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        long currentTime = System.currentTimeMillis();

        scheduledTasks.removeIf(task -> {
            if (task.executeTime <= currentTime) {
                task.task.run();
                return true;
            }
            return false;
        });

        scheduledTasks2.removeIf(task -> {
            if (task.executeTime <= currentTime) {
                task.task.run();
                return true;
            }
            return false;
        });
    }

    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        toggle();
    }

    @EventHandler
    private void onScreenOpen(OpenScreenEvent event) {
        if (event.screen instanceof DisconnectedScreen) {
            toggle();
        }
    }
}
