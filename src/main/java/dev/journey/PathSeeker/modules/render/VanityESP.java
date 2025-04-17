package dev.journey.PathSeeker.modules.render;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BannerBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.WallBannerBlock;
import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.world.chunk.WorldChunk;

// refactored to VanityESP
public class VanityESP extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Boolean> highlightItemFrames = sgGeneral.add(new BoolSetting.Builder()
            .name("item-frames")
            .description("Highlights item frames.")
            .defaultValue(true)
            .build()
    );
    // Added mapOutline
    private final Setting<SettingColor> mapOutlineColor = sgColors.add(new ColorSetting.Builder()
            .name("map-outline-color")
            .description("Outline color for item frames containing maps.")
            .defaultValue(new SettingColor(255, 255, 0, 255))
            .build()
    );

    private final Setting<Boolean> highlightBanners = sgGeneral.add(new BoolSetting.Builder()
            .name("banners")
            .description("Highlights banners.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> highlightSigns = sgGeneral.add(new BoolSetting.Builder()
            .name("signs")
            .description("Highlights signs.")
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

    private final Setting<SettingColor> signColor = sgColors.add(new ColorSetting.Builder()
            .name("sign-color")
            .description("Fill color for signs.")
            .defaultValue(new SettingColor(255, 165, 0, 50))
            .build()
    );

    private final Setting<SettingColor> signOutlineColor = sgColors.add(new ColorSetting.Builder()
            .name("sign-outline-color")
            .description("Outline color for signs.")
            .defaultValue(new SettingColor(255, 165, 0, 255))
            .build()
    );

    public VanityESP() {
        super(PathSeeker.Render, "VanityESP", "Highlights maparts and banners");
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.world == null || mc.player == null) return;
        // fix for MapartESP depending on wall/floor angle
        if (highlightItemFrames.get()) {
            for (ItemFrameEntity frame : mc.world.getEntitiesByClass(ItemFrameEntity.class, mc.player.getBoundingBox().expand(64), e -> {
                String id = e.getHeldItemStack().getItem().getTranslationKey();
                return id.equals("item.minecraft.filled_map") || id.contains("shulker_box");
            })) {

                Box box;
                float pitch = frame.getPitch();
                if (pitch == 90 || pitch == -90) {
                    box = frame.getBoundingBox().expand(0.12, 0.01, 0.12);
                } else {
                    box = frame.getBoundingBox().expand(0.12, 0.12, 0.01);
                }

                Color fill = new Color(mapartColor.get());
                Color outline = new Color(mapOutlineColor.get());

                event.renderer.box(box, fill, outline, ShapeMode.Both, 0);
            }
        }
        // redid some of my bannerESP logic to fit all wall-mounts and also added my positional logic for standing banners
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

                        Color fill = new Color(bannerColor.get());
                        Color outline = new Color(bannerOutlineColor.get());

                        if (state.contains(WallBannerBlock.FACING)) {
                            Direction facing = state.get(WallBannerBlock.FACING);
                            double centerX = pos.getX() + 0.5;
                            double centerZ = pos.getZ() + 0.5;
                            double offset = 0.1;
                            double depth = 0.03;
                            double width = 0.45;
                            double y1 = pos.getY() - 0.95;
                            double y2 = pos.getY() + 0.85;

                            switch (facing) {
                                case NORTH:
                                    box = new Box(centerX - width, y1, pos.getZ() + 1 - offset - depth, centerX + width, y2, pos.getZ() + 1 - offset);
                                    break;
                                case SOUTH:
                                    box = new Box(centerX - width, y1, pos.getZ() + offset, centerX + width, y2, pos.getZ() + offset + depth);
                                    break;
                                case WEST:
                                    box = new Box(pos.getX() + 1 - offset - depth, y1, centerZ - width, pos.getX() + 1 - offset, y2, centerZ + width);
                                    break;
                                case EAST:
                                    box = new Box(pos.getX() + offset, y1, centerZ - width, pos.getX() + offset + depth, y2, centerZ + width);
                                    break;
                                default:
                                    continue;
                            }

                            event.renderer.box(box, fill, outline, ShapeMode.Both, 0);
                        } else if (state.contains(BannerBlock.ROTATION)) {
                            int rotation = state.get(BannerBlock.ROTATION);
                            double centerX = pos.getX() + 0.5;
                            double centerZ = pos.getZ() + 0.5;
                            double y1 = pos.getY();
                            double y2 = pos.getY() + 1.85;

                            if (rotation == 0 || rotation == 8) {
                                double width = 0.45;
                                double depth = 0.03;
                                box = new Box(centerX - width, y1, centerZ - depth, centerX + width, y2, centerZ + depth);
                            } else if (rotation == 4 || rotation == 12) {
                                double width = 0.03;
                                double depth = 0.45;
                                box = new Box(centerX - width, y1, centerZ - depth, centerX + width, y2, centerZ + depth);
                            } else {
                                double size = 0.3;
                                box = new Box(centerX - size, y1, centerZ - size, centerX + size, y2, centerZ + size);
                            }

                            event.renderer.box(box, fill, outline, ShapeMode.Both, 0);

                        }
                    }
                }
            }
        }
    }
}

