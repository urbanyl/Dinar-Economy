package fr.dinar.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.List;
import java.util.Locale;

public class DinarHudRenderer implements HudRenderCallback {

    private static final int PANEL_BG = 0xB0000000;
    private static final int PANEL_BORDER = 0x33FFFFFF;
    private static final int WALLET_COLOR = 0xFFF2C94C;
    private static final int BANK_COLOR = 0xFF2ECC71;
    private static final int COMPANY_COLOR = 0xFF56CCF2;
    private static final int WALLET_TEXT = 0xFFFFF4C9;
    private static final int BANK_TEXT = 0xFF8BFFB0;
    private static final int COMPANY_TEXT = 0xFF7ED0FF;
    private static final int FLASH_MS = 250;
    private static final int MARGIN = 4;
    private static final int ICON_W = 16;
    private static final int ICON_H = 16;
    private static final int ICON_GAP = 6;
    private static final int PAD_X = 8;
    private static final int PAD_Y = 5;
    private static final int ROW_HEIGHT = 16;
    private static final int ROW_GAP = 4;

    private static final Identifier WALLET_ICON = Identifier.of("dinar", "textures/hud/wallet.png");
    private static final Identifier BANK_ICON = Identifier.of("dinar", "textures/hud/bank.png");
    private static final Identifier COMPANY_ICON = Identifier.of("dinar", "textures/hud/company.png");

    private long lastWalletBits = Long.MIN_VALUE;
    private long lastBankBits = Long.MIN_VALUE;
    private long walletFlashMs = 0;
    private long bankFlashMs = 0;
    private boolean flashInitialized = false;

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
        long bankBits = Double.doubleToLongBits(bank);
        if (!flashInitialized) {
            flashInitialized = true;
            lastWalletBits = walletBits;
            lastBankBits = bankBits;
        } else {
            if (walletBits != lastWalletBits) {
                lastWalletBits = walletBits;
                walletFlashMs = now;
            }
            if (bankBits != lastBankBits) {
                lastBankBits = bankBits;
                bankFlashMs = now;
            }
        }

        String walletText = formatMoney(wallet, symbol);
        String bankText = formatMoney(bank, symbol);
        String companyText = hasCompany ? formatMoney(DinarClientData.getCompanyBalance(), symbol) : "";

        int rows = hasCompany ? 3 : 2;
        int contentWidth = Math.max(textRenderer.getWidth(walletText),
                Math.max(textRenderer.getWidth(bankText), hasCompany ? textRenderer.getWidth(companyText) : 0));
        int panelWidth = PAD_X * 2 + ICON_W + ICON_GAP + contentWidth;
        int panelHeight = PAD_Y * 2 + rows * ROW_HEIGHT + (rows - 1) * ROW_GAP;

        int x = MARGIN;
        int y = MARGIN;

        context.fill(x - 1, y - 1, x + panelWidth + 1, y + panelHeight + 1, PANEL_BORDER);
        context.fill(x, y, x + panelWidth, y + panelHeight, PANEL_BG);

        int textX = x + PAD_X + ICON_W + ICON_GAP;
        int iconX = x + PAD_X;
        int rowY = y + PAD_Y;

        context.drawTexture(WALLET_ICON, iconX, rowY, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
        context.drawTextWithShadow(textRenderer, Text.literal(walletText), textX, rowY + 4,
                now - walletFlashMs < FLASH_MS ? 0xFFFFFFFF : WALLET_TEXT);
        rowY += ROW_HEIGHT + ROW_GAP;

        context.drawTexture(BANK_ICON, iconX, rowY, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
        context.drawTextWithShadow(textRenderer, Text.literal(bankText), textX, rowY + 4,
                now - bankFlashMs < FLASH_MS ? 0xFFFFFFFF : BANK_TEXT);

        if (hasCompany) {
            rowY += ROW_HEIGHT + ROW_GAP;
            context.drawTexture(COMPANY_ICON, iconX, rowY, 0, 0, ICON_W, ICON_H, ICON_W, ICON_H);
            context.drawTextWithShadow(textRenderer, Text.literal(companyText), textX, rowY + 4, COMPANY_TEXT);
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
            int color = "Banque".equals(gain.label) ? BANK_COLOR : WALLET_COLOR;
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

    private String formatMoney(double value, String symbol) {
        return String.format(Locale.ROOT, "%,.0f", value) + " " + symbol;
    }
}
