package dev.journey.PathSeeker.commands;

import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

public class CoordinateConverterCommand extends Command {
    public CoordinateConverterCommand() {
        super("coords", "Converts coordinates between Overworld and Nether.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("overworld")
            .then(argument("x", IntegerArgumentType.integer())
            .then(argument("y", IntegerArgumentType.integer())
            .then(argument("z", IntegerArgumentType.integer())
            .executes(context -> {
                int x = context.getArgument("x", Integer.class);
                int y = context.getArgument("y", Integer.class);
                int z = context.getArgument("z", Integer.class);
                
                int netherX = x / 8;
                int netherZ = z / 8;
                
                String result = String.format("Overworld: %d %d %d -> Nether: %d %d %d", x, y, z, netherX, y, netherZ);
                ChatUtils.info(result);
                copyToClipboard(result);
                return SINGLE_SUCCESS;
            })))));
            
        builder.then(literal("nether")
            .then(argument("x", IntegerArgumentType.integer())
            .then(argument("y", IntegerArgumentType.integer())
            .then(argument("z", IntegerArgumentType.integer())
            .executes(context -> {
                int x = context.getArgument("x", Integer.class);
                int y = context.getArgument("y", Integer.class);
                int z = context.getArgument("z", Integer.class);
                
                int overworldX = x * 8;
                int overworldZ = z * 8;
                
                String result = String.format("Nether: %d %d %d -> Overworld: %d %d %d", x, y, z, overworldX, y, overworldZ);
                ChatUtils.info(result);
                copyToClipboard(result);
                return SINGLE_SUCCESS;
            })))));
    }

    private void copyToClipboard(String text) {
        Toolkit.getDefaultToolkit()
            .getSystemClipboard()
            .setContents(new StringSelection(text), null);
        ChatUtils.info("Coordinates copied to clipboard!");
    }
} 