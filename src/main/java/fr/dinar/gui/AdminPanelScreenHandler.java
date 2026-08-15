package fr.dinar.gui;

import com.mojang.authlib.GameProfile;
import fr.dinar.DinarMod;
import fr.dinar.command.SalaryCommand;
import fr.dinar.economy.Account;
import fr.dinar.economy.EconomyManager;
import fr.dinar.economy.SalaryEntry;
import fr.dinar.lang.DinarLang;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AdminPanelScreenHandler extends GenericContainerScreenHandler {
    private static final int SIZE = 54;
    private static final int PLAYERS_START = 27;
    private static final int PLAYERS_PER_PAGE = 27;

    private final SimpleInventory panelInv;
    private final ServerPlayerEntity admin;
    private int page = 0;
    private UUID selected;

    public AdminPanelScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity admin) {
        this(syncId, playerInventory, admin, new SimpleInventory(SIZE));
    }

    private AdminPanelScreenHandler(int syncId, PlayerInventory playerInventory, ServerPlayerEntity admin, SimpleInventory panelInv) {
        super(ScreenHandlerType.GENERIC_9X6, syncId, playerInventory, panelInv, 6);
        this.panelInv = panelInv;
        this.admin = admin;
        this.selected = admin.getUuid();
        build();
    }

    public static void open(ServerPlayerEntity admin) {
        admin.openHandledScreen(new SimpleNamedScreenHandlerFactory(
                (syncId, inv, player) -> new AdminPanelScreenHandler(syncId, inv, admin),
                Text.literal(DinarLang.t("Panel Dinar")).setStyle(Style.EMPTY.withColor(Formatting.GOLD).withItalic(false))));
    }

    // ------------------------------------------------------------------
    // Construction de l'interface
    // ------------------------------------------------------------------

    private void build() {
        for (int i = 0; i < SIZE; i++) {
            panelInv.setStack(i, ItemStack.EMPTY);
        }
        EconomyManager eco = DinarMod.economy;

        panelInv.setStack(0, named(Items.BARRIER, DinarLang.t("§cFermer")));
        panelInv.setStack(1, named(Items.ORANGE_DYE, DinarLang.t("§6Taxe du joueur +1%")));
        panelInv.setStack(2, named(Items.LIGHT_BLUE_DYE, DinarLang.t("§bTaxe du joueur -1%")));
        panelInv.setStack(3, named(Items.GREEN_DYE, DinarLang.t("§aSalaire +100")));
        panelInv.setStack(5, named(Items.RED_DYE, DinarLang.t("§cSalaire -100")));
        panelInv.setStack(6, named(Items.CLOCK, DinarLang.t("§ePayer tous les salaires")));
        panelInv.setStack(7, named(Items.COMPASS, DinarLang.t("§7Recharger la liste")));

        ItemStack treasury = named(Items.GOLD_BLOCK, DinarLang.t("§6Trésorerie"));
        treasury.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal("§7" + eco.money(eco.getTreasury())))));
        panelInv.setStack(4, treasury);

        panelInv.setStack(8, selectedInfo());

        panelInv.setStack(9, action(Items.EMERALD, "§a+100", 100));
        panelInv.setStack(10, action(Items.EMERALD, "§a+1 000", 1000));
        panelInv.setStack(11, action(Items.EMERALD, "§a+10 000", 10000));
        panelInv.setStack(12, action(Items.EMERALD, "§a+100 000", 100000));
        panelInv.setStack(13, action(Items.REDSTONE_BLOCK, "§c-100", -100));
        panelInv.setStack(14, action(Items.REDSTONE_BLOCK, "§c-1 000", -1000));
        panelInv.setStack(15, action(Items.REDSTONE_BLOCK, "§c-10 000", -10000));
        panelInv.setStack(16, action(Items.LAVA_BUCKET, "§4Mettre à 0", 0));

        int totalPages = Math.max(1, (int) Math.ceil(eco.accountCount() / (double) PLAYERS_PER_PAGE));
        panelInv.setStack(18, named(Items.ARROW, DinarLang.t("§ePage précédente")));
        panelInv.setStack(22, named(Items.BOOK, DinarLang.t("§7Page %s/%s", page + 1, totalPages)));
        panelInv.setStack(26, named(Items.ARROW, DinarLang.t("§ePage suivante")));

        List<Account> players = eco.baltop(page, PLAYERS_PER_PAGE);
        for (int i = 0; i < players.size(); i++) {
            panelInv.setStack(PLAYERS_START + i, playerHead(players.get(i)));
        }
    }

    private ItemStack selectedInfo() {
        EconomyManager eco = DinarMod.economy;
        if (selected == null) {
            return named(Items.PLAYER_HEAD, DinarLang.t("§7Aucun joueur sélectionné"));
        }
        Account a = eco.account(selected);
        if (a == null) {
            return named(Items.PLAYER_HEAD, DinarLang.t("§7Joueur introuvable"));
        }
        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        if (a.uuid != null) {
            head.set(DataComponentTypes.PROFILE, new ProfileComponent(new GameProfile(a.uuid, a.name)));
        }
        head.set(DataComponentTypes.CUSTOM_NAME, Text.literal("§6★ " + a.name).setStyle(Style.EMPTY.withItalic(false)));
        Double tax = eco.getPersonalTax(a.uuid);
        SalaryEntry salary = eco.getSalary(a.uuid);
        head.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal(DinarLang.t("§7Solde : §e%s", eco.money(a.balance))),
                Text.literal(DinarLang.t("§7Taxe : §e%s", tax != null ? (int) (tax * 100) + "%" : DinarLang.t("aucune"))),
                Text.literal(DinarLang.t("§7Salaire : §e%s", salary != null
                        ? eco.money(salary.amount) + " / " + SalaryCommand.formatInterval(salary.intervalSeconds)
                        : DinarLang.t("aucun"))),
                Text.literal(DinarLang.t("§7Clic sur un joueur pour le sélectionner")))));
        head.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return head;
    }

    private ItemStack playerHead(Account a) {
        EconomyManager eco = DinarMod.economy;
        boolean isSelected = selected != null && selected.equals(a.uuid);
        String name = a.name != null ? a.name : DinarLang.t("Inconnu");

        ItemStack head = new ItemStack(Items.PLAYER_HEAD);
        if (a.uuid != null) {
            head.set(DataComponentTypes.PROFILE, new ProfileComponent(new GameProfile(a.uuid, a.name)));
        }
        head.set(DataComponentTypes.CUSTOM_NAME,
                Text.literal((isSelected ? "§6★ " : "§e") + name).setStyle(Style.EMPTY.withItalic(false)));

        Double tax = eco.getPersonalTax(a.uuid);
        SalaryEntry salary = eco.getSalary(a.uuid);
        double bal = a.balance;
        List<Text> lore = new ArrayList<>();
        lore.add(Text.literal(DinarLang.t("§7Solde : §e%s", eco.money(bal))));
        lore.add(Text.literal(DinarLang.t("§7Taxe : §e%s", tax != null ? (int) (tax * 100) + "%" : DinarLang.t("aucune"))));
        lore.add(Text.literal(DinarLang.t("§7Salaire : §e%s", salary != null
                ? eco.money(salary.amount) + " / " + SalaryCommand.formatInterval(salary.intervalSeconds)
                : DinarLang.t("aucun"))));
        lore.add(Text.literal(isSelected ? DinarLang.t("§aJoueur sélectionné") : DinarLang.t("§7Clic pour sélectionner")));
        head.set(DataComponentTypes.LORE, new LoreComponent(lore));

        if (isSelected) {
            head.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return head;
    }

    private static ItemStack named(Item item, String name) {
        ItemStack s = new ItemStack(item);
        s.set(DataComponentTypes.CUSTOM_NAME, Text.literal(name).setStyle(Style.EMPTY.withItalic(false)));
        return s;
    }

    private static ItemStack action(Item item, String name, long value) {
        ItemStack s = named(item, name);
        s.set(DataComponentTypes.LORE, new LoreComponent(List.of(
                Text.literal(DinarLang.t("§7Applique %s au joueur sélectionné",
                        value == 0 ? DinarLang.t("la remise à zéro") : (value > 0 ? "+" : "") + value + " D")))));
        return s;
    }

    // ------------------------------------------------------------------
    // Clics
    // ------------------------------------------------------------------

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        if (player instanceof ServerPlayerEntity sp && sp.getUuid().equals(admin.getUuid())) {
            if (slotIndex >= 0 && slotIndex < SIZE) {
                handleClick(slotIndex);
            }
        }
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return ItemStack.EMPTY;
    }

    private void handleClick(int slot) {
        EconomyManager eco = DinarMod.economy;
        switch (slot) {
            case 0 -> {
                admin.closeHandledScreen();
                return;
            }
            case 1 -> {
                if (selected != null) {
                    double t = eco.hasPersonalTax(selected) ? eco.getPersonalTax(selected) : 0;
                    eco.setPersonalTax(selected, t + 0.01);
                    admin.sendMessage(DinarLang.text("§aTaxe de %s : §e%s", eco.accountName(selected),
                            (int) (eco.getPersonalTax(selected) * 100) + "%"), false);
                }
            }
            case 2 -> {
                if (selected != null) {
                    double t = eco.hasPersonalTax(selected) ? eco.getPersonalTax(selected) : 0;
                    eco.setPersonalTax(selected, t - 0.01);
                    admin.sendMessage(DinarLang.text("§aTaxe de %s : §e%s", eco.accountName(selected),
                            eco.hasPersonalTax(selected) ? (int) (eco.getPersonalTax(selected) * 100) + "%" : DinarLang.t("aucune")), false);
                }
            }
            case 3 -> {
                if (selected != null) {
                    SalaryEntry s = eco.getSalary(selected);
                    double amount = s != null ? s.amount + 100 : 100;
                    long interval = s != null ? s.intervalSeconds : 3600;
                    eco.setSalary(selected, amount, interval);
                    admin.sendMessage(DinarLang.text("§aSalaire de %s : §e%s", eco.accountName(selected),
                            eco.money(eco.getSalary(selected).amount)), false);
                }
            }
            case 5 -> {
                if (selected != null) {
                    SalaryEntry s = eco.getSalary(selected);
                    if (s != null) {
                        double amount = s.amount - 100;
                        if (amount <= 0) {
                            eco.removeSalary(selected);
                            admin.sendMessage(DinarLang.text("§aSalaire de %s supprimé.", eco.accountName(selected)), false);
                        } else {
                            eco.setSalary(selected, amount, s.intervalSeconds);
                            admin.sendMessage(DinarLang.text("§aSalaire de %s : §e%s", eco.accountName(selected),
                                    eco.money(eco.getSalary(selected).amount)), false);
                        }
                    } else {
                        admin.sendMessage(DinarLang.text("§c%s n'a pas de salaire.", eco.accountName(selected)), false);
                    }
                }
            }
            case 6 -> {
                int n = eco.payAllSalaries();
                admin.sendMessage(DinarLang.text("§a%s salaire(s) payé(s).", n), false);
            }
            case 7 -> admin.sendMessage(DinarLang.text("§7Liste rechargée."), false);
            case 9 -> applyToSelected(eco, 100);
            case 10 -> applyToSelected(eco, 1000);
            case 11 -> applyToSelected(eco, 10000);
            case 12 -> applyToSelected(eco, 100000);
            case 13 -> applyToSelected(eco, -100);
            case 14 -> applyToSelected(eco, -1000);
            case 15 -> applyToSelected(eco, -10000);
            case 16 -> {
                if (selected != null) {
                    eco.setBalance(selected, eco.accountName(selected), 0);
                    admin.sendMessage(DinarLang.text("§aSolde de %s remis à 0.", eco.accountName(selected)), false);
                }
            }
            case 18 -> page = Math.max(0, page - 1);
            case 26 -> {
                int totalPages = Math.max(1, (int) Math.ceil(eco.accountCount() / (double) PLAYERS_PER_PAGE));
                page = Math.min(totalPages - 1, page + 1);
            }
            default -> {
                if (slot >= PLAYERS_START) {
                    int index = slot - PLAYERS_START;
                    List<Account> players = eco.baltop(page, PLAYERS_PER_PAGE);
                    if (index < players.size()) {
                        selected = players.get(index).uuid;
                    }
                }
            }
        }
        build();
        syncState();
    }

    private void applyToSelected(EconomyManager eco, long value) {
        if (selected == null) {
            admin.sendMessage(DinarLang.text("§cSélectionnez d'abord un joueur."), false);
            return;
        }
        if (value > 0) {
            eco.add(selected, eco.accountName(selected), value);
        } else if (value < 0) {
            eco.take(selected, eco.accountName(selected), -value);
        }
        admin.sendMessage(DinarLang.text("§a%s D §f→ §e%s §7(solde : §e%s§7)",
                (value >= 0 ? "+" : "") + value, eco.accountName(selected), eco.money(eco.balance(selected))), false);
    }
}
