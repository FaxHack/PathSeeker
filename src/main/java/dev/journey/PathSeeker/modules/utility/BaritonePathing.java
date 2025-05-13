package dev.journey.PathSeeker.modules.utility;

import baritone.api.BaritoneAPI;
import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.KeybindSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;

public class BaritonePathing extends Module {
    private final SettingGroup sgSettings = settings.getDefaultGroup();

    private final Setting<Keybind> flyKeybind = sgSettings.add(new KeybindSetting.Builder()
            .name("Baritone Path Macro")
            .description("Sends #thisway <distance> and #elytra.")
            .defaultValue(Keybind.none())
            .build()
    );

    private final Setting<Integer> distance = sgSettings.add(new IntSetting.Builder()
            .name("Distance")
            .description("The distance to fly using Baritone Elytra.")
            .defaultValue(500)
            .min(1)
            .sliderMax(10000)
            .build()
    );

    private final Setting<Keybind> abortKeybind = sgSettings.add(new KeybindSetting.Builder()
            .name("Abort Elytra Fly")
            .description("Stops Elytra fly (#stop).")
            .defaultValue(Keybind.none())
            .build()
    );
    private final boolean trailFollowerWasActive = false;

    public BaritonePathing() {
        super(PathSeeker.Automation, "BaritonePathMacro", "Easy macro to activate TrailFollower in the nether. Must deploy in air.");
        BaritoneAPI.getSettings().logger.value = this::info;  // fix for baritone chat logs
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (flyKeybind.get().isPressed()) {
            sendBaritoneCommand("thisway " + distance.get());
            sendBaritoneCommand("elytra");
            info("Activated Baritone macro: #thisway " + distance.get() + " + #elytra");
        }

        if (abortKeybind.get().isPressed()) {
            sendBaritoneCommand("stop");

            Module trailFollower = Modules.get().get("TrailFollower");
            if (trailFollower != null && trailFollower.isActive()) {
                trailFollower.toggle();  // <-- ACTUALLY DISABLE IT
                info("TrailFollower was active. Disabling it due to abort.");
            }
        }
    }


    private void sendBaritoneCommand(String command) {
        if (command != null && !command.isEmpty()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(command);
        }
    }
}