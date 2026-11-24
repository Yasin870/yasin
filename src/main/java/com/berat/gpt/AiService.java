package com.berat.gpt;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Dis yapay zeka API'si. Oyunun ana dongusunu hic bloklamaz, her sey arka planda yapilir. */
public final class AiService {
    public record Result(boolean ok, String detail) {}

    private static final Map<UUID, Deque<String[]>> HISTORY = new HashMap<>();
    private static final Map<UUID, Long> LAST = new HashMap<>();

    /** Ayar ekraninin durumu: "idle", "testing", "ok" veya "fail:<kod>". */
    public static volatile String testState = "idle";

    private AiService() {}

    public static String tierKey(int day) {
        if (day <= 3) return "ai.friendly";
        if (day <= 5) return "ai.cold";
        return "ai.hostile";
    }

    private static String promptKey(int day) {
        if (day <= 3) return "ai.prompt.friendly";
        if (day <= 5) return "ai.prompt.cold";
        return "ai.prompt.hostile";
    }

    /** Oyuncu mesaj yazdiginda cagrilir. */
    public static void ask(ServerPlayer p, String raw) {
        String msg = raw == null ? "" : raw.trim();
        if (msg.isEmpty()) {
            StoryEvents.say(p, Lang.get("cmd.usage"));
            return;
        }
        if (msg.length() > 300) msg = msg.substring(0, 300);
        MinecraftServer server = p.getServer();
        if (server == null) return;
        ServerLevel level = p.serverLevel();
        int day = StoryEvents.dayOf(level);

        // API dogrulanmadiysa: yerel, hazir replikler (API yoksa hikaye yine calisir)
        if (!GptConfig.isValidated()) {
            StoryEvents.say(p, Lang.pick(tierKey(day), p.getRandom()));
            return;
        }

        long now = System.currentTimeMillis();
        Long last = LAST.get(p.getUUID());
        if (last != null && now - last < 2000L) return; // istek sinirlamasi
        LAST.put(p.getUUID(), now);

        final String system = Lang.get(promptKey(day));
        final List<String[]> hist = new ArrayList<>(HISTORY.computeIfAbsent(p.getUUID(), k -> new ArrayDeque<>()));
        final String userMsg = msg;
        final UUID id = p.getUUID();

        CompletableFuture.runAsync(() -> {
            Result r = chat(system, hist, userMsg);
            server.execute(() -> {
                ServerPlayer pl = server.getPlayerList().getPlayer(id);
                if (pl == null) return;
                if (r.ok()) {
                    remember(id, userMsg, r.detail());
                    StoryEvents.say(pl, clean(r.detail()));
                } else {
                    // API gecici olarak yoksa yerel yedek davranis
                    StoryEvents.say(pl, Lang.pick(tierKey(day), pl.getRandom()));
                    pl.sendSystemMessage(Component.literal(detailMessage(r.detail())).withStyle(ChatFormatting.DARK_GRAY));
                }
            });
        });
    }

    private static void remember(UUID id, String user, String reply) {
        Deque<String[]> h = HISTORY.computeIfAbsent(id, k -> new ArrayDeque<>());
        h.addLast(new String[] {"user", user});
        h.addLast(new String[] {"assistant", reply});
        while (h.size() > 8) h.removeFirst();
    }

    private static String clean(String s) {
        String t = s.replace('\n', ' ').replace("\u00A7", "").trim();
        return t.length() > 240 ? t.substring(0, 240) : t;
    }

    /** Ayar ekranindan baglanti testi. Sonuc arka plan is parcaciginda gelir. */
    public static void validateAsync(Consumer<Result> cb) {
        CompletableFuture.runAsync(() -> cb.accept(chat("Reply with OK.", new ArrayList<>(), "ping")));
    }

    public static String detailMessage(String detail) {
        if (detail == null) return Lang.get("err.noreply");
        if (detail.startsWith("http:")) return Lang.get("err.http", detail.substring(5));
        switch (detail) {
            case "none": return Lang.get("err.none");
            case "auth": return Lang.get("err.auth");
            case "rate": return Lang.get("err.rate");
            case "network": return Lang.get("err.network");
            case "timeout": return Lang.get("err.timeout");
            case "badurl": return Lang.get("err.badurl");
            case "empty": return Lang.get("err.empty");
            case "parse": return Lang.get("err.parse");
            default: return Lang.get("err.noreply");
        }
    }

    private static JsonObject obj(String role, String content) {
        JsonObject o = new JsonObject();
        o.addProperty("role", role);
        o.addProperty("content", content);
        return o;
    }

    private static Result chat(String system, List<String[]> hist, String user) {
        HttpURLConnection c = null;
        try {
            String key = GptConfig.key().trim();
            if (key.isEmpty()) return new Result(false, "none");
            String url = GptConfig.effectiveUrl();
            if (!url.startsWith("https://")) return new Result(false, "badurl");

            JsonArray msgs = new JsonArray();
            msgs.add(obj("system", system));
            for (String[] h : hist) msgs.add(obj(h[0], h[1]));
            msgs.add(obj("user", user));
            JsonObject body = new JsonObject();
            body.addProperty("model", GptConfig.model());
            body.add("messages", msgs);

            c = (HttpURLConnection) new URL(url).openConnection();
            c.setRequestMethod("POST");
            c.setConnectTimeout(10_000);
            c.setReadTimeout(45_000);
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Authorization", "Bearer " + key);
            try (OutputStream os = c.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }
            int code = c.getResponseCode();
            if (code == 401 || code == 403) return new Result(false, "auth");
            if (code == 429) return new Result(false, "rate");
            if (code < 200 || code >= 300) return new Result(false, "http:" + code);

            String s = new String(c.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            JsonObject j = JsonParser.parseString(s).getAsJsonObject();
            String text = j.getAsJsonArray("choices").get(0).getAsJsonObject()
                    .getAsJsonObject("message").get("content").getAsString();
            if (text == null || text.isBlank()) return new Result(false, "empty");
            return new Result(true, text.trim());
        } catch (SocketTimeoutException e) {
            return new Result(false, "timeout");
        } catch (IOException e) {
            return new Result(false, "network");
        } catch (Exception e) {
            return new Result(false, "parse");
        } finally {
            if (c != null) c.disconnect();
        }
    }
}
