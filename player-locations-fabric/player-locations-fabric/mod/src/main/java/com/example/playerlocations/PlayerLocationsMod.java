package com.example.playerlocations;

import net.fabricmc.api.DedicatedServerModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Properties;

public class PlayerLocationsMod implements DedicatedServerModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("playerlocations");

    private static final String DEFAULT_CONFIG =
            "# Full URL of your website's update endpoint\n" +
            "url=https://YOUR-SITE.example.com/update\n" +
            "\n" +
            "# Must match the SECRET you set on the website server\n" +
            "secret=change-me-to-a-long-random-string\n" +
            "\n" +
            "# How often to send locations (seconds)\n" +
            "interval-seconds=5\n";

    private final HttpClient client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private String url;
    private String secret;
    private int intervalTicks;
    private int tickCounter = 0;

    @Override
    public void onInitializeServer() {
        loadConfig();
        if (url == null || url.isBlank() || url.contains("YOUR-SITE")) {
            LOGGER.warn("Set 'url' in config/playerlocations.properties, then restart the server.");
            return;
        }
        // Tick events run on the main server thread, so reading player data is safe.
        ServerTickEvents.END_SERVER_TICK.register(this::onTick);
        LOGGER.info("Sending player locations to {} every {} ticks", url, intervalTicks);
    }

    private void loadConfig() {
        Path path = FabricLoader.getInstance().getConfigDir().resolve("playerlocations.properties");
        try {
            if (!Files.exists(path)) {
                Files.writeString(path, DEFAULT_CONFIG);
            }
            Properties props = new Properties();
            try (Reader reader = Files.newBufferedReader(path)) {
                props.load(reader);
            }
            url = props.getProperty("url", "").trim();
            secret = props.getProperty("secret", "").trim();
            int seconds = 5;
            try {
                seconds = Math.max(1, Integer.parseInt(props.getProperty("interval-seconds", "5").trim()));
            } catch (NumberFormatException ignored) {
            }
            intervalTicks = seconds * 20;
        } catch (IOException e) {
            LOGGER.error("Could not read config: {}", e.getMessage());
        }
    }

    private void onTick(MinecraftServer server) {
        if (++tickCounter < intervalTicks) return;
        tickCounter = 0;
        push(server);
    }

    private void push(MinecraftServer server) {
        StringBuilder json = new StringBuilder("{\"players\":[");
        boolean first = true;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (!first) json.append(',');
            first = false;
            json.append("{\"name\":\"").append(escape(p.getName().getString())).append('"')
                .append(",\"world\":\"").append(escape(dimensionId(p))).append('"')
                .append(",\"x\":").append(Math.round(p.getX()))
                .append(",\"y\":").append(Math.round(p.getY()))
                .append(",\"z\":").append(Math.round(p.getZ()))
                .append('}');
        }
        json.append("]}");

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofSeconds(5))
                    .header("Content-Type", "application/json")
                    .header("x-secret", secret)
                    .POST(HttpRequest.BodyPublishers.ofString(json.toString()))
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                    .exceptionally(e -> {
                        LOGGER.warn("Could not reach website: {}", e.getMessage());
                        return null;
                    });
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid url in config: {}", e.getMessage());
        }
    }

    // Avoids renamed registry APIs: ResourceKey prints as "ResourceKey[minecraft:dimension / minecraft:overworld]"
    private static String dimensionId(ServerPlayer p) {
        String key = p.level().dimension().toString();
        int slash = key.lastIndexOf(" / ");
        int end = key.lastIndexOf(']');
        return (slash >= 0 && end > slash) ? key.substring(slash + 3, end) : key;
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
