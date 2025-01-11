package dev.journey.PathSeeker.utils;

import dev.journey.PathSeeker.PathSeeker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class ApiHandler {
    public static final String API_2B2T_URL = "https://api.2b2t.vc";
    private static final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    public String fetchResponse(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "PathSeeker/1.0")
                    .GET()
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 204) {
                return "204 Undocumented";
            }

            return response.body();
        } catch (Exception e) {
            PathSeeker.LOG.error("[ApiHandler] Failed to fetch response: {}", e.getMessage());
            return null;
        }
    }
}