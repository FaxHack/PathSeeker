package dev.journey.PathSeeker.modules.utility;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.RenderUtils;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.vehicle.AbstractMinecartEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class StackedMinecartsDetector extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // General Settings
    private final Setting<Integer> detectionRadius = sgGeneral.add(new IntSetting.Builder()
            .name("detection-radius")
            .description("Maximum distance between minecarts to consider them stacked")
            .defaultValue(1)
            .min(0)
            .sliderMax(5)
            .build()
    );

    private final Setting<Integer> minStackSize = sgGeneral.add(new IntSetting.Builder()
            .name("minimum-stack-size")
            .description("Minimum number of minecarts needed to trigger detection")
            .defaultValue(2)
            .min(2)
            .sliderRange(2, 10)
            .build()
    );

    // Render Settings
    private final Setting<Boolean> render = sgRender.add(new BoolSetting.Builder()
            .name("render")
            .description("Renders a box around stacked minecarts")
            .defaultValue(true)
            .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("How the shapes are rendered")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> boxColor = sgRender.add(new ColorSetting.Builder()
            .name("box-color")
            .description("Color of the rendered box")
            .defaultValue(new SettingColor(255, 0, 0, 100))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("Color of the rendered lines")
            .defaultValue(new SettingColor(255, 0, 0, 255))
            .build()
    );

    private final Setting<Boolean> tracers = sgRender.add(new BoolSetting.Builder()
            .name("tracers")
            .description("Render tracers to stacked minecarts")
            .defaultValue(true)
            .build()
    );

    private final Map<BlockPos, Integer> stackedPositions = new HashMap<>();
    private final Set<BlockPos> toRemove = new HashSet<>();

    public StackedMinecartsDetector() {
        super(PathSeeker.Utility, "stacked-minecarts", "Detects stacked minecarts in the world.");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.world == null) return;

        stackedPositions.clear();
        Map<BlockPos, List<AbstractMinecartEntity>> minecartMap = new HashMap<>();

        // Group minecarts by their block position
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof AbstractMinecartEntity minecart) {
                BlockPos pos = minecart.getBlockPos();
                minecartMap.computeIfAbsent(pos, k -> new ArrayList<>()).add(minecart);
            }
        }

        // Check for stacks
        for (Map.Entry<BlockPos, List<AbstractMinecartEntity>> entry : minecartMap.entrySet()) {
            if (entry.getValue().size() >= minStackSize.get()) {
                stackedPositions.put(entry.getKey(), entry.getValue().size());
            }
        }
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (!render.get()) return;

        for (Map.Entry<BlockPos, Integer> entry : stackedPositions.entrySet()) {
            BlockPos pos = entry.getKey();
            int count = entry.getValue();

            Box box = new Box(
                    pos.getX() + 0.1, pos.getY(), pos.getZ() + 0.1,
                    pos.getX() + 0.9, pos.getY() + (0.2 * count), pos.getZ() + 0.9
            );

            // Render box
            event.renderer.box(
                    box.minX, box.minY, box.minZ,
                    box.maxX, box.maxY, box.maxZ,
                    boxColor.get(), lineColor.get(),
                    shapeMode.get(),
                    0
            );

            // Render tracers
            if (tracers.get()) {
                Vec3d center = RenderUtils.center;
                Vec3d minecartPos = new Vec3d(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5);
                event.renderer.line(center.x, center.y, center.z,
                        minecartPos.x, minecartPos.y, minecartPos.z,
                        lineColor.get()
                );
            }
        }
    }

    public Map<BlockPos, Integer> getStackedPositions() {
        return Collections.unmodifiableMap(stackedPositions);
    }
}