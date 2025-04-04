package dev.journey.PathSeeker.modules.automation;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.player.PlayerUtils; // may use for inven
import meteordevelopment.orbit.EventHandler;
import net.minecraft.item.Items;
import net.minecraft.item.BlockItem;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

public class AutoPortal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRange = settings.createGroup("Range");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
            .name("place-delay-ms")
            .description("placement delay in case of lag")
            .defaultValue(0)
            .min(0)
            .sliderMax(1000)
            .build());

    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a block overlay where the block will be placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered.")
            .defaultValue(ShapeMode.Both)
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("The color of the sides of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 10))
            .visible(render::get)
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("The color of the lines of the blocks being rendered.")
            .defaultValue(new SettingColor(204, 0, 0, 255))
            .visible(render::get)
            .build()
    );

    private final Setting<Boolean> customRange = sgRange.add(new BoolSetting.Builder()
            .name("custom-range")
            .description("Use custom range for air place.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> range = sgRange.add(new DoubleSetting.Builder()
            .name("range")
            .description("Custom range to place at.")
            .visible(customRange::get)
            .defaultValue(5)
            .min(0)
            .sliderMax(5.5)
            .build()
    );

    private BlockPos base;
    private int stage = -1;
    private long lastPlaceTime = 0;
    private boolean waitingForJumpPeak = false;
    private HitResult hitResult;
    private int placeDelay = 0;

    public AutoPortal() {
        super(PathSeeker.Automation, "AutoPortal", "For the Base Hunter who has places to be.");
    }

    @Override
    public void onActivate() {
        mc.options.useKey.setPressed(false);
        if (mc.player != null) {
            base = mc.player.getBlockPos().up();
            lastPlaceTime = 0;
            waitingForJumpPeak = false;
            stage = -1;
            placeDelay = 0;
        }
    }

    @Override
    public void onDeactivate() {
        waitingForJumpPeak = false;
        mc.options.useKey.setPressed(false);
        info("Toggled " + name + " §coff§r.");
    }


    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null || base == null) return;

        FindItemResult obsidian = InvUtils.findInHotbar(Items.OBSIDIAN);
        if (!obsidian.found()) {
            error("No obsidian in hotbar!");
            mc.options.useKey.setPressed(false);
            this.toggle();
            return;
        }

        mc.player.getInventory().selectedSlot = obsidian.slot();
        long now = System.currentTimeMillis();
        if (now < lastPlaceTime) return;

        placeDelay++;
        double r = customRange.get() ? range.get() : mc.player.getBlockInteractionRange();
        hitResult = mc.getCameraEntity().raycast(r, 0, false);

        if (stage == 11) {
            toggle();
            return;
        }

        if (stage == 5) {
            BlockPos target = getBlockForStage(stage);
            handlePreciseJumpPlacement(now, target, stage + 1);
            return;
        }


        if (stage == -1 || stage == 6) {
            BlockPos target = getBlockForStage(stage);
            handleJumpPlacement(now, target, stage + 1);
            return;
        }

        if (stage == 9 || stage == 10) {
            BlockPos target = getBlockForStage(stage);

            Vec3d eyes = mc.player.getEyePos();
            Vec3d hit = Vec3d.ofCenter(target).add(0, -0.5, 0);
            Vec3d diff = hit.subtract(eyes);
            double length = Math.sqrt(diff.lengthSquared());
            double pitch = -Math.toDegrees(Math.asin(diff.y / length));
            double yaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;

            mc.player.setYaw((float) yaw);
            mc.player.setPitch((float) pitch);

            if (placeBlockWithDirection(target, Direction.UP)) {
                mc.player.swingHand(Hand.MAIN_HAND);
                lastPlaceTime = System.currentTimeMillis() + (delay.get() / 2);
                stage++;
            }
            return;
        }

        BlockPos target = getBlockForStage(stage);
        if (target == null) {
            error("Invalid stage: " + stage);
            toggle();
            return;
        }

        Vec3d eyes = mc.player.getEyePos();
        Vec3d hit = Vec3d.ofCenter(target);
        Vec3d diff = hit.subtract(eyes);
        double length = Math.sqrt(diff.lengthSquared());
        double pitch = -Math.toDegrees(Math.asin(diff.y / length));
        double yaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);

        if (hitResult instanceof BlockHitResult blockHitResult
                && (mc.player.getMainHandStack().getItem() instanceof BlockItem
                || mc.player.getMainHandStack().getItem() instanceof SpawnEggItem)) {

            if (placeDelay >= delay.get()) {
                if (mc.options.useKey.isPressed()) {
                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0,0,0), Direction.DOWN));

                    mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(Hand.OFF_HAND, blockHitResult, mc.player.currentScreenHandler.getRevision() + 2));

                    mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, new BlockPos(0,0,0), Direction.DOWN));

                    mc.player.swingHand(Hand.MAIN_HAND);
                    placeDelay = 0;
                }
            }
        }

        if (mc.world.getBlockState(target).isAir()) {
            if (placeBlock(target)) {
                mc.player.swingHand(Hand.MAIN_HAND);
                lastPlaceTime = System.currentTimeMillis() + delay.get();
                stage++;
            }
        }
        else
        {
            stage++;
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        // grim render
        if (!(hitResult instanceof BlockHitResult blockHitResult)
                || !mc.world.getBlockState(blockHitResult.getBlockPos()).isReplaceable()
                || !(mc.player.getMainHandStack().getItem() instanceof BlockItem)
                && !(mc.player.getMainHandStack().getItem() instanceof SpawnEggItem)
                || !render.get()) return;

        event.renderer.box(blockHitResult.getBlockPos(), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
    }

    private void handleJumpPlacement(long now, BlockPos target, int nextStage) {
        if (!waitingForJumpPeak) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
                waitingForJumpPeak = true;
                lastPlaceTime = now;
            }
            return;
        }

        Vec3d eyes = mc.player.getEyePos();
        Vec3d hit = Vec3d.ofCenter(target);
        Vec3d diff = hit.subtract(eyes);
        double length = Math.sqrt(diff.lengthSquared());
        double pitch = -Math.toDegrees(Math.asin(diff.y / length));
        double yaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);

        if (!mc.player.isOnGround()) {
            if (mc.player.getVelocity().y <= 0) {
                mc.options.useKey.setPressed(true);
            }
        } else {
            mc.options.useKey.setPressed(false);

            if (!mc.world.getBlockState(target).isAir()) {
                stage = nextStage;
                waitingForJumpPeak = false;
                lastPlaceTime = System.currentTimeMillis() + delay.get();
            } else {
                waitingForJumpPeak = false;
            }
        }
    }

    private void handlePreciseJumpPlacement(long now, BlockPos target, int nextStage) {
        if (!waitingForJumpPeak) {
            if (mc.player.isOnGround()) {
                mc.player.jump();
                waitingForJumpPeak = true;
                lastPlaceTime = now;
            }
            return;
        }

        Vec3d eyes = mc.player.getEyePos();
        Vec3d hit = Vec3d.ofCenter(target);
        Vec3d diff = hit.subtract(eyes);
        double length = Math.sqrt(diff.lengthSquared());
        double pitch = -Math.toDegrees(Math.asin(diff.y / length));
        double yaw = Math.toDegrees(Math.atan2(diff.z, diff.x)) - 90;

        mc.player.setYaw((float) yaw);
        mc.player.setPitch((float) pitch);

        if (!mc.player.isOnGround()) {
            if (mc.player.getVelocity().y <= 0 && mc.player.getVelocity().y > -0.1) {
                // Only click right at the peak of the jump for maximum precision
                mc.options.useKey.setPressed(true);
            }
            else {
                mc.options.useKey.setPressed(false);
            }
        }
        else {
            mc.options.useKey.setPressed(false);

            if (!mc.world.getBlockState(target).isAir()) {
                stage = nextStage;
                waitingForJumpPeak = false;
                lastPlaceTime = System.currentTimeMillis() + delay.get();
            }
            else {
                waitingForJumpPeak = false;
            }
        }
    }

    private BlockPos getBlockForStage(int stage) {
        if (base == null) return null;

        return switch (stage) {
            case -1 -> base.down();
            // obsidian placement
            case 0 -> base.add(1, -1, 0);
            case 1 -> base.add(2, -1, 0);
            case 2 -> base.add(3, -1, 0);
            case 3 -> base.add(3, 0, 0);
            case 4 -> base.add(3, 1, 0);
            case 5 -> base.add(0, 1, 0);
            case 6 -> base.add(0, 2, 0);
            case 7 -> base.add(3, 2, 0);
            case 8 -> base.add(3, 3, 0);
            case 9 -> base.add(2, 3, 0);
            case 10 -> base.add(1, 3, 0);
            default -> null;
        };
    }

    private boolean placeBlock(BlockPos pos) {
        for (Direction dir : Direction.values()) {
            BlockPos adjacent = pos.offset(dir);
            if (!mc.world.getBlockState(adjacent).isAir()) {
                BlockHitResult hit = new BlockHitResult(
                        Vec3d.ofCenter(pos.offset(dir)),
                        dir.getOpposite(),
                        pos.offset(dir),
                        false
                );
                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
                return true;
            }
        }

        BlockPos above = pos.up();
        if (!mc.world.getBlockState(above).isAir()) {
            Vec3d hitVec = Vec3d.ofCenter(pos).add(0, 0.5, 0);
            BlockHitResult hit = new BlockHitResult(hitVec, Direction.DOWN, pos, false);
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            return true;
        }

        return false;
    }

    private boolean placeBlockWithDirection(BlockPos pos, Direction dir) {
        BlockPos adjacent = pos.offset(dir);
        if (!mc.world.getBlockState(adjacent).isAir()) {
            BlockHitResult hit = new BlockHitResult(
                    Vec3d.ofCenter(pos.offset(dir)),
                    dir.getOpposite(),
                    pos.offset(dir),
                    false
            );
            mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, hit);
            return true;
        }
        return placeBlock(pos);
    }
}