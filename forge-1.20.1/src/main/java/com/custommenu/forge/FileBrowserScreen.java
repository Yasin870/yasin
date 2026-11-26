package com.custommenu.forge;

import com.custommenu.core.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.*;
import java.util.function.Consumer;

/** Modun kendi dosya yöneticisi: klasörlerde gez, ara, boyutları gör, seç ve onayla. */
public class FileBrowserScreen extends Screen {
    private static final int TOP = 62, ROW = 18;
    private final Screen parent;
    private final ScreenType type;
    private final boolean audioOnly;
    private final Consumer<File> onPick;
    private File dir, selected;
    private String filter = "";
    private int offset = 0;
    private boolean copy = false;
    private EditBox search;
    private List<File> entries = new ArrayList<>();

    public FileBrowserScreen(Screen parent, ScreenType type, boolean audioOnly, Consumer<File> onPick) {
        super(Component.literal(audioOnly ? "Müzik seç" : "Görsel / video seç"));
        this.parent = parent; this.type = type; this.audioOnly = audioOnly; this.onPick = onPick;
        File[] starts = { new File("/storage/emulated/0"), new File(System.getProperty("user.home", ".")), new File(".") };
        for (File f : starts) if (f.isDirectory()) { dir = f; break; }
        refresh();
    }

    private void refresh() {
        List<File> out = new ArrayList<>();
        try {
            File[] list = dir.listFiles();
            if (list != null) for (File f : list) {
                if (f.getName().startsWith(".")) continue;
                if (!filter.isEmpty() && !f.getName().toLowerCase(Locale.ROOT).contains(filter.toLowerCase(Locale.ROOT))) continue;
                if (f.isDirectory()) out.add(f);
                else {
                    MediaKind k = MediaKind.detect(f.getName());
                    if (audioOnly ? k == MediaKind.AUDIO : (k != MediaKind.UNKNOWN && k != MediaKind.AUDIO)) out.add(f);
                }
            }
        } catch (Throwable ignored) { }
        out.sort((a, b) -> a.isDirectory() != b.isDirectory() ? (a.isDirectory() ? -1 : 1) : a.getName().compareToIgnoreCase(b.getName()));
        entries = out; offset = 0;
    }
    private int visibleRows() { return Math.max(1, (height - TOP - 36) / ROW); }
    private static String size(long b) {
        if (b < 1024) return b + " B";
        if (b < 1024 * 1024) return (b / 1024) + " KB";
        return String.format(Locale.ROOT, "%.1f MB", b / 1048576.0);
    }

    @Override protected void init() {
        search = new EditBox(font, 10, 30, width - 20, 20, Component.literal("Ara"));
        search.setHint(Component.literal("Dosya ara..."));
        search.setValue(filter);
        search.setResponder(v -> { filter = v; refresh(); });
        addRenderableWidget(search);
        int bw = Math.max(60, (width - 25) / 4), y = height - 26;
        addRenderableWidget(Button.builder(Component.literal("Üst klasör"), b -> {
            File p = dir.getParentFile(); if (p != null) { dir = p; selected = null; refresh(); }
        }).bounds(5, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Kopya: " + (copy ? "Açık" : "Kapalı")), b -> { copy = !copy; rebuildWidgets(); })
                .bounds(10 + bw, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("Seç"), b -> confirm()).bounds(15 + bw * 2, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("İptal"), b -> onClose()).bounds(20 + bw * 3, y, bw, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▲"), b -> offset = Math.max(0, offset - visibleRows() / 2))
                .bounds(width - 26, TOP, 22, 20).build());
        addRenderableWidget(Button.builder(Component.literal("▼"), b -> offset = Math.min(Math.max(0, entries.size() - visibleRows()), offset + visibleRows() / 2))
                .bounds(width - 26, TOP + 24, 22, 20).build());
    }

    private void confirm() {
        if (selected == null || !selected.isFile()) return;
        File f = selected;
        try {
            if (copy) f = MediaScanner.copyInto(selected, CustomMenuMod.CONFIG.dir().resolve(type.folder).toFile());
        } catch (Throwable ignored) { f = selected; } // kopyalanamazsa orijinali kullan
        onPick.accept(f);
        minecraft.setScreen(parent);
    }

    @Override public boolean mouseClicked(double mx, double my, int btn) {
        if (super.mouseClicked(mx, my, btn)) return true;
        if (mx >= 10 && mx < width - 30 && my >= TOP) {
            int i = offset + (int) ((my - TOP) / ROW);
            if (i < entries.size() && (my - TOP) / ROW < visibleRows()) {
                File f = entries.get(i);
                if (f.isDirectory()) { dir = f; selected = null; setFocused(null); refresh(); }
                else selected = f;
                return true;
            }
        }
        return false;
    }
    @Override public boolean mouseScrolled(double mx, double my, double delta) {
        offset = (int) Math.max(0, Math.min(Math.max(0, entries.size() - visibleRows()), offset - delta));
        return true;
    }
    @Override public void render(GuiGraphics g, int mx, int my, float pt) {
        renderBackground(g);
        g.drawCenteredString(font, title, width / 2, 10, 0xFFFFFF);
        g.drawString(font, font.plainSubstrByWidth(dir.getAbsolutePath(), width - 20), 10, 52, 0x88CCFF);
        for (int r = 0; r < visibleRows(); r++) {
            int i = offset + r; if (i >= entries.size()) break;
            File f = entries.get(i); int y = TOP + r * ROW;
            boolean hover = mx >= 10 && mx < width - 30 && my >= y && my < y + ROW;
            if (f.equals(selected)) g.fill(10, y, width - 30, y + ROW - 1, 0x806040C0);
            else if (hover) g.fill(10, y, width - 30, y + ROW - 1, 0x40FFFFFF);
            String label = f.isDirectory() ? "[Klasör] " + f.getName() : f.getName() + "   (" + size(f.length()) + ")";
            g.drawString(font, font.plainSubstrByWidth(label, width - 50), 14, y + 5, f.isDirectory() ? 0xFFDD66 : 0xFFFFFF);
        }
        if (entries.isEmpty()) g.drawCenteredString(font, Component.literal("Bu klasörde uygun dosya yok"), width / 2, TOP + 20, 0xAAAAAA);
        super.render(g, mx, my, pt);
    }
    @Override public void onClose() { minecraft.setScreen(parent); }
}
