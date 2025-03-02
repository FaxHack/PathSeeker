package dev.journey.PathSeeker.modules.utility;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.movement.elytrafly.ElytraFly;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;

import java.util.Optional;

import static dev.journey.PathSeeker.utils.PathSeekerUtil.firework;

public class Pitch40Util extends Module {
    private static final double DEFAULT_BOUND_GAP = 60.0;
    private static final double DEFAULT_VELOCITY_THRESHOLD = -0.05;
    private static final int DEFAULT_FIREWORK_COOLDOWN = 300;
    private static final double LOWER_BOUND_OFFSET = 10.0;
    private static final double Y_POSITION_OFFSET = 5.0;
    private static final float UPWARD_PITCH = -40.0f;

    // Settings groups
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgFirework = settings.createGroup("Firework");

    // General settings
    private final Setting<Double> boundGap = sgGeneral.add(new DoubleSetting.Builder()
            .name("bound-gap")
            .description("The gap between the upper and lower bounds")
            .defaultValue(DEFAULT_BOUND_GAP)
            .min(10.0)
            .sliderMax(100.0)
            .build()
    );

    // Firework settings
    private final Setting<Boolean> autoFirework = sgFirework.add(new BoolSetting.Builder()
            .name("auto-firework")
            .description("Automatically uses fireworks when velocity is too low")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> velocityThreshold = sgFirework.add(new DoubleSetting.Builder()
            .name("velocity-threshold")
            .description("Minimum velocity required for firework activation")
            .defaultValue(DEFAULT_VELOCITY_THRESHOLD)
            .visible(autoFirework::get)
            .build()
    );

    private final Setting<Integer> fireworkCooldownTicks = sgFirework.add(new IntSetting.Builder()
            .name("cooldown-ticks")
            .description("Ticks to wait between firework uses")
            .defaultValue(DEFAULT_FIREWORK_COOLDOWN)
            .min(20)
            .sliderMax(600)
            .visible(autoFirework::get)
            .build()
    );

    // Instance variables
    private final Module elytraFlyModule;
    private int fireworkCooldown;
    private boolean goingUp;
    private int elytraSwapSlot;

    public Pitch40Util() {
        super(PathSeeker.Utility, "Pitch40Util", "Maintains pitch 40 flight on 2b2t and dynamically adjusts bounds");
        this.elytraFlyModule = Modules.get().get(ElytraFly.class);
        reset();
    }

    private void reset() {
        fireworkCooldown = 0;
        goingUp = true;
        elytraSwapSlot = -1;
    }

    @Override
    public void onActivate() {
        reset();
    }

    @Override
    public void onDeactivate() {
        if (elytraFlyModule != null && elytraFlyModule.isActive()) {
            elytraFlyModule.toggle();
        }
        reset();
    }

    private Optional<Setting<Double>> getElytraFlySetting(String name) {
        try {
            return Optional.ofNullable((Setting<Double>) elytraFlyModule.settings.get(name));
        } catch (ClassCastException e) {
            error("Failed to get ElytraFly setting: " + name);
            return Optional.empty();
        }
    }

    private void resetBounds() {
        if (mc.player == null) return;

        double playerY = mc.player.getY();
        double baseHeight = playerY - Y_POSITION_OFFSET;

        getElytraFlySetting("pitch40-upper-bounds").ifPresent(upper -> upper.set(baseHeight));
        getElytraFlySetting("pitch40-lower-bounds").ifPresent(lower -> lower.set(baseHeight - boundGap.get()));
    }

    private void handleElytraSwap() {
        if (elytraSwapSlot != -1) {
            InvUtils.swap(elytraSwapSlot, true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            InvUtils.swapBack();
            elytraSwapSlot = -1;
        }
    }

    private boolean shouldUseFirework(PlayerEntity player, double upperBound) {
        return autoFirework.get()
                && fireworkCooldown == 0
                && player.getVelocity().y < velocityThreshold.get()
                && player.getY() < upperBound;
    }

    private void handleFirework() {
        int launchStatus = firework(mc);
        if (launchStatus >= 0) {
            fireworkCooldown = fireworkCooldownTicks.get();
            if (launchStatus != 200) {
                elytraSwapSlot = launchStatus;
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || !elytraFlyModule.isActive()) {
            if (mc.player != null && !mc.player.getAbilities().allowFlying) {
                elytraFlyModule.toggle();
                resetBounds();
            }
            return;
        }

        if (fireworkCooldown > 0) fireworkCooldown--;
        handleElytraSwap();

        // Get current bounds
        Optional<Setting<Double>> lowerBoundSetting = getElytraFlySetting("pitch40-lower-bounds");
        Optional<Setting<Double>> upperBoundSetting = getElytraFlySetting("pitch40-upper-bounds");

        if (!lowerBoundSetting.isPresent() || !upperBoundSetting.isPresent()) return;

        double lowerBound = lowerBoundSetting.get().get();
        double upperBound = upperBoundSetting.get().get();

        // Check if player fell below bounds
        if (mc.player.getY() <= lowerBound - LOWER_BOUND_OFFSET) {
            resetBounds();
            return;
        }

        // Handle upward flight
        if (Math.abs(mc.player.getPitch() - UPWARD_PITCH) < 0.1) {
            goingUp = true;
            if (shouldUseFirework(mc.player, upperBound)) {
                handleFirework();
            }
        }
        // Handle peak detection
        else if (goingUp && mc.player.getVelocity().y <= 0) {
            goingUp = false;
            resetBounds();
        }
    }
}