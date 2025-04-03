package dev.journey.PathSeeker.utils.Update;

import dev.journey.PathSeeker.PathSeeker;
import net.minecraft.client.MinecraftClient;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

public class UpdateChecker {
    private static final String GITHUB_URL = "https://api.github.com/repos/FaxHack/PathSeeker/releases/latest";
    private static final String CURRENT_VERSION = "1.0.7";
    private static final String USER_AGENT = "Mozilla/5.0";

    public static void checkForUpdate() {
        if (UserConfig.isUpdateCheckDisabled()) return;
    
        CompletableFuture.runAsync(() -> {
            try {
                String latestVersion = fetchLatestVersion();
                if (latestVersion != null && isNewerVersion(latestVersion)) {
                    MinecraftClient.getInstance().execute(() ->
                            MinecraftClient.getInstance().setScreen(new UpdateScreen(latestVersion))
                    );
                }
            } catch (Exception e) {
                PathSeeker.LOG.error("Failed to check for updates", e);
            }
        });
    }

    private static String fetchLatestVersion() {
        try {
            URL url = new URI(GITHUB_URL).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", USER_AGENT);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            if (conn.getResponseCode() != 200) {
                PathSeeker.LOG.error("Failed to check for updates: HTTP {}", conn.getResponseCode());
                return null;
            }

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                String response = reader.lines().collect(Collectors.joining());
                JSONObject json = new JSONObject(response);
                return json.getString("tag_name");
            }
        } catch (Exception e) {
            PathSeeker.LOG.error("Failed to fetch latest version", e);
            return null;
        }
    }

    private static boolean isNewerVersion(String latest) {
        try {
            String[] latestParts = latest.replaceAll("[^0-9.]", "").split("\\.");
            String[] currentParts = UpdateChecker.CURRENT_VERSION.replaceAll("[^0-9.]", "").split("\\.");

            int length = Math.max(latestParts.length, currentParts.length);
            for (int i = 0; i < length; i++) {
                int l = i < latestParts.length ? Integer.parseInt(latestParts[i]) : 0;
                int c = i < currentParts.length ? Integer.parseInt(currentParts[i]) : 0;
                if (l > c) return true;
                if (l < c) return false;
            }
            return false;
        } catch (NumberFormatException e) {
            PathSeeker.LOG.error("Failed to parse version numbers", e);
            return false;
        }
    }
}