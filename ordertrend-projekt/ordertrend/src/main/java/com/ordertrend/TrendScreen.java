package com.ordertrend;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TrendScreen extends Screen {
    private static final int ROW_H = 14;
    private static final int TOP = 62;
    private static final int W = 340;
    private static final String[] SORTS = {"Größter Anstieg", "Größter Fall", "Name", "Höchster Preis"};

    private static int sortMode = 0;

    private List<Store.Row> rows = new ArrayList<>();
    private int scroll = 0;
    private int x0;

    public TrendScreen() {
        super(Text.literal("Order Trend"));
    }

    @Override
    protected void init() {
        x0 = (width - W) / 2;
        rows = Store.analyze();
        sortRows();

        int by = height - 26;
        addDrawableChild(ButtonWidget.builder(Text.literal("Scan starten"), b -> {
            close();
            OrderScanner.start();
        }).dimensions(x0, by, 100, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Sortierung: " + SORTS[sortMode]), b -> {
            sortMode = (sortMode + 1) % SORTS.length;
            clearAndInit();
        }).dimensions(x0 + 104, by, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Daten löschen"), b -> {
            Store.clear();
            clearAndInit();
        }).dimensions(x0 + 258, by, 82, 20).build());
    }

    private int visibleRows() {
        return Math.max(1, (height - TOP - 34) / ROW_H);
    }

    private static double val(double d) {
        return Double.isNaN(d) ? 0 : d;
    }

    private void sortRows() {
        switch (sortMode) {
            case 0 -> rows.sort((a, b) -> Double.compare(val(b.chg), val(a.chg)));
            case 1 -> rows.sort((a, b) -> Double.compare(val(a.chg), val(b.chg)));
            case 2 -> rows.sort((a, b) -> a.id.compareTo(b.id));
            default -> rows.sort((a, b) -> Double.compare(b.avg, a.avg));
        }
    }

    private static String pretty(String id) {
        return id.replace('_', ' ');
    }

    private static String fmt(double v) {
        if (v >= 1e9) return String.format(Locale.US, "%.2fB", v / 1e9);
        if (v >= 1e6) return String.format(Locale.US, "%.2fM", v / 1e6);
        if (v >= 1e3) return String.format(Locale.US, "%.2fK", v / 1e3);
        return String.format(Locale.US, "%.2f", v);
    }

    private static String pct(double v) {
        return Double.isNaN(v) ? "-" : String.format(Locale.US, "%+.1f%%", v);
    }

    private static int color(double v) {
        if (Double.isNaN(v)) return 0xFF888888;
        if (v > 0.5) return 0xFF55FF55;
        if (v < -0.5) return 0xFFFF5555;
        return 0xFFAAAAAA;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int max = Math.max(0, rows.size() - visibleRows());
        scroll += verticalAmount > 0 ? -1 : 1;
        scroll = Math.max(0, Math.min(scroll, max));
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, title, width / 2, 10, 0xFFFFFFFF);

        String info = Store.snapshotCount() + " Scan(s)";
        if (Store.lastTime() > 0) {
            info += " | letzter: " + new SimpleDateFormat("dd.MM. HH:mm").format(new Date(Store.lastTime()));
        }
        if (OrderScanner.isRunning()) info += " | Scan läuft...";
        ctx.drawCenteredTextWithShadow(textRenderer, info, width / 2, 24, 0xFFAAAAAA);

        if (rows.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, "Noch keine Daten. Klicke \"Scan starten\" (oder Taste J).",
                    width / 2, height / 2 - 10, 0xFFFFFF55);
            ctx.drawCenteredTextWithShadow(textRenderer, "Für Trends brauchst du mindestens 2 Scans.",
                    width / 2, height / 2 + 4, 0xFFAAAAAA);
            return;
        }

        int hy = TOP - 14;
        ctx.drawTextWithShadow(textRenderer, "Item", x0, hy, 0xFFFFFF55);
        ctx.drawTextWithShadow(textRenderer, "Ø Preis", x0 + 112, hy, 0xFFFFFF55);
        ctx.drawTextWithShadow(textRenderer, "vs. zuletzt", x0 + 170, hy, 0xFFFFFF55);
        ctx.drawTextWithShadow(textRenderer, "vs. Ø5", x0 + 232, hy, 0xFFFFFF55);
        ctx.drawTextWithShadow(textRenderer, "Verlauf", x0 + 284, hy, 0xFFFFFF55);

        int vis = visibleRows();
        int max = Math.max(0, rows.size() - vis);
        scroll = Math.max(0, Math.min(scroll, max));

        for (int i = 0; i < vis; i++) {
            int idx = scroll + i;
            if (idx >= rows.size()) break;
            Store.Row r = rows.get(idx);
            int y = TOP + i * ROW_H;
            if (i % 2 == 0) ctx.fill(x0 - 4, y - 2, x0 + W, y + ROW_H - 2, 0x30FFFFFF);

            String name = textRenderer.trimToWidth(pretty(r.id), 104);
            ctx.drawTextWithShadow(textRenderer, name, x0, y, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer, fmt(r.avg), x0 + 112, y, 0xFFDDDDDD);
            ctx.drawTextWithShadow(textRenderer, pct(r.chg), x0 + 170, y, color(r.chg));
            ctx.drawTextWithShadow(textRenderer, pct(r.chg5), x0 + 232, y, color(r.chg5));
            drawSpark(ctx, r.hist, x0 + 284, y + 9);
        }

        ctx.drawCenteredTextWithShadow(textRenderer,
                (scroll + 1) + "-" + Math.min(rows.size(), scroll + vis) + " von " + rows.size(),
                width / 2, height - 38, 0xFF888888);
    }

    private void drawSpark(DrawContext ctx, List<Double> hist, int x, int baseY) {
        if (hist.size() < 2) return;
        double min = Double.MAX_VALUE, mx = 0;
        for (double v : hist) {
            min = Math.min(min, v);
            mx = Math.max(mx, v);
        }
        double range = mx - min;
        for (int i = 0; i < hist.size(); i++) {
            int h = range <= 0 ? 5 : 2 + (int) ((hist.get(i) - min) / range * 8);
            int col = (i > 0 && hist.get(i) >= hist.get(i - 1)) ? 0xFF55FF55 : 0xFFFF5555;
            if (i == 0) col = 0xFF888888;
            ctx.fill(x + i * 4, baseY - h, x + i * 4 + 3, baseY, col);
        }
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
