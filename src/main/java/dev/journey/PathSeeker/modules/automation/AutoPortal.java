package dev.journey.PathSeeker.modules.automation;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class AutoPortal extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final List<BlockPos> waitingForBreak = new ArrayList<>();


    private final Setting<Integer> placeDelay = sgGeneral.add(new IntSetting.Builder()
            .name("place-delay")
            .description("Ticks between each obsidian placement.")
            .defaultValue(1)
            .sliderRange(1, 20)
            .build()
    );

    private final Setting<Integer> blocksPerTick = sgGeneral.add(new IntSetting.Builder()
            .name("blocks-per-tick")
            .description("How many blocks to place each tick.")
            .defaultValue(1)
            .sliderRange(1, 5)
            .build()
    );

    private final Setting<Boolean> render = sgGeneral.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders the portal frame as it's being placed.")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgGeneral.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the box is rendered.")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgGeneral.add(new ColorSetting.Builder()
            .name("side-color")
            .defaultValue(new SettingColor(100, 100, 255, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgGeneral.add(new ColorSetting.Builder()
            .name("line-color")
            .defaultValue(new SettingColor(100, 100, 255, 255))
            .build()
    );

    private final List<BlockPos> portalBlocks = new ArrayList<>();
    private int delay = 0;
    private int index = 0;

    public AutoPortal() {
        super(PathSeeker.Automation, "AutoPortal", "For the Base Hunter who has places to be.");
    }

    @Override
    public void onActivate() {
        int obsidianCount = 0;
        for (int i = 0; i < 36; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) {
                obsidianCount += mc.player.getInventory().getStack(i).getCount();
            }
        }

        if (obsidianCount < 10) {
            error("Not enough obsidian to build the portal (need at least 10)!");
            toggle();
            return;
        }
        portalBlocks.clear();
        index = 0;
        delay = 0;

        // directly in front
        Direction forward = mc.player.getHorizontalFacing();
        Direction right = forward.rotateYClockwise();
        BlockPos base = mc.player.getBlockPos()
                .offset(forward, 2)
                .offset(right, -1);
        // duplicate check
        int obsidianCheck = 0;
        // check for these base blocks to shift portal up as needed
        List<Block> softBlocks = List.of(
                Blocks.SOUL_SAND, Blocks.SOUL_SOIL, Blocks.TALL_GRASS, Blocks.SNOW, Blocks.FERN
        );

        BlockPos finalBase = base;
        boolean shiftUp = List.of(
                base.offset(right, 1),
                base.offset(right, 2)
        ).stream().anyMatch(pos -> softBlocks.contains(mc.world.getBlockState(pos).getBlock()));

        if (shiftUp) finalBase = base.up();
        // redefined finalBase to include block checks in the future
        List<BlockPos> checkPositions = List.of(
                finalBase.offset(right, 1), finalBase.offset(right, 2),
                finalBase.offset(right, 0).up(1), finalBase.offset(right, 0).up(2), finalBase.offset(right, 0).up(3),
                finalBase.offset(right, 3).up(1), finalBase.offset(right, 3).up(2), finalBase.offset(right, 3).up(3),
                finalBase.offset(right, 1).up(4), finalBase.offset(right, 2).up(4)
        );
        // block obstruction check (temporary until fixed) - added some common blocks to skip
        boolean obstructed = checkPositions.stream().anyMatch(pos -> {
            Block block = mc.world.getBlockState(pos).getBlock();
            return !mc.world.getBlockState(pos).isAir()
                    && block != Blocks.SOUL_SAND
                    && block != Blocks.SOUL_SOIL
                    && block != Blocks.TALL_GRASS
                    && block != Blocks.SNOW
                    && block != Blocks.FERN;
        });
        // will remove later once we fix portal block obstruction
        if (obstructed) {
            error("Portal area obstructed. Move and try again.");
            portalBlocks.clear();
            portalBlocks.addAll(checkPositions); // just render blocked frame
            index = checkPositions.size(); // skip building
            return;
        }

        for (BlockPos checkPos : checkPositions) {
            if (mc.world.getBlockState(checkPos).getBlock().asItem() == Items.OBSIDIAN) {
                obsidianCheck++;
            }
        }

        if (obsidianCheck >= checkPositions.size()) {
            error("A portal already exists here!");
            toggle();
            return;
        }
        // refactored base. to finalBase. after defining blockstate checks
        portalBlocks.add(finalBase.offset(right, 1));
        portalBlocks.add(finalBase.offset(right, 2));

        for (int i = 1; i <= 3; i++) {
            portalBlocks.add(finalBase.offset(right, 0).up(i));
        }

        for (int i = 1; i <= 3; i++) {
            portalBlocks.add(finalBase.offset(right, 3).up(i));
        }

        portalBlocks.add(finalBase.offset(right, 1).up(4));
        portalBlocks.add(finalBase.offset(right, 2).up(4));

        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == Items.OBSIDIAN) {
                mc.player.getInventory().selectedSlot = i;
                break;
            }
        }
    }

    @Override
    public void onDeactivate() {
        portalBlocks.clear();
        index = 0;
        delay = 0;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null || mc.world == null) return;
        if (!(mc.player.getMainHandStack().getItem() instanceof BlockItem blockItem)) return;
        if (blockItem.getBlock().asItem() != Items.OBSIDIAN) return;

        if (index >= portalBlocks.size()) {
            toggle();
            return;
        }

        delay++;
        if (delay < placeDelay.get()) return;
        for (int i = 0; i < blocksPerTick.get() && index < portalBlocks.size(); i++, index++) {
            BlockPos pos = portalBlocks.get(index);
            // prevent faulty portal placements (not being used due to boolean obstruction check above, but will fix in the future)
            Block block = mc.world.getBlockState(pos).getBlock();
            if (!mc.world.getBlockState(pos).isAir() && block != Blocks.SOUL_SAND && block != Blocks.SOUL_SOIL) {
                if (!waitingForBreak.contains(pos) && mc.world.getBlockState(pos).getBlock().asItem() != Items.OBSIDIAN) {
                    if (mc.interactionManager != null) {
                        mc.interactionManager.attackBlock(pos, Direction.UP);
                        mc.player.swingHand(Hand.MAIN_HAND);
                        waitingForBreak.add(pos);
                    }
                }
                index--; // loop again to finish placing
                return;
            }

            waitingForBreak.remove(pos);

            BlockHitResult bhr = new BlockHitResult(Vec3d.ofCenter(pos), Direction.UP, pos, false);

            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            mc.player.networkHandler.sendPacket(new PlayerInteractBlockC2SPacket(
                    Hand.OFF_HAND, bhr, mc.player.currentScreenHandler.getRevision() + 2));
            mc.player.networkHandler.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ORIGIN, Direction.DOWN));
            mc.player.swingHand(Hand.MAIN_HAND);
        }
        delay = 0;

        if (index >= portalBlocks.size()) {
            // auto light
            for (int i = 0; i < 9; i++) {
                if (mc.player.getInventory().getStack(i).getItem() == Items.FLINT_AND_STEEL) {
                    mc.player.getInventory().selectedSlot = i;

                    BlockPos firePos = portalBlocks.get(0).up();
                    BlockHitResult fireHit = new BlockHitResult(Vec3d.ofCenter(firePos), Direction.UP, firePos, false);

                    mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, fireHit);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    break;
                }
            }
            info("Portal complete. AutoPortal disabled.");
            toggle();
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;
        for (int i = index; i < portalBlocks.size(); i++) {
            BlockPos pos = portalBlocks.get(i);
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }
    }
}
