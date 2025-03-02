package dev.journey.PathSeeker.modules.exploration.searcharea;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;

import static dev.journey.PathSeeker.utils.PathSeekerUtil.sendWebhook;
import static dev.journey.PathSeeker.utils.PathSeekerUtil.setPressed;

public class SearchAreaMode {

    protected static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    protected final SearchArea searchArea;
    protected final MinecraftClient mc;
    private final SearchAreaModes type;
    protected long paused = 0;

    public SearchAreaMode(SearchAreaModes type) {
        this.searchArea = Modules.get().get(SearchArea.class);
        this.mc = MinecraftClient.getInstance();
        this.type = type;
    }

    public void onTick() {

    }

    public void onActivate() {

    }

    public void onDeactivate() {
        setPressed(mc.options.forwardKey, false);
    }

    // stolen from autowalk

    public void onMessageReceive(ReceiveMessageEvent event) {
        Text message = event.getMessage();
        if (message.getString().contains("joined the game")) {
            // why is it a double???
            paused = (long) (System.nanoTime() + 1e10);
        }
        if (searchArea.webhookMode.get() != SearchArea.WebhookSettings.Off) {
            String title;
            boolean ping = false;
            if (searchArea.pingForStashFinder.get() &&
                    (message.getString().contains("Possible build") || message.getString().contains("Stash Finder"))) {
                if (!(searchArea.webhookMode.get() == SearchArea.WebhookSettings.LogBoth || searchArea.webhookMode.get() == SearchArea.WebhookSettings.LogStashes))
                    return;
                title = "Something Found!";
                ping = true;
            } else {
                if (!(searchArea.webhookMode.get() == SearchArea.WebhookSettings.LogBoth || searchArea.webhookMode.get() == SearchArea.WebhookSettings.LogChat))
                    return;
                title = "Chat Message";
            }
            sendWebhook(searchArea.webhookLink.get(), title, message.getString(), searchArea.discordId.get(), mc.player.getGameProfile().getName());
        }

    }

    protected File getJsonFile(String fileName) {
        try {
            return new File(new File(new File(MeteorClient.FOLDER, "search-area"), searchArea.saveLocation.get()), fileName + ".json");
        } catch (NullPointerException e) {
            return null;
        }
    }

    protected void saveToJson(boolean goingToStart, PathingData pd) {
        // Fix issue where "null" gets saved to the file creating crashes when it gets loaded next time.
        if (pd == null) return;
        // last pos doesn't matter if disconnecting while going to start
        if (!goingToStart) pd.currPos = mc.player.getBlockPos();
        try {
            File file = getJsonFile(type.toString());
            if (file == null) return;
            file.getParentFile().mkdirs();
            Writer writer = new FileWriter(file);
            GSON.toJson(pd, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void clear() {
        File file = getJsonFile(type.toString());
        file.delete();
    }

    public void clear(String mode) {
        File file = getJsonFile(mode);
        file.delete();
    }

    public void clearAll() {
        for (SearchAreaModes mode : SearchAreaModes.values()) {
            clear(mode.toString());
        }
    }

    public String toString() {
        return type.toString();
    }

    protected static class PathingData {
        public BlockPos initialPos;
        public BlockPos currPos;
        public float yawDirection;
        public boolean mainPath;
    }

}