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
    private long lastRocketUse = 0;
    private boolean launched = false;
    private boolean manuallyEnabled = false;
    private double yTarget = -1;
    private float targetPitch = 0;

    public AFKVanillaFly() {
        super(PathSeeker.Automation, "AFKVanillaFly", "Maintains a level Y-flight with fireworks and smooth pitch control.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

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

    public AFKVanillaFly() {
        super(PathSeeker.Automation, "AFKVanillaFly", "Maintains a level Y-flight with fireworks and smooth pitch control.");
    }

    @Override
    public void onActivate() {
        manuallyEnabled = true; // Track manual activation
        launched = false;
        yTarget = -1;

        Firework firework = Modules.get().get(Firework.class);
        if (!firework.isActive()) firework.toggle();

        if (mc.player == null || !mc.player.isFallFlying()) {
            info("You must be flying before enabling AFKVanillaFly.");
        }
    }

    @Override
    public void onDeactivate() {
        manuallyEnabled = false; // Reset manual flag
        Firework firework = Modules.get().get(Firework.class);
        if (firework.isActive()) firework.toggle();
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

            float targetPitch = 0;
            if (Math.abs(yDiff) > 10.0) {
                targetPitch = (float) (-Math.atan2(yDiff, 100) * (180 / Math.PI));
            } else if (yDiff > 2.0) {
                targetPitch = 10f;
            } else if (yDiff < -2.0) {
                targetPitch = -10f;
            } else {
                targetPitch = 0f;
            }

            // pitch fix
            targetPitch = Math.max(-30f, Math.min(30f, targetPitch));

            float currentPitch = mc.player.getPitch();
            mc.player.setPitch(currentPitch + (targetPitch - currentPitch) * 0.1f);

            Firework firework = Modules.get().get(Firework.class);
            if (!firework.isActive()) firework.toggle();

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
            }
            yTarget = -1;
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        TrailFollower trailFollower = Modules.get().get(TrailFollower.class);
        Firework firework = Modules.get().get(Firework.class);

        boolean shouldAutoEnable = trailFollower.isActive()
                && trailFollower.flightMode.get() == TrailFollower.FlightMode.VANILLA
                && mc.world != null
                && mc.world.getRegistryKey().getValue().getPath().equals("overworld");
        if (shouldAutoEnable && !this.isActive() && !manuallyEnabled) {
            this.toggle(); // Auto enable
            if (!firework.isActive()) firework.toggle();
        }
        if (!shouldAutoEnable && !manuallyEnabled && this.isActive()) {
            this.toggle(); // Auto disable
            if (firework.isActive()) firework.toggle();
        }
        if (this.isActive()) tickFlyLogic();
    }
}
    public enum AutoFireworkMode {
        VELOCITY,
        TIMED_DELAY
    }
}
