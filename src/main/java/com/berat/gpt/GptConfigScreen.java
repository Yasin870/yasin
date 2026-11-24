package com.berat.gpt;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;

/** Minecraft GPT ayar ekrani: dil, saglayici, API anahtari, model ve baglanti testi. */
public class GptConfigScreen extends Screen {
    private final Screen parent;
    private EditBox keyBox;
    private EditBox modelBox;
    private EditBox urlBox;
    private int keyY;
    private int modelY;
    private int urlY;
    private int statusY;

    public GptConfigScreen(Screen parent) {
        super(Component.literal("Minecraft GPT"));
        this.parent = parent;
    }

    private static String t(String key) {
        return Lang.get(key);
    }

    private static Component c(String s) {
        return Component.literal(s);
    }

    @Override
    protected void init() {
        int w = Math.min(290, this.width - 20);
        int x = (this.width - w) / 2;
        int h = 20;
        int y = 26;

        // Dil / Language
        String langName = "en".equals(GptConfig.language()) ? "English" : "T\u00FCrk\u00E7e";
        this.addRenderableWidget(Button.builder(c("Dil / Language: " + langName), b -> {
            applyFields();
            GptConfig.setLanguage("en".equals(GptConfig.language()) ? "tr" : "en");
            Lang.reload();
            this.rebuildWidgets();
        }).bounds(x, y, w, h).tooltip(Tooltip.create(c(t("tip.language")))).build());
        y += 23;

        // Saglayici
        this.addRenderableWidget(Button.builder(c(t("gui.provider") + ": " + t("gui.prov." + GptConfig.provider())), b -> {
            applyFields();
            GptConfig.cycleProvider();
            this.rebuildWidgets();
        }).bounds(x, y, w, h).tooltip(Tooltip.create(c(t("tip.provider")))).build());
        y += 32;

        // API anahtari (yildizla gizli)
        keyY = y;
        keyBox = new EditBox(this.font, x, y, w, h, c(t("gui.apikey")));
        keyBox.setMaxLength(300);
        keyBox.setValue(GptConfig.key());
        keyBox.addFormatter((s, i) -> FormattedCharSequence.forward("*".repeat(s.length()), Style.EMPTY));
        keyBox.setHint(c(t("gui.apikey")));
        keyBox.setTooltip(Tooltip.create(c(t("tip.apikey"))));
        this.addRenderableWidget(keyBox);
        y += 32;

        // Model
        modelY = y;
        modelBox = new EditBox(this.font, x, y, w, h, c(t("gui.model")));
        modelBox.setMaxLength(100);
        modelBox.setValue(GptConfig.model());
        modelBox.setTooltip(Tooltip.create(c(t("tip.model"))));
        this.addRenderableWidget(modelBox);
        y += 32;

        // Ozel adres (yalnizca CUSTOM)
        urlBox = null;
        if ("CUSTOM".equals(GptConfig.provider())) {
            urlY = y;
            urlBox = new EditBox(this.font, x, y, w, h, c(t("gui.url")));
            urlBox.setMaxLength(300);
            urlBox.setValue(GptConfig.customUrl());
            urlBox.setTooltip(Tooltip.create(c(t("tip.url"))));
            this.addRenderableWidget(urlBox);
            y += 32;
        }

        // Anahtari kaydet
        String onOff = GptConfig.saveKeyOn() ? t("gui.on") : t("gui.off");
        this.addRenderableWidget(Button.builder(c(t("gui.savekey") + ": " + onOff), b -> {
            applyFields();
            GptConfig.toggleSaveKey();
            this.rebuildWidgets();
        }).bounds(x, y, w, h).tooltip(Tooltip.create(c(t("tip.savekey")))).build());
        y += 23;

        // Test + Bitti
        int half = (w - 6) / 2;
        this.addRenderableWidget(Button.builder(c(t("gui.test")), b -> doTest())
                .bounds(x, y, half, h).tooltip(Tooltip.create(c(t("tip.test")))).build());
        this.addRenderableWidget(Button.builder(c(t("gui.done")), b -> this.onClose())
                .bounds(x + half + 6, y, half, h).build());
        y += 26;
        statusY = y;
    }

    private void applyFields() {
        if (keyBox == null) return;
        String k = keyBox.getValue().trim();
        if (!k.equals(GptConfig.key())) GptConfig.setKey(k);
        String m = modelBox.getValue().trim();
        if (!m.equals(GptConfig.model())) GptConfig.setModel(m);
        if (urlBox != null) {
            String u = urlBox.getValue().trim();
            if (!u.equals(GptConfig.customUrl())) GptConfig.setCustomUrl(u);
        }
    }

    private void doTest() {
        applyFields();
        if (!GptConfig.hasKey()) {
            AiService.testState = "fail:none";
            return;
        }
        AiService.testState = "testing";
        AiService.validateAsync(res -> Minecraft.getInstance().execute(() -> {
            if (res.ok()) {
                GptConfig.markValidated();
                AiService.testState = "ok";
            } else {
                AiService.testState = "fail:" + res.detail();
            }
        }));
    }

    private String statusText() {
        String s = AiService.testState;
        if ("testing".equals(s)) return t("status.testing");
        if ("ok".equals(s)) return t("status.ok");
        if (s.startsWith("fail:")) return t("status.fail") + ": " + AiService.detailMessage(s.substring(5));
        if (!GptConfig.hasKey()) return t("status.none");
        return GptConfig.isValidated() ? t("status.ok") : t("status.unverified");
    }

    private int statusColor() {
        String s = AiService.testState;
        if ("testing".equals(s)) return 0xFFFF55;
        if ("ok".equals(s) || (s.equals("idle") && GptConfig.isValidated())) return 0x55FF55;
        if (s.startsWith("fail:")) return 0xFF5555;
        return 0xAAAAAA;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(g);
        g.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);
        int w = Math.min(290, this.width - 20);
        int x = (this.width - w) / 2;
        g.drawString(this.font, t("gui.apikey"), x, keyY - 10, 0xA0A0A0, false);
        g.drawString(this.font, t("gui.model"), x, modelY - 10, 0xA0A0A0, false);
        if (urlBox != null) g.drawString(this.font, t("gui.url"), x, urlY - 10, 0xA0A0A0, false);
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(this.font, t("gui.status") + ": " + statusText(), this.width / 2, statusY, statusColor());
    }

    @Override
    public void onClose() {
        applyFields();
        this.minecraft.setScreen(parent);
    }
}
