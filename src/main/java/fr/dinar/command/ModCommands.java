package fr.dinar.command;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public final class ModCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
                                CommandRegistryAccess registryAccess,
                                CommandManager.RegistrationEnvironment environment) {
        BalanceCommand.register(dispatcher);
        PayCommand.register(dispatcher);
        RequestCommand.register(dispatcher);
        BaltopCommand.register(dispatcher);
        EcoCommand.register(dispatcher);
        SalaryCommand.register(dispatcher);
        TaxCommand.register(dispatcher);
        DinarCommand.register(dispatcher);
        CaliphatCommand.register(dispatcher);
        LoiCommand.register(dispatcher);
        BankCommand.register(dispatcher);
        LoanCommand.register(dispatcher);
        ShopCommand.register(dispatcher);
        CompanyCommand.register(dispatcher);
        AuctionCommand.register(dispatcher);
        ContractCommand.register(dispatcher);
        AmendeCommand.register(dispatcher);
        CourrierCommand.register(dispatcher);
        DossierCommand.register(dispatcher);
        PoliceCommand.register(dispatcher);
        PrisonCommand.register(dispatcher);
        JournalCommand.register(dispatcher);
        RegisterCommand.register(dispatcher);
        IdentiteCommand.register(dispatcher);
        CarteCommand.register(dispatcher);
    }

    private ModCommands() {}
}
