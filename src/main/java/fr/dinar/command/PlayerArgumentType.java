package fr.dinar.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import fr.dinar.DinarMod;
import fr.dinar.economy.Account;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class PlayerArgumentType implements ArgumentType<String> {

    public static PlayerArgumentType player() {
        return new PlayerArgumentType();
    }

    @Override
    public String parse(StringReader reader) throws CommandSyntaxException {
        String input = reader.readUnquotedString();
        if (input.isEmpty()) {
            throw new SimpleCommandExceptionType(Text.literal("Joueur requis")).createWithContext(reader);
        }
        if (DinarMod.economy != null && DinarMod.economy.getServer() != null) {
            String resolved = resolveName(input);
            if (resolved == null) {
                throw new SimpleCommandExceptionType(Text.literal("Joueur introuvable : §e" + input))
                        .createWithContext(reader);
            }
            return resolved;
        }
        return input;
    }

    @Override
    public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
        String remaining = builder.getRemainingLowerCase();
        List<String> matches = new ArrayList<>();
        for (String n : knownNames()) {
            if (n.toLowerCase(Locale.ROOT).startsWith(remaining)) matches.add(n);
        }
        matches.sort(Comparator.comparing(String::toLowerCase));
        int limit = Math.min(matches.size(), 50);
        for (int i = 0; i < limit; i++) {
            builder.suggest(matches.get(i));
        }
        return builder.buildFuture();
    }

    public static String getString(CommandContext<?> context, String name) {
        return context.getArgument(name, String.class);
    }

    private static String resolveName(String input) {
        List<String> names = knownNames();
        String lower = input.toLowerCase(Locale.ROOT);
        String exact = null;
        for (String n : names) {
            if (n.equalsIgnoreCase(input)) {
                if (exact != null && !exact.equalsIgnoreCase(n)) return null;
                exact = n;
            }
        }
        if (exact != null) return exact;
        List<String> prefixed = new ArrayList<>();
        for (String n : names) {
            if (n.toLowerCase(Locale.ROOT).startsWith(lower)) {
                boolean dup = false;
                for (String p : prefixed) {
                    if (p.equalsIgnoreCase(n)) {
                        dup = true;
                        break;
                    }
                }
                if (!dup) prefixed.add(n);
            }
        }
        return prefixed.size() == 1 ? prefixed.get(0) : null;
    }

    private static List<String> knownNames() {
        List<String> names = new ArrayList<>();
        if (DinarMod.economy == null) return names;
        MinecraftServer server = DinarMod.economy.getServer();
        if (server != null) {
            for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                names.add(p.getGameProfile().getName());
            }
        }
        for (Account a : DinarMod.economy.getAccounts()) {
            if (a.name != null) names.add(a.name);
        }
        return names;
    }
}
