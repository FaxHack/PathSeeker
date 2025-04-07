package dev.journey.PathSeeker.modules.automation;

import dev.journey.PathSeeker.PathSeeker;
import dev.journey.PathSeeker.modules.exploration.TrailFollower;
import dev.journey.PathSeeker.modules.utility.Firework;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;

public class AFKVanillaFly extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<AutoFireworkMode> fireworkMode = sgGeneral.add(new EnumSetting.Builder<AutoFireworkMode>()
            .name("Auto Firework Mode")
            .description("Choose between velocity-based or timed firework usage.")
            .defaultValue(AutoFireworkMode.VELOCITY)
            .build()
    );
    private final Setting<Integer> fireworkDelay = sgGeneral.add(new IntSetting.Builder()
            .name("Timed Delay (ms)")
            .description("Delay between fireworks in TIMED mode.")
            .defaultValue(3000)
            .sliderRange(0, 6000)
            .visible(() -> fireworkMode.get() == AutoFireworkMode.TIMED_DELAY)
            .build()
    );
    private final Setting<Double> velocityThreshold = sgGeneral.add(new DoubleSetting.Builder()
            .name("Velocity Threshold")
            .description("Use a firework if speed is below this value in VELOCITY mode.")
            .defaultValue(0.7)
            .sliderRange(0.1, 2.0)
            .visible(() -> fireworkMode.get() == AutoFireworkMode.VELOCITY)
            .build()
    );
    private long lastRocketUse = 0;
    private boolean launched = false;
    private double yTarget = -1;
    private float targetPitch = 0;

    public AFKVanillaFly() {
        super(PathSeeker.Automation, "AFKVanillaFly", "Maintains a level Y-flight with fireworks and smooth pitch control.");
    }

    @Override
    public void onActivate() {
        launched = false;
        yTarget = -1;
        // fix for firework hanging
        Firework firework = Modules.get().get(Firework.class);
        if (firework.isActive()) {
            firework.toggle();
            info("Disabled Firework module to prevent conflict.");
        }
        if (mc.player == null || !mc.player.isFallFlying()) {
            info("You must be flying before enabling AFKVanillaFly.");
            }
        }

    public void tickFlyLogic() {
        if (mc.player == null) return;

        double currentY = mc.player.getY();

        if (mc.player.isFallFlying()) {
            if (yTarget == -1 || !launched) {
                yTarget = currentY;
                launched = true;
            }

            double yDiff = currentY - yTarget;

            if (Math.abs(yDiff) > 10.0) {
                yTarget = currentY;
                info("Y-lock reset due to altitude deviation.");
            }

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
            mc.player.setPitch(currentPitch + (targetPitch - currentPitch) * 0.1f);

            if (fireworkMode.get() == AutoFireworkMode.TIMED_DELAY) {
                if (System.currentTimeMillis() - lastRocketUse > fireworkDelay.get()) {
                    tryUseFirework();
                }
            } else if (fireworkMode.get() == AutoFireworkMode.VELOCITY) {
                double speed = Math.sqrt(Math.pow(mc.player.getVelocity().x, 2) + Math.pow(mc.player.getVelocity().z, 2));
                if (speed < velocityThreshold.get()) {
                    tryUseFirework();
                }
            }
        } else {
            // Just jumped or crashed out of flight
            TrailFollower trailFollower = Modules.get().get(TrailFollower.class);
            if (trailFollower.isActive() && trailFollower.flightMode.get() == TrailFollower.FlightMode.VANILLA) {
                trailFollower.toggle();
                info("You stopped flying. TrailFollower was disabled.");
            }
            if (!launched) {
                mc.player.jump();
                launched = true;
            } else if (System.currentTimeMillis() - lastRocketUse > 1000) {
                tryUseFirework();
            }
            yTarget = -1;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        tickFlyLogic();
    }


    public void resetYLock() {
        yTarget = -1;
        launched = false;
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
                    info("No empty hotbar slot available.");
                    return;
                }
            } else {
                info("No fireworks in inventory.");
                return;
            }
        }

        // Use false so elytra isn't required
        dev.journey.PathSeeker.utils.PathSeekerUtil.firework(mc, false);
        mc.player.swingHand(Hand.MAIN_HAND);
        lastRocketUse = System.currentTimeMillis();
    }

    private int findEmptyHotbarSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).isEmpty()) return i;
        }
        return -1;
    }

    public enum AutoFireworkMode {
        VELOCITY,
        TIMED_DELAY
    }
}
