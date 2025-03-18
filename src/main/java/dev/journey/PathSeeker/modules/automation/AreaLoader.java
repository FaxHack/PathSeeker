package dev.journey.PathSeeker.modules.automation;

import baritone.api.BaritoneAPI;
import baritone.api.process.IElytraProcess;
import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.dimension.DimensionType;


public class AreaLoader extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final Setting<Integer> gapDistance = sgGeneral.add(new IntSetting.Builder()
            .name("gap-distance")
            .description("Gap in chunks between each line of the spiral.")
            .defaultValue(16)
            .min(5)
            .sliderMax(256)
            .build()
    );
    private final Setting<Integer> flightLevel = sgGeneral.add(new IntSetting.Builder()
            .name("flight-level")
            .description("What y-level to (roughly) fly at. It will still navigate around terrain as necessary.")
            .defaultValue(180)
            .min(80)
            .sliderMax(255)
            .build()
    );
    private final int[][] multipliers = {
            {0, 1}, {1, 0}, {0, -1}, {-1, 0}
    };
    private IElytraProcess efly;
    private int sideDistance = 0;
    private int multiIndex = 0;
    private BlockPos lastDest = null;
    private RegistryEntry<DimensionType> dimension;
    private boolean dimensionChanged = false;

    public AreaLoader() {
        super(PathSeeker.Hunting, "area-loader", "Spiral out from your position to load chunks in an area");
    }

    private void reset() {
        multiIndex = 0;
        sideDistance = 16 * gapDistance.get();
        efly = BaritoneAPI.getProvider().getPrimaryBaritone().getElytraProcess();
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (efly == null || !efly.isLoaded()) {
            this.toggle();
            return;
        }

        if (mc.world.getDimensionEntry() != dimension) {
            if (dimensionChanged) return;

            dimensionChanged = true;
            ChatUtils.sendMsg("area-loader", Text.literal("Dimension changed, pausing."));
            return;
        } else if (dimensionChanged) {
            dimensionChanged = false;
            ChatUtils.sendMsg("area-loader", Text.literal("Resuming."));
            efly.pathTo(lastDest);
        }

        var pos = mc.player.getBlockPos();
        var dest = efly.currentDestination();
        if (dest == null) return;

        var deltaX = Math.abs(pos.getX() - dest.getX());
        var deltaY = Math.abs(pos.getZ() - dest.getZ());

        if (deltaX < 60 && deltaY < 60) {
            lastDest = getNextDestination(dest);
            efly.pathTo(lastDest);
        }
    }

    private BlockPos getNextDestination(BlockPos start) {
        var multi = multipliers[multiIndex];
        multiIndex = (multiIndex + 1) % multipliers.length;

        var out = new BlockPos(
                start.getX() + (multi[0] * sideDistance),
                flightLevel.get(),
                start.getZ() + (multi[1] * sideDistance)
        );

        sideDistance += gapDistance.get() * 16;

        return out;
    }

    @Override
    public void onDeactivate() {
        if (efly == null || !efly.isActive()) return;
        BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute("stop");
    }


    @Override
    public void onActivate() {
        this.reset();
        dimension = mc.world.getDimensionEntry();
        lastDest = getNextDestination(mc.player.getBlockPos());
        efly.pathTo(lastDest);
    }
}