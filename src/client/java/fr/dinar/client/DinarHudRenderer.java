package fr.dinar.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

public class DinarHudRenderer implements HudRenderCallback {

    private static final int PANEL_BG = 0xB0000000;
    private static final int PANEL_BORDER = 0x33FFFFFF;
    private static final int CASH_GREEN = 0xFF41E01F;
    private static final int BANK_BLUE = 0xFF56CCF2;
    private static final int COMPANY_GREEN = 0xFF2ECC71;
    private static final int WALLET_TEXT = 0xFFE9FFE2;
    private static final int BANK_TEXT = 0xFF7ED0FF;
    private static final int COMPANY_TEXT = 0xFF8BFFB0;
    private static final int FLASH_MS = 250;
    private static final int MARGIN = 4;

    private long lastWalletBits = Long.MIN_VALUE;
    private long lastBankBits = Long.MIN_VALUE;
    private long walletFlashMs = 0;
    private long bankFlashMs = 0;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        if (client.options.hudHidden) return;
        if (client.currentScreen != null) return;

        TextRenderer textRenderer = client.textRenderer;
        String symbol = DinarClientData.getCurrencySymbol();
        double wallet = DinarClientData.getWalletBalance();
        double bank = DinarClientData.getBankBalance();
        boolean hasCompany = DinarClientData.getHasCompany();

        long now = System.currentTimeMillis();
        long walletBits = Double.doubleToLongBits(wallet);
        if (walletBits != lastWalletBits) {
            lastWalletBits = walletBits;
            walletFlashMs = now;
        }
        long bankBits = Double.doubleToLongBits(bank);
        if (bankBits != lastBankBits) {
            lastBankBits = bankBits;
            bankFlashMs = now;
        }

        String walletText = formatMoney(wallet, symbol);
        String bankText = formatMoney(bank, symbol);
        String companyText = hasCompany ? formatMoney(DinarClientData.getCompanyBalance(), symbol) : "";

        int iconSize = 10;
        int iconGap = 5;
        int padX = 8;
        int padY = 5;
        int rowHeight = 11;
        int rowGap = 1;

        int rows = hasCompany ? 3 : 2;
        int contentWidth = Math.max(textRenderer.getWidth(walletText),
                Math.max(textRenderer.getWidth(bankText), hasCompany ? textRenderer.getWidth(companyText) : 0));
        int panelWidth = padX * 2 + iconSize + iconGap + contentWidth;
        int panelHeight = padY * 2 + rows * rowHeight + (rows - 1) * rowGap;

        int x = MARGIN;
        int y = MARGIN;

        context.fill(x - 1, y - 1, x + panelWidth + 1, y + panelHeight + 1, PANEL_BORDER);
        context.fill(x, y, x + panelWidth, y + panelHeight, PANEL_BG);

        int textX = x + padX + iconSize + iconGap;
        int iconX = x + padX;
        int rowY = y + padY;

        drawIcon(context, iconX, rowY, CASH_GREEN);
        context.drawTextWithShadow(textRenderer, Text.literal(walletText), textX, rowY,
                now - walletFlashMs < FLASH_MS ? 0xFFFFFFFF : WALLET_TEXT);
        rowY += rowHeight + rowGap;

        drawIcon(context, iconX, rowY, BANK_BLUE);
        context.drawTextWithShadow(textRenderer, Text.literal(bankText), textX, rowY,
                now - bankFlashMs < FLASH_MS ? 0xFFFFFFFF : BANK_TEXT);

        if (hasCompany) {
            rowY += rowHeight + rowGap;
            drawIcon(context, iconX, rowY, COMPANY_GREEN);
            context.drawTextWithShadow(textRenderer, Text.literal(companyText), textX, rowY, COMPANY_TEXT);
        }

        renderGainNotifications(context, textRenderer, symbol, x, y + panelHeight + 6);
    }

    private void renderGainNotifications(DrawContext context, TextRenderer textRenderer, String symbol, int x, int y) {
        DinarClientData.cleanupGains();
        List<MoneyGainEvent> gains = DinarClientData.getMoneyGains();
        if (gains.isEmpty()) return;

        int rowHeight = 12;
        int index = 0;
        for (MoneyGainEvent gain : gains) {
            float alpha = gain.alpha();
            if (alpha <= 0) continue;
            int color = "Banque".equals(gain.label) ? BANK_BLUE : CASH_GREEN;
            color = withAlpha(color, alpha);
            String text = "+" + formatMoney(gain.amount, symbol);
            int textX = x;
            int textY = y + index * rowHeight + (int) (gain.slide() * 6);
            int width = textRenderer.getWidth(text) + 10;
            context.fill(textX, textY - 1, textX + width, textY + 9, withAlpha(0xCC000000, alpha));
            context.drawTextWithShadow(textRenderer, Text.literal(text).withColor(color), textX + 5, textY, color);
            index++;
        }
    }

    private static int withAlpha(int argb, float alpha) {
        int a = (int) (alpha * 255) & 0xFF;
        return (argb & 0x00FFFFFF) | (a << 24);
    }

    private void drawIcon(DrawContext context, int x, int y, int color) {
        int s = 10;
        context.fill(x + 2, y, x + s - 2, y + 1, color);
        context.fill(x, y + 1, x + s, y + s - 1, color);
        context.fill(x + 2, y + s, x + s - 2, y + s + 1, color);
    }

    private String formatMoney(double value, String symbol) {
        return String.format(Locale.ROOT, "%,.0f", value) + " " + symbol;
    }
}
