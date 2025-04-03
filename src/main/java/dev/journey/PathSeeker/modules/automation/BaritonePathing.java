package dev.journey.PathSeeker.modules.automation;

import baritone.api.BaritoneAPI;
import dev.journey.PathSeeker.PathSeeker;
import dev.journey.PathSeeker.modules.exploration.TrailFollower;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Keybind;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.text.Text;

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

    public BaritonePathing() {
        super(PathSeeker.Automation, "BaritonePathMacro", "Easy macro to activate TrailFollower in the nether. Must deploy in air.");
        BaritoneAPI.getSettings().logger.value = (s) -> {}; // Disable baritone chat spam
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (flyKeybind.get().isPressed()) {
            sendBaritoneCommand("thisway " + distance.get());
            sendBaritoneCommand("elytra");
        }

        if (abortKeybind.get().isPressed()) {
            sendBaritoneCommand("stop");

            TrailFollower trailFollower = Modules.get().get(TrailFollower.class);
            if (trailFollower.isActive()) {
                trailFollower.toggle();
                info("TrailFollower was active. Disabling it due to abort.");
            }
        }
    }

    private void sendBaritoneCommand(String command) {
        if (command != null && !command.isEmpty()) {
            BaritoneAPI.getProvider().getPrimaryBaritone().getCommandManager().execute(command);
        }
    }

    @EventHandler
    private void onReceiveMessage(ReceiveMessageEvent event) {
        String msg = event.getMessage().getString();
        if (msg.contains("[Baritone] Goal:") || msg.contains("ok canceled")) {
            event.setCancelled(true);
        }
    }
}
