package fr.dinar.placeholder;

import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import fr.dinar.DinarMod;
import fr.dinar.lang.DinarLang;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.Locale;

public final class DinarPlaceholders {

    public static void register() {
        Placeholders.register(Identifier.of("dinar", "balance"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player");
            return PlaceholderResult.value(Text.literal(DinarMod.economy.money(DinarMod.economy.balance(ctx.player().getUuid()))));
        });

        Placeholders.register(Identifier.of("dinar", "balance_formatted"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player");
            return PlaceholderResult.value(Text.literal(DinarMod.economy.money(DinarMod.economy.balance(ctx.player().getUuid()))));
        });

        Placeholders.register(Identifier.of("dinar", "balance_raw"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player");
            return PlaceholderResult.value(Text.literal(String.format(Locale.US, "%.2f", DinarMod.economy.balance(ctx.player().getUuid()))));
        });

        Placeholders.register(Identifier.of("dinar", "balance_int"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player");
            return PlaceholderResult.value(Text.literal(String.valueOf((long) DinarMod.economy.balance(ctx.player().getUuid()))));
        });

        Placeholders.register(Identifier.of("dinar", "rank"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player");
            return PlaceholderResult.value(Text.literal("#" + DinarMod.economy.rank(ctx.player().getUuid())));
        });

        Placeholders.register(Identifier.of("dinar", "treasury"), (ctx, arg) ->
                PlaceholderResult.value(Text.literal(DinarMod.economy.money(DinarMod.economy.getTreasury()))));

        Placeholders.register(Identifier.of("dinar", "currency"), (ctx, arg) ->
                PlaceholderResult.value(Text.literal(DinarMod.config.currencySymbol)));

        Placeholders.register(Identifier.of("dinar", "currency_name"), (ctx, arg) ->
                PlaceholderResult.value(Text.literal(DinarMod.config.currencyName)));

        Placeholders.register(Identifier.of("dinar", "leader"), (ctx, arg) ->
                PlaceholderResult.value(Text.literal(DinarMod.government.hasLeader()
                        ? DinarMod.government.getLeaderName() : DinarLang.t("Aucun"))));

        Placeholders.register(Identifier.of("dinar", "laws"), (ctx, arg) ->
                PlaceholderResult.value(Text.literal(String.valueOf(DinarMod.government.getAdoptedLawCount()))));

        Placeholders.register(Identifier.of("dinar", "decree"), (ctx, arg) -> {
            String d = DinarMod.government.getDecree();
            return PlaceholderResult.value(Text.literal(d != null && !d.isEmpty() ? d : DinarLang.t("Aucun")));
        });

        Placeholders.register(Identifier.of("dinar", "is_leader"), (ctx, arg) -> {
            if (!ctx.hasPlayer()) return PlaceholderResult.invalid("No player");
            return PlaceholderResult.value(Text.literal(
                    DinarMod.government.isLeader(ctx.player().getUuid()) ? "true" : "false"));
        });
    }

    private DinarPlaceholders() {}
}
