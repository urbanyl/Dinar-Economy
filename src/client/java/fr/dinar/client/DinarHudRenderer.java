package fr.dinar.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class DinarHudRenderer implements HudRenderCallback {

    private static final int BG_COLOR = 0xB8121220;
    private static final int BORDER_COLOR = 0xFFD4AF37;
    private static final int WALLET_COLOR = 0xFFF2C94C;
    private static final int BANK_COLOR = 0xFF56CCF2;
    private static final int COMPANY_COLOR = 0xFF6FCF97;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        if (client.options.hudHidden) return;
        if (client.currentScreen != null) return;

        TextRenderer textRenderer = client.textRenderer;

        String symbol = DinarClientData.getCurrencySymbol();
        String walletText = formatMoney(DinarClientData.getWalletBalance(), symbol);
        String bankText = formatMoney(DinarClientData.getBankBalance(), symbol);
        String companyText = formatMoney(DinarClientData.getCompanyBalance(), symbol);
        boolean hasCompany = DinarClientData.getHasCompany();

        int walletWidth = textRenderer.getWidth(walletText);
        int bankWidth = textRenderer.getWidth(bankText);
        int companyWidth = textRenderer.getWidth(companyText);

        int iconSize = 6;
        int iconGap = 4;
        int padX = 8;
        int padY = 5;
        int rowGap = 2;
        int rowHeight = 9;

        int rows = hasCompany ? 3 : 2;
        int contentWidth = Math.max(walletWidth, Math.max(bankWidth, hasCompany ? companyWidth : 0));
        int panelWidth = padX * 2 + iconSize + iconGap + contentWidth;
        int panelHeight = padY * 2 + rows * rowHeight + (rows - 1) * rowGap;

        int screenWidth = client.getWindow().getScaledWidth();
        int margin = 4;
        int x = screenWidth - panelWidth - margin;
        int y = margin;

        context.fill(x - 1, y - 1, x + panelWidth + 1, y + panelHeight + 1, BORDER_COLOR);
        context.fill(x, y, x + panelWidth, y + panelHeight, BG_COLOR);

        int textX = x + padX + iconSize + iconGap;
        int iconX = x + padX;
        int rowY = y + padY;

        drawIcon(context, iconX, rowY, WALLET_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal(walletText), textX, rowY, WALLET_COLOR);
        rowY += rowHeight + rowGap;

        drawIcon(context, iconX, rowY, BANK_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal(bankText), textX, rowY, BANK_COLOR);
        if (hasCompany) {
            rowY += rowHeight + rowGap;
            drawIcon(context, iconX, rowY, COMPANY_COLOR);
            context.drawTextWithShadow(textRenderer, Text.literal(companyText), textX, rowY, COMPANY_COLOR);
        }
    }

    private void drawIcon(DrawContext context, int x, int y, int color) {
        int s = 6;
        context.fill(x + 1, y, x + s - 1, y + 1, color);
        context.fill(x, y + 1, x + s, y + s - 1, color);
        context.fill(x + 1, y + s, x + s - 1, y + s + 1, color);
    }

    private String formatMoney(double value, String symbol) {
        double abs = Math.abs(value);
        String formatted;
        if (abs >= 1_000_000) {
            formatted = String.format("%.1fM", value / 1_000_000);
        } else if (abs >= 1_000) {
            formatted = String.format("%.1fK", value / 1_000);
        } else {
            formatted = String.format("%.0f", value);
        }
        return formatted + " " + symbol;
    }
}
