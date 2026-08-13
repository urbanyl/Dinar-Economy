package fr.dinar.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public class DinarHudRenderer implements HudRenderCallback {

    private static final int BG_COLOR = 0xCC1A1A2E;
    private static final int BORDER_COLOR = 0xFFD4AF37;
    private static final int WALLET_COLOR = 0xFFD4AF37;
    private static final int BANK_COLOR = 0xFF5BC0EB;
    private static final int LABEL_COLOR = 0xFFAAAAAA;
    private static final int SEPARATOR_COLOR = 0xFF555555;

    @Override
    public void onHudRender(DrawContext context, RenderTickCounter tickCounter) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null || client.player == null) return;
        if (client.options.hudHidden) return;
        if (client.currentScreen != null) return;

        TextRenderer textRenderer = client.textRenderer;

        double wallet = DinarClientData.getWalletBalance();
        double bank = DinarClientData.getBankBalance();
        String symbol = DinarClientData.getCurrencySymbol();

        String walletText = formatMoney(wallet, symbol);
        String bankText = formatMoney(bank, symbol);

        String labelWallet = "§7\ue142 Portefeuille";
        String labelBank = "§7\ue144 Banque";
        String valueWallet = "§e" + walletText;
        String valueBank = "§b" + bankText;

        int labelWalletWidth = textRenderer.getWidth(stripFormatting(labelWallet));
        int valueWalletWidth = textRenderer.getWidth(stripFormatting(valueWallet));
        int labelBankWidth = textRenderer.getWidth(stripFormatting(labelBank));
        int valueBankWidth = textRenderer.getWidth(stripFormatting(valueBank));

        int walletBlockWidth = Math.max(labelWalletWidth, valueWalletWidth) + 16;
        int bankBlockWidth = Math.max(labelBankWidth, valueBankWidth) + 16;
        int gap = 10;
        int totalWidth = walletBlockWidth + gap + bankBlockWidth;
        int panelHeight = 34;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = (screenWidth - totalWidth) / 2;
        int y = screenHeight - panelHeight - 6;

        context.fill(x - 2, y - 2, x + totalWidth + 2, y + panelHeight + 2, BORDER_COLOR);
        context.fill(x, y, x + totalWidth, y + panelHeight, BG_COLOR);

        int walletX = x + 8;
        int bankX = x + walletBlockWidth + gap + 8;

        drawIcon(context, walletX - 2, y + 3, 0xFFD4AF37);
        context.drawTextWithShadow(textRenderer, Text.literal(labelWallet), walletX + 8, y + 4, LABEL_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal(valueWallet), walletX + 8, y + 17, WALLET_COLOR);

        context.fill(x + walletBlockWidth + gap / 2, y + 4, x + walletBlockWidth + gap / 2 + 1, y + panelHeight - 4, SEPARATOR_COLOR);

        drawIcon(context, bankX - 2, y + 3, 0xFF5BC0EB);
        context.drawTextWithShadow(textRenderer, Text.literal(labelBank), bankX + 8, y + 4, LABEL_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal(valueBank), bankX + 8, y + 17, BANK_COLOR);
    }

    private void drawIcon(DrawContext context, int x, int y, int color) {
        int s = 8;
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

    private String stripFormatting(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "").replaceAll("\\\\u[0-9a-fA-F]{4}", "");
    }
}
