package com.berat.gpt;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.util.RandomSource;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Minecraft GPT'ye ozel dil sistemi. Vanilla Minecraft metinlerine dokunmaz. */
public final class Lang {
    private static volatile JsonObject tr = new JsonObject();
    private static volatile JsonObject en = new JsonObject();

    private Lang() {}

    /** Dil dosyalarini yeniden okur (oyunu kapatmadan). */
    public static synchronized void reload() {
        tr = load("tr_tr.json");
        en = load("en_us.json");
    }

    private static JsonObject load(String name) {
        try (InputStream in = Lang.class.getResourceAsStream("/assets/minecraftgpt/gptlang/" + name)) {
            if (in == null) return new JsonObject();
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return new JsonObject();
        }
    }

    private static JsonElement find(String key) {
        JsonObject cur = "en".equals(GptConfig.language()) ? en : tr;
        JsonElement el = cur.get(key);
        if (el == null) el = tr.get(key); // eksik ceviri: Turkce yedek
        return el;
    }

    private static String first(JsonElement el) {
        if (el == null || el.isJsonNull()) return "...";
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            return arr.size() == 0 ? "..." : arr.get(0).getAsString();
        }
        return el.getAsString();
    }

    public static String get(String key, Object... args) {
        String text = first(find(key));
        if (args.length > 0) {
            try {
                return String.format(Locale.ROOT, text, args);
            } catch (Exception e) {
                return text;
            }
        }
        return text;
    }

    /** Dizi ise rastgele bir satir, metin ise kendisini dondurur. */
    public static String pick(String key, RandomSource r) {
        JsonElement el = find(key);
        if (el != null && el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            if (arr.size() > 0) return arr.get(r.nextInt(arr.size())).getAsString();
        }
        return first(el);
    }

    public static List<String> list(String key) {
        List<String> out = new ArrayList<>();
        JsonElement el = find(key);
        if (el != null && el.isJsonArray()) {
            for (JsonElement e : el.getAsJsonArray()) out.add(e.getAsString());
        } else if (el != null && !el.isJsonNull()) {
            out.add(el.getAsString());
        }
        return out;
    }
}
