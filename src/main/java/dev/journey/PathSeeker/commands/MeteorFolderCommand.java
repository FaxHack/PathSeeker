package dev.journey.PathSeeker.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.text.Text;

import java.awt.*;
import java.io.File;
import java.io.IOException;

public class MeteorFolderCommand extends Command {
    public MeteorFolderCommand() {
        super("meteorfolder", "Opens the meteor folder.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            {
                ChatUtils.sendMsg(Text.of("Opening meteor folder."));
            }
            File meteor = new File(MinecraftClient.getInstance().runDirectory, "meteor-client");
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(meteor);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // Fallback or error handling
                ChatUtils.sendMsg(Text.of("Desktop API not supported on this system."));
            }
            return SINGLE_SUCCESS;
        });
    }
}
