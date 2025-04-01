package dev.journey.PathSeeker.modules.automation;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.utils.player.Rotations; // may possibly use
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity; // will use
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;



public class TridentAura extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> onlyHostile = sgGeneral.add(new BoolSetting.Builder()
            .name("Hostile Only Debug")
            .description("only targets hostile mobs.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
            .name("Range")
            .description("max range")
            .defaultValue(50)
            .min(1)
            .sliderMax(100)
            .build()
    );

    public TridentAura() {
        super(PathSeeker.Automation, "Trident Aura", "For trident spam");
    }

    private Entity currentTarget = null;

    @Override
    public void onDeactivate() {
        currentTarget = null;
    }

    private int cooldownTicks = 0;
    private boolean justSwitched = false;

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        if (!mc.options.useKey.isPressed()) {
            currentTarget = null;
            cooldownTicks = 0;
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }


        double closestDistance = range.get();
        currentTarget = null;

        for (Entity entity : mc.world.getEntities()) {
            if (!(entity instanceof LivingEntity)) continue;
            if (entity == mc.player) continue;
            if (onlyHostile.get() && !(entity instanceof HostileEntity)) continue;

            double distance = mc.player.distanceTo(entity);
            if (distance < closestDistance) {
                closestDistance = distance;
                currentTarget = entity;
            }
        }

        if (currentTarget == null) return;

        // aimbot math
        double dx = currentTarget.getX() - mc.player.getX();
        double dz = currentTarget.getZ() - mc.player.getZ();
        double dy = (currentTarget.getY() + currentTarget.getHeight() / 2.0)
                - (mc.player.getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double horizontalDist = Math.sqrt(dx * dx + dz * dz);
        double dropComp = 0.05 * Math.pow(horizontalDist / 2.5, 2);
        dy += dropComp;

        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90f;
        float targetPitch = (float) -Math.toDegrees(Math.atan2(dy, horizontalDist));
        float currentYaw = mc.player.getYaw();
        float currentPitch = mc.player.getPitch();

        float smoothYaw = currentYaw + (targetYaw - currentYaw) * 0.3f;
        float smoothPitch = currentPitch + (targetPitch - currentPitch) * 0.3f;

        mc.player.setYaw(smoothYaw);
        mc.player.setPitch(smoothPitch);

        ItemStack stack = mc.player.getMainHandStack();
        if (!stack.isOf(Items.TRIDENT)) {

            for (int i = 0; i < 9; i++) {
                ItemStack item = mc.player.getInventory().getStack(i);
                if (item.isOf(Items.TRIDENT)) {
                    mc.player.getInventory().selectedSlot = i;
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    return;
                }
            }
            // pull inventory
            for (int i = 9; i < mc.player.getInventory().size(); i++) {
                ItemStack invStack = mc.player.getInventory().getStack(i);
                if (invStack.isOf(Items.TRIDENT)) {
                    for (int j = 0; j < 9; j++) {
                        if (mc.player.getInventory().getStack(j).isEmpty()) {
                            InvUtils.move().from(i).to(j);
                            mc.player.getInventory().selectedSlot = j;
                            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                            return;
                        }
                    }
                }
            }
            return;
        }

        if (!mc.player.isUsingItem()) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
        } else if (mc.player.getItemUseTime() >= 10) {
            mc.interactionManager.stopUsingItem(mc.player);
            cooldownTicks = 2;
        }
    }
}