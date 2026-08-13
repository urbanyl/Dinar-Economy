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

        String walletText = formatMoney(getWalletBalance(client));
        String bankText = formatMoney(getBankBalance(client));

        String labelWallet = "§7💰 Portefeuille";
        String labelBank = "§7🏦 Banque";
        String valueWallet = "§e" + walletText;
        String valueBank = "§b" + bankText;

        int labelWalletWidth = textRenderer.getWidth(stripFormatting(labelWallet));
        int valueWalletWidth = textRenderer.getWidth(stripFormatting(valueWallet));
        int labelBankWidth = textRenderer.getWidth(stripFormatting(labelBank));
        int valueBankWidth = textRenderer.getWidth(stripFormatting(valueBank));

        int walletBlockWidth = Math.max(labelWalletWidth, valueWalletWidth) + 12;
        int bankBlockWidth = Math.max(labelBankWidth, valueBankWidth) + 12;
        int gap = 8;
        int totalWidth = walletBlockWidth + gap + bankBlockWidth;
        int panelHeight = 32;

        int screenWidth = client.getWindow().getScaledWidth();
        int screenHeight = client.getWindow().getScaledHeight();
        int x = (screenWidth - totalWidth) / 2;
        int y = screenHeight - panelHeight - 5;

        context.fill(x - 2, y - 2, x + totalWidth + 2, y + panelHeight + 2, BORDER_COLOR);
        context.fill(x, y, x + totalWidth, y + panelHeight, BG_COLOR);

        int walletX = x + 6;
        int bankX = x + walletBlockWidth + gap + 6;

        context.drawTextWithShadow(textRenderer, Text.literal(labelWallet), walletX, y + 4, LABEL_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal(valueWallet), walletX, y + 16, WALLET_COLOR);

        context.fill(x + walletBlockWidth + gap / 2, y + 4, x + walletBlockWidth + gap / 2 + 1, y + panelHeight - 4, SEPARATOR_COLOR);

        context.drawTextWithShadow(textRenderer, Text.literal(labelBank), bankX, y + 4, LABEL_COLOR);
        context.drawTextWithShadow(textRenderer, Text.literal(valueBank), bankX, y + 16, BANK_COLOR);
    }

    private double getWalletBalance(MinecraftClient client) {
        if (client.player == null) return 0;
        try {
            return fr.dinar.DinarMod.economy.balance(client.player.getUuid());
        } catch (Exception e) {
            return 0;
        }
    }

    private double getBankBalance(MinecraftClient client) {
        if (client.player == null) return 0;
        try {
            return fr.dinar.DinarMod.economy.bankBalance(client.player.getUuid());
        } catch (Exception e) {
            return 0;
        }
    }

    private String formatMoney(double value) {
        try {
            return fr.dinar.DinarMod.economy.format(value) + " " + fr.dinar.DinarMod.config.currencySymbol;
        } catch (Exception e) {
            return String.format("%.2f D", value);
        }
    }

    private String stripFormatting(String text) {
        return text.replaceAll("§[0-9a-fk-or]", "");
    }
}
