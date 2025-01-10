package dev.journey.PathSeeker.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class ApiHandler {
    public static final String API_2B2T_URL = "https://api.2b2t.vc";

    public String fetchResponse(String urlString) {
        try {
            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);

            int status = connection.getResponseCode();

            if (status == 200) {
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                StringBuilder content = new StringBuilder();
                String line;

                while ((line = in.readLine()) != null) {
                    content.append(line);
                }

                in.close();
                connection.disconnect();

                return content.toString();
            } else if (status == 204) {
                return "204 Undocumented";
            } else {
                return null;
            }
        } catch (Exception e) {
            PathSeekerUtil.logError("Failed to fetch API response: " + e.getMessage());
            return null;
        }
    }
}
