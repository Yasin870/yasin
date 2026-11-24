package com.berat.gpt;

import net.minecraftforge.common.ForgeConfigSpec;

/** Minecraft GPT ayarlari. API anahtari hicbir zaman log'a veya sohbete yazilmaz. */
public final class GptConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> LANGUAGE;
    public static final ForgeConfigSpec.ConfigValue<String> PROVIDER;
    public static final ForgeConfigSpec.ConfigValue<String> API_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> MODEL;
    public static final ForgeConfigSpec.ConfigValue<String> CUSTOM_URL;
    public static final ForgeConfigSpec.BooleanValue SAVE_KEY;
    public static final ForgeConfigSpec.ConfigValue<String> VALIDATED;

    /** "Anahtari kaydet" kapaliyken anahtar yalnizca bellekte tutulur. */
    private static volatile String sessionKey = "";

    static {
        ForgeConfigSpec.Builder b = new ForgeConfigSpec.Builder();
        b.comment("Minecraft GPT ayarlari / settings").push("gpt");
        LANGUAGE = b.comment("tr veya en / tr or en").define("language", "tr");
        PROVIDER = b.comment("XAI, OPENAI veya CUSTOM").define("provider", "XAI");
        API_KEY = b.comment("API anahtari - paylasma / API key - do not share").define("api_key", "");
        MODEL = b.comment("Model adi / model name").define("model", "grok-4.3");
        CUSTOM_URL = b.comment("Ozel adres (CUSTOM icin) / custom URL").define("custom_url", "");
        SAVE_KEY = b.comment("Anahtari dosyaya kaydet / save key to file").define("save_key", true);
        VALIDATED = b.comment("Dogrulama kaydi, elle degistirme / validation record, do not edit").define("validated", "");
        b.pop();
        SPEC = b.build();
    }

    private GptConfig() {}

    // ---- dil ----
    public static String language() {
        try {
            return "en".equalsIgnoreCase(LANGUAGE.get()) ? "en" : "tr";
        } catch (Exception e) {
            return "tr";
        }
    }

    public static void setLanguage(String lang) {
        LANGUAGE.set("en".equals(lang) ? "en" : "tr");
        LANGUAGE.save();
    }

    // ---- saglayici / model ----
    public static String provider() {
        try {
            String p = PROVIDER.get();
            if ("OPENAI".equals(p) || "CUSTOM".equals(p)) return p;
        } catch (Exception ignored) {
        }
        return "XAI";
    }

    public static String defaultModel(String provider) {
        switch (provider) {
            case "OPENAI": return "gpt-4o-mini";
            case "CUSTOM": return "";
            default: return "grok-4.3";
        }
    }

    public static void cycleProvider() {
        String next;
        switch (provider()) {
            case "XAI": next = "OPENAI"; break;
            case "OPENAI": next = "CUSTOM"; break;
            default: next = "XAI"; break;
        }
        PROVIDER.set(next);
        MODEL.set(defaultModel(next));
        PROVIDER.save();
        MODEL.save();
    }

    public static String model() {
        try {
            return MODEL.get().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static void setModel(String m) {
        MODEL.set(m);
        MODEL.save();
    }

    public static String customUrl() {
        try {
            return CUSTOM_URL.get().trim();
        } catch (Exception e) {
            return "";
        }
    }

    public static void setCustomUrl(String u) {
        CUSTOM_URL.set(u);
        CUSTOM_URL.save();
    }

    public static String effectiveUrl() {
        switch (provider()) {
            case "OPENAI": return "https://api.openai.com/v1/chat/completions";
            case "CUSTOM": return customUrl();
            default: return "https://api.x.ai/v1/chat/completions";
        }
    }

    // ---- anahtar ----
    public static boolean saveKeyOn() {
        try {
            return SAVE_KEY.get();
        } catch (Exception e) {
            return true;
        }
    }

    public static String key() {
        try {
            return saveKeyOn() ? API_KEY.get() : sessionKey;
        } catch (Exception e) {
            return sessionKey;
        }
    }

    public static boolean hasKey() {
        return !key().trim().isEmpty();
    }

    public static void setKey(String k) {
        sessionKey = k;
        API_KEY.set(saveKeyOn() ? k : "");
        API_KEY.save();
    }

    public static void toggleSaveKey() {
        String current = key();
        boolean next = !saveKeyOn();
        sessionKey = current;
        SAVE_KEY.set(next);
        API_KEY.set(next ? current : "");
        SAVE_KEY.save();
        API_KEY.save();
    }

    // ---- dogrulama kaydi: anahtar/saglayici/model degisirse gecersiz olur ----
    private static String fingerprint() {
        String raw = key() + "|" + provider() + "|" + model() + "|" + effectiveUrl();
        return Integer.toHexString(raw.hashCode());
    }

    public static boolean isValidated() {
        try {
            return hasKey() && fingerprint().equals(VALIDATED.get());
        } catch (Exception e) {
            return false;
        }
    }

    public static void markValidated() {
        VALIDATED.set(fingerprint());
        VALIDATED.save();
    }
}
