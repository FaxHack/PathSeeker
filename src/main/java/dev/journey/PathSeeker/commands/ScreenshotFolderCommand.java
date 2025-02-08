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

public class ScreenshotFolderCommand extends Command {
    public ScreenshotFolderCommand() {
        super("screenshotfolder", "Opens the screenshot folder.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(context -> {
            {
                ChatUtils.sendMsg(Text.of("Opening screenshot folder."));
            }
            File screenshots = new File(MinecraftClient.getInstance().runDirectory, "screenshots");
            if (Desktop.isDesktopSupported()) {
                try {
                    Desktop.getDesktop().open(screenshots);
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
