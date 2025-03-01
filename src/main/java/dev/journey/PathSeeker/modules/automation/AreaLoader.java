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
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.math.BlockPos;


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
        if (efly == null || !efly.isActive()) {
            this.toggle();
            return;
        }

        var pos = mc.player.getBlockPos();
        var dest = efly.currentDestination();

        var deltaX = Math.abs(pos.getX() - dest.getX());
        var deltaY = Math.abs(pos.getZ() - dest.getZ());

        if (deltaX < 50 && deltaY < 50) {
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
        if (PlayerUtils.getDimension() == Dimension.Nether) {
            ChatUtils.errorPrefix("area-loader", "This module is not supported in the nether");
            this.toggle();
            return;
        }

        this.reset();

        lastDest = getNextDestination(mc.player.getBlockPos());
        efly.pathTo(lastDest);
    }

}