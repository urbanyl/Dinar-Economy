package fr.dinar.logs;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import fr.dinar.DinarMod;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class DiscordWebhook {

    private static final Gson GSON = new Gson();
    private static final ExecutorService EXEC = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "Dinar-Webhook");
        t.setDaemon(true);
        return t;
    });
    private static volatile boolean errorLogged = false;

    private DiscordWebhook() {}

    public static void sendAsync(String url, DiscordEmbed embed) {
        if (url == null || url.isBlank()) return;
        EXEC.execute(() -> send(url, embed));
    }

    private static void send(String url, DiscordEmbed embed) {
        try {
            JsonArray embeds = new JsonArray();
            embeds.add(embed.toJson());
            JsonObject root = new JsonObject();
            root.add("embeds", embeds);

            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build();
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(10))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(root)))
                    .build();
            client.send(request, HttpResponse.BodyHandlers.discarding());
        } catch (Exception e) {
            if (!errorLogged) {
                errorLogged = true;
                DinarMod.LOGGER.warn("[Dinar] Echec envoi webhook Discord.", e);
            }
        }
    }

    public static final class DiscordEmbed {
        private final JsonObject json = new JsonObject();

        public DiscordEmbed title(String title) {
            json.addProperty("title", title);
            return this;
        }

        public DiscordEmbed description(String description) {
            json.addProperty("description", description);
            return this;
        }

        public DiscordEmbed color(int color) {
            json.addProperty("color", color);
            return this;
        }

        public DiscordEmbed field(String name, String value) {
            return field(name, value, true);
        }

        public DiscordEmbed field(String name, String value, boolean inline) {
            JsonObject f = new JsonObject();
            f.addProperty("name", name);
            f.addProperty("value", value);
            f.addProperty("inline", inline);
            if (!json.has("fields")) json.add("fields", new JsonArray());
            json.getAsJsonArray("fields").add(f);
            return this;
        }

        public DiscordEmbed footer(String text) {
            JsonObject fo = new JsonObject();
            fo.addProperty("text", text);
            json.add("footer", fo);
            return this;
        }

        public JsonObject toJson() {
            json.addProperty("timestamp", DateTimeFormatter.ISO_INSTANT.format(Instant.now()));
            return json;
        }
    }
}
