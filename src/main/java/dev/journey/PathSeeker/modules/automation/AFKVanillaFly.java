package dev.journey.PathSeeker.modules.automation;

import dev.journey.PathSeeker.PathSeeker;
import dev.journey.PathSeeker.modules.exploration.TrailFollower;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.settings.*;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;


public class AFKVanillaFly extends Module {
    private long lastRocketUse = 0;
    private boolean launched = false;
    private double yTarget = -1;
    private float targetPitch = 0;

    public AFKVanillaFly() {
        super(PathSeeker.Automation, "AFKVanillaFly", "Maintains a level Y-flight with fireworks and smooth pitch control.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public enum AutoFireworkMode {
        VELOCITY,
        TIMED_DELAY
    }

    private final Setting<AutoFireworkMode> fireworkMode = sgGeneral.add(new EnumSetting.Builder<AutoFireworkMode>()
            .name("Auto Firework Mode")
            .description("Choose between velocity-based or timed firework usage.")
            .defaultValue(AutoFireworkMode.VELOCITY)
            .build()
    );

    private final Setting<Integer> fireworkDelay = sgGeneral.add(new IntSetting.Builder()
            .name("Timed Delay (ms)")
            .description("How long to wait between fireworks when using Timed Delay.")
            .defaultValue(3000)
            .sliderRange(0, 6000)
            .visible(() -> fireworkMode.get() == AutoFireworkMode.TIMED_DELAY)
            .build()
    );

    private final Setting<Double> velocityThreshold = sgGeneral.add(new DoubleSetting.Builder()
            .name("Velocity Threshold")
            .description("Use a firework if your horizontal speed is below this value.")
            .defaultValue(0.7)
            .sliderRange(0.1, 2.0)
            .visible(() -> fireworkMode.get() == AutoFireworkMode.VELOCITY)
            .build()
    );

    @Override
    public void onActivate() {
        launched = false;
        yTarget = -1;

        if (mc.player == null || !mc.player.isFallFlying()) {
            info("You must be flying before enabling AFKVanillaFly.");
        }
    }

    // this method is now then default logic, it did not need to be called in TrailFollower
    public void tickFlyLogic() {
        if (mc.player == null) return;

        double currentY = mc.player.getY();

        if (mc.player.isFallFlying()) {
            if (yTarget == -1 || !launched) {
                yTarget = currentY;
                launched = true;
            }

            // will prevent from flying straight down into the ground - adjust y range if player moves vertical
            double yDiffFromLock = currentY - yTarget;
            if (Math.abs(yDiffFromLock) > 10.0) {
                yTarget = currentY; // reset the current y-level to maintain
                info("Y-lock reset due to altitude deviation.");
            }

            double yDiff = currentY - yTarget;

            if (Math.abs(yDiff) > 10.0) {
                targetPitch = (float) (-Math.atan2(yDiff, 100) * (180 / Math.PI));
            } else if (yDiff > 2.0) {
                targetPitch = 10f;
            } else if (yDiff < -2.0) {
                targetPitch = -10f;
            } else {
                targetPitch = 0f;
            }

            float currentPitch = mc.player.getPitch();
            float pitchDiff = targetPitch - currentPitch;
            mc.player.setPitch(currentPitch + pitchDiff * 0.1f);

            if (fireworkMode.get() == AutoFireworkMode.TIMED_DELAY) {
                if (System.currentTimeMillis() - lastRocketUse > fireworkDelay.get()) {
                    tryUseFirework();
                }
            } else if (fireworkMode.get() == AutoFireworkMode.VELOCITY) {
                double horizontalSpeed = Math.sqrt(Math.pow(mc.player.getVelocity().x, 2) + Math.pow(mc.player.getVelocity().z, 2));
                if (horizontalSpeed < velocityThreshold.get()) {
                    tryUseFirework();
                }
            }
            //need this for initiate flying check if on ground, will configure in the future (won't affect grim fly since not being used)
        } else {
            if (!launched) {
                mc.player.jump();
                launched = true;
            } else if (System.currentTimeMillis() - lastRocketUse > 1000) {
                tryUseFirework();
            }
            yTarget = -1;
        }
    }


    public void resetYLock() {
        yTarget = -1;
        launched = false;
    }


    @EventHandler
    private void onTick(TickEvent.Pre event) {
        tickFlyLogic();
    }


    private void tryUseFirework() {
        FindItemResult hotbar = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!hotbar.found()) {
            FindItemResult inv = InvUtils.find(Items.FIREWORK_ROCKET);
            if (inv.found()) {
                int hotbarSlot = findEmptyHotbarSlot();
                if (hotbarSlot != -1) {
                    InvUtils.move().from(inv.slot()).to(hotbarSlot);
                } else {
                    info("No empty hotbar slot available to move fireworks.");
                    return;
                }
            } else {
                info("No fireworks found in hotbar or inventory.");
                return;
            }
        }

        if (mc.player.getMainHandStack().isOf(Items.FIREWORK_ROCKET)) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        }

        lastRocketUse = System.currentTimeMillis();
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }
}
