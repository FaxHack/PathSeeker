package dev.journey.PathSeeker.modules.render;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

public class CollectorESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Boolean> highlightItemFrames = sgGeneral.add(new BoolSetting.Builder()
            .name("item-frames")
            .description("Highlights item frames.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> highlightBanners = sgGeneral.add(new BoolSetting.Builder()
            .name("banners")
            .description("Highlights banners.")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> mapartColor = sgColors.add(new ColorSetting.Builder()
            .name("mapart-frame-color")
            .description("Color for item frames containing maps.")
            .defaultValue(new SettingColor(255, 255, 0, 50))
            .build()
    );

    private final Setting<SettingColor> bannerColor = sgColors.add(new ColorSetting.Builder()
            .name("banner-color")
            .description("Color for banner ESP.")
            .defaultValue(new SettingColor(255, 0, 0, 50))
            .build()
    );

    private final Setting<SettingColor> bannerOutlineColor = sgColors.add(new ColorSetting.Builder()
            .name("banner-outline")
            .description("Outline color for banners.")
            .defaultValue(new SettingColor(255, 0, 0, 255))  // Red outline
            .build()
    );

    public CollectorESP() {
        super(PathSeeker.Render, "CollectorESP", "Highlights maparts and banners");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;

        if (highlightItemFrames.get()) {
            for (ItemFrameEntity frame : mc.world.getEntitiesByClass(ItemFrameEntity.class, mc.player.getBoundingBox().expand(64), e ->
                    e.getHeldItemStack().getItem().getTranslationKey().equals("item.minecraft.filled_map"))) {

                Box box = frame.getBoundingBox();
                Color fill = new Color(mapartColor.get());
                Color outline = new Color(mapartColor.get()).a(255);
                event.renderer.box(box, fill, outline, ShapeMode.Both, 0);
            }
        }

        if (highlightBanners.get()) {
            int radius = 8;
            BlockPos playerPos = mc.player.getBlockPos();

            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    WorldChunk chunk = mc.world.getChunk(playerPos.getX() / 16 + dx, playerPos.getZ() / 16 + dz);
                    if (chunk == null) continue;

                    for (BlockEntity be : chunk.getBlockEntities().values()) {
                        if (!(be instanceof BannerBlockEntity banner)) continue;

                        BlockPos pos = banner.getPos();
                        BlockState state = mc.world.getBlockState(pos);
                        Box box;

                        if (state.contains(WallBannerBlock.FACING)) {
                            Direction facing = state.get(WallBannerBlock.FACING);
                            // will angle shaderbox up from the bottom later to match wall mounted banners
                            switch (facing) {
                                case NORTH:
                                    box = new Box(pos.getX() + 0.05, pos.getY() - 0.95, pos.getZ() + 0.93,
                                            pos.getX() + 0.95, pos.getY() + 0.85, pos.getZ() + 0.995);
                                    break;
                                case SOUTH:
                                    box = new Box(pos.getX() + 0.05, pos.getY() - 0.95, pos.getZ() + 0.005,
                                            pos.getX() + 0.95, pos.getY() + 0.85, pos.getZ() + 0.07);
                                    break;
                                case WEST:
                                    box = new Box(pos.getX() + 0.93, pos.getY() - 0.95, pos.getZ() + 0.05,
                                            pos.getX() + 0.995, pos.getY() + 0.85, pos.getZ() + 0.95);
                                    break;
                                case EAST:
                                    box = new Box(pos.getX() + 0.005, pos.getY() - 0.95, pos.getZ() + 0.05,
                                            pos.getX() + 0.07, pos.getY() + 0.85, pos.getZ() + 0.95);
                                    break;
                                default:
                                    continue;
                            }
                        } else {
                            // will do standing banners based on direction later
                            box = new Box(pos.getX() + 0.25, pos.getY(), pos.getZ() + 0.1,
                                    pos.getX() + 0.75, pos.getY() + 1.85, pos.getZ() + 0.9);
                        }

                        Color fill = new Color(bannerColor.get());
                        Color outline = new Color(bannerOutlineColor.get());
                        event.renderer.box(box, fill, outline, ShapeMode.Both, 0);
                    }
                }
            }
        }
    }
}
