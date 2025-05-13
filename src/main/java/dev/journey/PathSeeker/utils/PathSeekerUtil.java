package dev.journey.PathSeeker.utils;

import dev.journey.PathSeeker.PathSeeker;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

import javax.annotation.Nullable;
import java.awt.*;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.TimeUnit;

public class PathSeekerUtil {
    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private String lastSeen;
    private String firstSeen;
    private long playtime;

    public PathSeekerUtil() {
        this.lastSeen = "";
        this.firstSeen = "";
        this.playtime = 0;
    }

    public static String randomColorCode() {
        String[] colorCodes = {"§4", "§c", "§6", "§e", "§2", "§a", "§b", "§3", "§1", "§9", "§d", "§5", "§7", "§8", "§0"};
        return colorCodes[(int) (Math.random() * colorCodes.length)];
    }

    public static void sendPlayerMessage(String message) {
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal(message), false);
        }
    }

    public static void firework(MinecraftClient mc, boolean requireElytra) {
        if (mc.player == null) return;

        if (requireElytra && !mc.player.isFallFlying()) return;

        mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);

    }

    public static void logError(String message) {
        System.err.println("[PathSeeker] " + message);
    }

    public static String formatDate(String timestamp) {
        try {
            Instant instant = Instant.parse(timestamp);
            ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy", Locale.US);
            return zonedDateTime.format(formatter);
        } catch (Exception e) {
            logError("Failed to parse date: " + timestamp);
            return "Invalid date";
        }
    }

    public static String formatPlaytime(long seconds) {
        if (seconds <= 0) return "none";

        long days = TimeUnit.SECONDS.toDays(seconds);
        long hours = TimeUnit.SECONDS.toHours(seconds) % 24;
        long minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60;

        if (days >= 30) {
            return (days / 30) + " months";
        } else if (days >= 7) {
            return (days / 7) + " weeks";
        } else if (days > 0) {
            return days + " days";
        } else if (hours > 0) {
            return hours + " hours";
        } else if (minutes > 0) {
            return minutes + " minutes";
        } else {
            return seconds + " seconds";
        }
    }

    public static boolean checkOrCreateFile(MinecraftClient mc, String fileName) {
        File file = FabricLoader.getInstance().getGameDir().resolve(fileName).toFile();

        if (!file.exists()) {
            try {
                if (file.createNewFile()) {
                    if (mc.player != null) {
                        mc.player.sendMessage(
                                Text.literal("§8<" + PathSeekerUtil.randomColorCode() + "§o✨§r§8> §7Created " + file.getName() + " in meteor-client folder.")
                        );
                        MutableText msg = Text.literal("§8<" + PathSeekerUtil.randomColorCode() + "§o✨§r§8> §7Click §2§lhere §r§7to open the file.");
                        Style style = Style.EMPTY.withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_FILE, file.getAbsolutePath()));

                        MutableText txt = msg.setStyle(style);
                        mc.player.sendMessage(txt);
                    }
                    return true;
                }
            } catch (Exception err) {
                PathSeeker.LOG.error("[PathSeeker] Error creating" + file.getAbsolutePath() + "! - Why:\n" + err);
            }
        } else return true;

        return false;
    }

    public static void openFile(MinecraftClient ignoredMc, String fileName) {
        File file = FabricLoader.getInstance().getGameDir().resolve(fileName).toFile();

        if (Desktop.isDesktopSupported()) {
            EventQueue.invokeLater(() -> {
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception err) {
                    PathSeeker.LOG.error("[PathSeeker] Failed to open " + file.getAbsolutePath() + "! - Why:\n" + err);
                }
            });
        } else {
            PathSeeker.LOG.error("[PathSeeker] Desktop operations not supported.");
        }
    }

    public static int firework(MinecraftClient mc) {
        int elytraSwapSlot = -1;

        if (!mc.player.getInventory().getArmorStack(2).isOf(Items.ELYTRA)) {
            FindItemResult itemResult = InvUtils.findInHotbar(Items.ELYTRA);
            if (!itemResult.found()) {
                return -1;
            } else {
                elytraSwapSlot = itemResult.slot();
                InvUtils.swap(itemResult.slot(), true);
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                InvUtils.swapBack();
                mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
            }
        }

        FindItemResult itemResult = InvUtils.findInHotbar(Items.FIREWORK_ROCKET);
        if (!itemResult.found()) return -1;

        if (itemResult.isOffhand()) {
            mc.interactionManager.interactItem(mc.player, Hand.OFF_HAND);
            mc.player.swingHand(Hand.OFF_HAND);
        } else {
            InvUtils.swap(itemResult.slot(), true);
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            InvUtils.swapBack();
        }

        return elytraSwapSlot != -1 ? elytraSwapSlot : 200;
    }

    public static void sendWebhook(String webhookURL, String title, String message, String pingID, String playerName) {
        String json = "";
        json += "{\"embeds\": [{"
                + "\"title\": \"" + title + "\","
                + "\"description\": \"" + message + "\","
                + "\"color\": 15258703,"
                + "\"footer\": {"
                + "\"text\": \"From: " + playerName + "\"}"
                + "}]}";
        sendWebhook(webhookURL, json, pingID);
    }

    public static void sendWebhook(String webhookURL, String jsonObject, String pingID) {
        sendRequest(webhookURL, jsonObject);

        if (pingID != null) {
            jsonObject = "{\"content\": \"<@" + pingID + ">\"}";
            sendRequest(webhookURL, jsonObject);
        }
    }

    public static Vec3d yawToDirection(double yaw) {
        yaw = yaw * Math.PI / 180;
        double x = -Math.sin(yaw);
        double z = Math.cos(yaw);
        return new Vec3d(x, 0, z);
    }

    public static double angleOnAxis(double yaw) {
        if (yaw < 0) yaw += 360;
        return Math.round(yaw / 45.0f) * 45;
    }

    public static double distancePointToDirection(Vec3d point, Vec3d direction, @Nullable Vec3d start) {
        if (start == null) start = Vec3d.ZERO;

        point = point.multiply(new Vec3d(1, 0, 1));
        start = start.multiply(new Vec3d(1, 0, 1));
        direction = direction.multiply(new Vec3d(1, 0, 1));

        Vec3d directionVec = point.subtract(start);

        double projectionLength = directionVec.dotProduct(direction) / direction.lengthSquared();
        Vec3d projection = direction.multiply(projectionLength);
        Vec3d perp = directionVec.subtract(projection);
        return perp.length();
    }

    private static void sendRequest(String webhookURL, String json) {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI(webhookURL))
                    .header("Content-Type", "application/json")
                    .header("User-Agent", "Mozilla")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            client.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void setPressed(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
        Input.setKeyState(key, pressed);
    }

    public static Vec3d positionInDirection(Vec3d pos, double yaw, double distance) {
        Vec3d offset = (new Vec3d(Math.sin(-yaw * Math.PI / 180), 0, Math.cos(-yaw * Math.PI / 180)).normalize()).multiply(distance);
        return pos.add(offset);
    }

    public String getFormattedLastSeen() {
        return formatDate(this.lastSeen);
    }

    public String getFormattedFirstSeen() {
        return formatDate(this.firstSeen);
    }

    public String getFormattedPlaytime() {
        return formatPlaytime(this.playtime);
    }

    public void updateTimeInfo(String lastSeen, String firstSeen, long playtime) {
        this.lastSeen = lastSeen;
        this.firstSeen = firstSeen;
        this.playtime = playtime;
    }
}
