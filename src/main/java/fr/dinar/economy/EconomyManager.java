package fr.dinar.economy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import fr.dinar.DinarMod;
import fr.dinar.config.DinarConfig;
import fr.dinar.scoreboard.BalanceScoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.mojang.authlib.GameProfile;

public class EconomyManager {
    private final Map<UUID, Account> accounts = new HashMap<>();
    private final Map<UUID, Double> personalTaxes = new HashMap<>();
    private final Map<UUID, SalaryEntry> salaries = new HashMap<>();
    private final Map<UUID, Double> bankBalances = new HashMap<>();
    private final Map<UUID, LoanEntry> loans = new HashMap<>();
    private final RequestManager requests = new RequestManager();
    private final BalanceScoreboard scoreboard = new BalanceScoreboard();

    private MinecraftServer server;
    private Path dataFile;
    private double treasury = 0;
    private double globalTransactionTax;
    private double salaryTax;
    private long lastAutoSaveTick = 0;
    private long lastSalaryTick = 0;
    private long lastScoreboardTick = 0;

    // ------------------------------------------------------------------
    // Cycle de vie
    // ------------------------------------------------------------------

    public void onServerStart(MinecraftServer server) {
        this.server = server;
        this.dataFile = server.getSavePath(WorldSavePath.ROOT).resolve("dinar").resolve("data.json");
        accounts.clear();
        personalTaxes.clear();
        salaries.clear();
        treasury = 0;
        globalTransactionTax = DinarMod.config.globalTransactionTax;
        salaryTax = DinarMod.config.salaryTax;
        load();
        DinarMod.LOGGER.info("[Dinar] Économie chargée : {} comptes, trésorerie {}", accounts.size(), money(treasury));
        if (DinarMod.config.scoreboard.enabled) {
            scoreboard.start(server);
        }
    }

    public void onServerStop(MinecraftServer server) {
        save();
        scoreboard.stop();
        this.server = null;
    }

    public void onPlayerJoin(ServerPlayerEntity player) {
        getOrCreate(player.getUuid(), player.getGameProfile().getName());
        if (DinarMod.config.scoreboard.enabled) {
            scoreboard.update(this);
        }
    }

    public void tick(MinecraftServer server) {
        long t = server.getTicks();
        DinarConfig cfg = DinarMod.config;
        if (t - lastSalaryTick >= cfg.salaryCheckIntervalTicks) {
            lastSalaryTick = t;
            checkSalaries();
        }
        if (t - lastAutoSaveTick >= cfg.autoSaveIntervalTicks) {
            lastAutoSaveTick = t;
            save();
        }
        if (t - lastScoreboardTick >= cfg.scoreboard.updateIntervalTicks) {
            lastScoreboardTick = t;
            scoreboard.update(this);
        }
        if (t % 1200 == 0) {
            requests.purge();
        }
    }

    // ------------------------------------------------------------------
    // Comptes
    // ------------------------------------------------------------------

    public Account getOrCreate(UUID uuid, String name) {
        return accounts.computeIfAbsent(uuid, id -> {
            Account a = new Account(id, name, DinarMod.config.startingBalance);
            return a;
        });
    }

    public Account account(UUID uuid) {
        return accounts.get(uuid);
    }

    public double balance(UUID uuid) {
        return getOrCreate(uuid, null).balance;
    }

    public double add(UUID uuid, String name, double amount) {
        Account a = getOrCreate(uuid, name);
        a.balance = round(a.balance + amount);
        return a.balance;
    }

    public double take(UUID uuid, String name, double amount) {
        Account a = getOrCreate(uuid, name);
        double actual = DinarMod.config.allowNegative ? amount : Math.min(amount, a.balance);
        a.balance = round(a.balance - actual);
        return actual;
    }

    public void setBalance(UUID uuid, String name, double amount) {
        Account a = getOrCreate(uuid, name);
        a.balance = round(amount);
    }

    public void resetAll() {
        for (Account a : accounts.values()) {
            a.balance = round(DinarMod.config.startingBalance);
        }
    }

    public int accountCount() {
        return accounts.size();
    }

    public double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

    // ------------------------------------------------------------------
    // Trésorerie
    // ------------------------------------------------------------------

    public double getTreasury() {
        return treasury;
    }

    public void addTreasury(double amount) {
        treasury = round(treasury + amount);
    }

    public void takeTreasury(double amount) {
        treasury = round(Math.max(0, treasury - amount));
    }

    // ------------------------------------------------------------------
    // Taxes
    // ------------------------------------------------------------------

    public double getGlobalTransactionTax() {
        return globalTransactionTax;
    }

    public void setGlobalTransactionTax(double rate) {
        globalTransactionTax = clamp01(rate);
    }

    public double getSalaryTax() {
        return salaryTax;
    }

    public void setSalaryTax(double rate) {
        salaryTax = clamp01(rate);
    }

    public Double getPersonalTax(UUID uuid) {
        return personalTaxes.get(uuid);
    }

    public Map<UUID, Double> getPersonalTaxes() {
        return personalTaxes;
    }

    public boolean hasPersonalTax(UUID uuid) {
        return personalTaxes.containsKey(uuid);
    }

    public void setPersonalTax(UUID uuid, double rate) {
        rate = clamp01(rate);
        if (rate <= 0) {
            personalTaxes.remove(uuid);
        } else {
            personalTaxes.put(uuid, round4(rate));
        }
    }

    public double effectiveTax(UUID recipient) {
        Double personal = personalTaxes.get(recipient);
        return personal != null ? personal : globalTransactionTax;
    }

    public double salaryTaxFor(UUID recipient) {
        Double personal = personalTaxes.get(recipient);
        return personal != null ? personal : salaryTax;
    }

    private double clamp01(double v) {
        return Math.max(0, Math.min(1, v));
    }

    private double round4(double v) {
        return Math.round(v * 10000.0) / 10000.0;
    }

    // ------------------------------------------------------------------
    // Transferts / paiements
    // ------------------------------------------------------------------

    public TransferResult transfer(PlayerRef sender, PlayerRef target, double amount, String reason) {
        if (amount <= 0) {
            return TransferResult.fail("Le montant doit être positif.");
        }
        if (sender.uuid().equals(target.uuid())) {
            return TransferResult.fail("Impossible de se payer soi-même.");
        }
        double tax = round(amount * effectiveTax(target.uuid()));
        double received = round(amount - tax);
        Account s = getOrCreate(sender.uuid(), sender.displayName());
        if (!DinarMod.config.allowNegative && s.balance < amount) {
            return TransferResult.fail("Solde insuffisant.");
        }
        s.balance = round(s.balance - amount);
        Account t = getOrCreate(target.uuid(), target.displayName());
        t.balance = round(t.balance + received);
        treasury = round(treasury + tax);
        log(s, "SEND", amount, target.displayName(), reason);
        log(t, "RECEIVE", received, sender.displayName(), reason);
        return TransferResult.ok(tax, received);
    }

    public void paySalary(SalaryEntry entry, String name) {
        double tax = round(entry.amount * salaryTaxFor(entry.uuid));
        double net = round(entry.amount - tax);
        Account a = getOrCreate(entry.uuid, name);
        a.balance = round(a.balance + net);
        treasury = round(treasury + tax);
        entry.lastPaid = System.currentTimeMillis();
        log(a, "SALARY", net, "Salaire", null);
        ServerPlayerEntity p = online(entry.uuid);
        if (p != null) {
            p.sendMessage(Text.literal("§a[Salaire] §fVous avez reçu §e" + money(net) + " §f(salaire)."), false);
        }
    }

    public int payAllSalaries() {
        List<Map.Entry<UUID, SalaryEntry>> all = new ArrayList<>(salaries.entrySet());
        for (Map.Entry<UUID, SalaryEntry> e : all) {
            paySalary(e.getValue(), accountName(e.getKey()));
        }
        return all.size();
    }

    public void checkSalaries() {
        long now = System.currentTimeMillis();
        for (Map.Entry<UUID, SalaryEntry> e : salaries.entrySet()) {
            SalaryEntry s = e.getValue();
            if (now - s.lastPaid >= s.intervalSeconds * 1000L) {
                paySalary(s, accountName(e.getKey()));
            }
        }
    }

    private void log(Account a, String type, double amount, String other, String reason) {
        a.history.addFirst(new TransactionEntry(System.currentTimeMillis(), type, amount, other, reason));
        while (a.history.size() > DinarMod.config.historySize) {
            a.history.removeLast();
        }
    }

    // ------------------------------------------------------------------
    // Salaires
    // ------------------------------------------------------------------

    public SalaryEntry getSalary(UUID uuid) {
        return salaries.get(uuid);
    }

    public void setSalary(UUID uuid, double amount, long intervalSeconds) {
        SalaryEntry existing = salaries.get(uuid);
        if (existing != null) {
            existing.amount = round(amount);
            existing.intervalSeconds = intervalSeconds;
        } else {
            salaries.put(uuid, new SalaryEntry(uuid, round(amount), intervalSeconds));
        }
    }

    public void removeSalary(UUID uuid) {
        salaries.remove(uuid);
    }

    public Map<UUID, SalaryEntry> getSalaries() {
        return salaries;
    }

    public String accountName(UUID uuid) {
        Account a = accounts.get(uuid);
        if (a != null && a.name != null) return a.name;
        ServerPlayerEntity p = online(uuid);
        return p != null ? p.getGameProfile().getName() : "Inconnu";
    }

    // ------------------------------------------------------------------
    // Demandes d'argent (/dmd)
    // ------------------------------------------------------------------

    public TransactionRequest createRequest(PlayerRef sender, PlayerRef target, double amount, String message) {
        return requests.create(sender.uuid(), target.uuid(), amount, message, DinarMod.config.requestExpirySeconds);
    }

    public List<TransactionRequest> requestsFor(UUID uuid) {
        return requests.forPlayer(uuid);
    }

    public TransactionRequest getRequest(int id) {
        return requests.get(id);
    }

    public void removeRequest(int id) {
        requests.remove(id);
    }

    // ------------------------------------------------------------------
    // Résolution de joueurs
    // ------------------------------------------------------------------

    public PlayerRef resolve(ServerCommandSource source, String name) {
        ServerPlayerEntity onlinePlayer = server.getPlayerManager().getPlayer(name);
        if (onlinePlayer != null) {
            return PlayerRef.ofOnline(onlinePlayer);
        }
        for (Account a : accounts.values()) {
            if (a.name != null && a.name.equalsIgnoreCase(name)) {
                return new PlayerRef(a.uuid, a.name, null);
            }
        }
        if (server.getUserCache() != null) {
            Optional<GameProfile> profile = server.getUserCache().findByName(name);
            if (profile.isPresent() && profile.get().getId() != null) {
                GameProfile gp = profile.get();
                return new PlayerRef(gp.getId(), gp.getName(), null);
            }
        }
        return null;
    }

    public PlayerRef resolveUuid(UUID uuid) {
        ServerPlayerEntity p = online(uuid);
        if (p != null) {
            return PlayerRef.ofOnline(p);
        }
        Account a = accounts.get(uuid);
        return new PlayerRef(uuid, a != null ? a.name : "Inconnu", null);
    }

    public ServerPlayerEntity online(UUID uuid) {
        return server == null ? null : server.getPlayerManager().getPlayer(uuid);
    }

    // ------------------------------------------------------------------
    // Classement
    // ------------------------------------------------------------------

    public List<Account> baltop(int page, int perPage) {
        return accounts.values().stream()
                .sorted(Comparator.comparingDouble((Account a) -> a.balance).reversed())
                .skip((long) page * perPage)
                .limit(perPage)
                .toList();
    }

    public int pageCount(int perPage) {
        return Math.max(1, (int) Math.ceil(accounts.size() / (double) perPage));
    }

    public int rank(UUID uuid) {
        List<Account> sorted = accounts.values().stream()
                .sorted(Comparator.comparingDouble((Account a) -> a.balance).reversed())
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            if (sorted.get(i).uuid.equals(uuid)) return i + 1;
        }
        return 0;
    }

    // ------------------------------------------------------------------
    // Formatage
    // ------------------------------------------------------------------

    public String format(double value) {
        DinarConfig cfg = DinarMod.config;
        if (!cfg.suffixFormat) {
            return String.format(Locale.US, "%.2f", value);
        }
        double abs = Math.abs(value);
        String[] suffixes = {"", "K", "M", "B", "T", "Q"};
        int i = 0;
        while (abs >= 1000 && i < suffixes.length - 1) {
            abs /= 1000;
            i++;
        }
        String num = i == 0 ? String.valueOf((long) value) : String.format(Locale.US, "%.2f", abs);
        return (value < 0 ? "-" : "") + num + suffixes[i];
    }

    public String money(double value) {
        return format(value) + " " + DinarMod.config.currencySymbol;
    }

    // ------------------------------------------------------------------
    // Banque
    // ------------------------------------------------------------------

    public double bankBalance(UUID uuid) {
        return bankBalances.getOrDefault(uuid, 0.0);
    }

    public double bankDeposit(UUID uuid, String name, double amount) {
        if (amount <= 0) return 0;
        Account a = getOrCreate(uuid, name);
        if (!DinarMod.config.allowNegative && a.balance < amount) return 0;
        a.balance = round(a.balance - amount);
        double current = bankBalance(uuid);
        bankBalances.put(uuid, round(current + amount));
        log(a, "BANK_DEPOSIT", amount, "Banque", null);
        return bankBalance(uuid);
    }

    public double bankWithdraw(UUID uuid, String name, double amount) {
        if (amount <= 0) return 0;
        double bank = bankBalance(uuid);
        if (bank < amount) return 0;
        bankBalances.put(uuid, round(bank - amount));
        Account a = getOrCreate(uuid, name);
        a.balance = round(a.balance + amount);
        log(a, "BANK_WITHDRAW", amount, "Banque", null);
        return bankBalance(uuid);
    }

    public void setBankBalance(UUID uuid, double amount) {
        bankBalances.put(uuid, round(Math.max(0, amount)));
    }

    public Map<UUID, Double> getBankBalances() {
        return bankBalances;
    }

    // ------------------------------------------------------------------
    // Prêts
    // ------------------------------------------------------------------

    public LoanEntry createLoan(UUID uuid, String name, double amount, double interestRate, long durationSeconds) {
        if (amount <= 0) return null;
        LoanEntry existing = loans.get(uuid);
        if (existing != null && !existing.isRepaid()) return null;
        Account a = getOrCreate(uuid, name);
        a.balance = round(a.balance + amount);
        double totalOwed = round(amount * (1 + interestRate));
        LoanEntry loan = new LoanEntry(uuid, amount, interestRate, totalOwed, durationSeconds);
        loans.put(uuid, loan);
        log(a, "LOAN_TAKE", amount, "Prêt", null);
        return loan;
    }

    public double repayLoan(UUID uuid, String name, double amount) {
        LoanEntry loan = loans.get(uuid);
        if (loan == null || loan.isRepaid()) return 0;
        Account a = getOrCreate(uuid, name);
        double actual = Math.min(amount, a.balance);
        if (actual <= 0) return 0;
        a.balance = round(a.balance - actual);
        loan.amountRepaid = round(loan.amountRepaid + actual);
        if (loan.amountRepaid >= loan.totalOwed) {
            loan.repaid = true;
        }
        log(a, "LOAN_REPAY", actual, "Prêt", null);
        return actual;
    }

    public LoanEntry getLoan(UUID uuid) {
        return loans.get(uuid);
    }

    public Map<UUID, LoanEntry> getLoans() {
        return loans;
    }

    // ------------------------------------------------------------------
    // Divers
    // ------------------------------------------------------------------

    public MinecraftServer getServer() {
        return server;
    }

    public BalanceScoreboard getScoreboard() {
        return scoreboard;
    }

    public RequestManager getRequests() {
        return requests;
    }

    // ------------------------------------------------------------------
    // Persistance
    // ------------------------------------------------------------------

    public void save() {
        if (server == null || dataFile == null) return;
        try {
            JsonObject root = new JsonObject();
            root.addProperty("version", 1);
            root.addProperty("treasury", treasury);
            root.addProperty("globalTransactionTax", globalTransactionTax);
            root.addProperty("salaryTax", salaryTax);

            JsonArray accs = new JsonArray();
            for (Account a : accounts.values()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", a.uuid != null ? a.uuid.toString() : "");
                o.addProperty("name", a.name);
                o.addProperty("balance", a.balance);
                JsonArray hist = new JsonArray();
                for (TransactionEntry t : a.history) {
                    JsonObject h = new JsonObject();
                    h.addProperty("time", t.time());
                    h.addProperty("type", t.type());
                    h.addProperty("amount", t.amount());
                    h.addProperty("other", t.otherName());
                    h.addProperty("reason", t.reason());
                    hist.add(h);
                }
                o.add("history", hist);
                accs.add(o);
            }
            root.add("accounts", accs);

            JsonArray taxes = new JsonArray();
            for (Map.Entry<UUID, Double> e : personalTaxes.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", e.getKey().toString());
                o.addProperty("rate", e.getValue());
                taxes.add(o);
            }
            root.add("personalTaxes", taxes);

            JsonArray sals = new JsonArray();
            for (Map.Entry<UUID, SalaryEntry> e : salaries.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", e.getKey().toString());
                o.addProperty("amount", e.getValue().amount);
                o.addProperty("intervalSeconds", e.getValue().intervalSeconds);
                o.addProperty("lastPaid", e.getValue().lastPaid);
                sals.add(o);
            }
            root.add("salaries", sals);

            JsonArray banks = new JsonArray();
            for (Map.Entry<UUID, Double> e : bankBalances.entrySet()) {
                JsonObject o = new JsonObject();
                o.addProperty("uuid", e.getKey().toString());
                o.addProperty("balance", e.getValue());
                banks.add(o);
            }
            root.add("bankBalances", banks);

            JsonArray loansArr = new JsonArray();
            for (Map.Entry<UUID, LoanEntry> e : loans.entrySet()) {
                LoanEntry l = e.getValue();
                JsonObject o = new JsonObject();
                o.addProperty("uuid", l.uuid.toString());
                o.addProperty("amount", l.amount);
                o.addProperty("interestRate", l.interestRate);
                o.addProperty("totalOwed", l.totalOwed);
                o.addProperty("amountRepaid", l.amountRepaid);
                o.addProperty("createdAt", l.createdAt);
                o.addProperty("expiresAt", l.expiresAt);
                o.addProperty("repaid", l.repaid);
                loansArr.add(o);
            }
            root.add("loans", loansArr);

            Files.createDirectories(dataFile.getParent());
            Path tmp = dataFile.resolveSibling("data.json.tmp");
            Files.writeString(tmp, new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Échec de la sauvegarde.", e);
        }
    }

    public void load() {
        if (dataFile == null || !Files.exists(dataFile)) return;
        try {
            JsonObject root = JsonParser.parseString(Files.readString(dataFile)).getAsJsonObject();
            if (root.has("treasury")) treasury = root.get("treasury").getAsDouble();
            if (root.has("globalTransactionTax")) globalTransactionTax = root.get("globalTransactionTax").getAsDouble();
            if (root.has("salaryTax")) salaryTax = root.get("salaryTax").getAsDouble();

            if (root.has("accounts")) {
                for (var el : root.getAsJsonArray("accounts")) {
                    JsonObject o = el.getAsJsonObject();
                    UUID uuid = o.has("uuid") && !o.get("uuid").getAsString().isEmpty() ? UUID.fromString(o.get("uuid").getAsString()) : null;
                    if (uuid == null) continue;
                    Account a = new Account(uuid, o.has("name") ? o.get("name").getAsString() : null,
                            o.has("balance") ? o.get("balance").getAsDouble() : DinarMod.config.startingBalance);
                    if (o.has("history")) {
                        for (var he : o.getAsJsonArray("history")) {
                            JsonObject h = he.getAsJsonObject();
                            a.history.addLast(new TransactionEntry(
                                    h.has("time") ? h.get("time").getAsLong() : 0,
                                    h.has("type") ? h.get("type").getAsString() : "",
                                    h.has("amount") ? h.get("amount").getAsDouble() : 0,
                                    h.has("other") ? h.get("other").getAsString() : "",
                                    h.has("reason") ? h.get("reason").getAsString() : null));
                        }
                    }
                    accounts.put(uuid, a);
                }
            }

            if (root.has("personalTaxes")) {
                for (var el : root.getAsJsonArray("personalTaxes")) {
                    JsonObject o = el.getAsJsonObject();
                    personalTaxes.put(UUID.fromString(o.get("uuid").getAsString()), o.get("rate").getAsDouble());
                }
            }

            if (root.has("salaries")) {
                for (var el : root.getAsJsonArray("salaries")) {
                    JsonObject o = el.getAsJsonObject();
                    SalaryEntry s = new SalaryEntry();
                    s.uuid = UUID.fromString(o.get("uuid").getAsString());
                    s.amount = o.get("amount").getAsDouble();
                    s.intervalSeconds = o.has("intervalSeconds") ? o.get("intervalSeconds").getAsLong() : 3600;
                    s.lastPaid = o.has("lastPaid") ? o.get("lastPaid").getAsLong() : System.currentTimeMillis();
                    salaries.put(s.uuid, s);
                }
            }

            if (root.has("bankBalances")) {
                for (var el : root.getAsJsonArray("bankBalances")) {
                    JsonObject o = el.getAsJsonObject();
                    bankBalances.put(UUID.fromString(o.get("uuid").getAsString()), o.get("balance").getAsDouble());
                }
            }

            if (root.has("loans")) {
                for (var el : root.getAsJsonArray("loans")) {
                    JsonObject o = el.getAsJsonObject();
                    LoanEntry l = new LoanEntry();
                    l.uuid = UUID.fromString(o.get("uuid").getAsString());
                    l.amount = o.get("amount").getAsDouble();
                    l.interestRate = o.get("interestRate").getAsDouble();
                    l.totalOwed = o.get("totalOwed").getAsDouble();
                    l.amountRepaid = o.has("amountRepaid") ? o.get("amountRepaid").getAsDouble() : 0;
                    l.createdAt = o.has("createdAt") ? o.get("createdAt").getAsLong() : System.currentTimeMillis();
                    l.expiresAt = o.has("expiresAt") ? o.get("expiresAt").getAsLong() : System.currentTimeMillis();
                    l.repaid = o.has("repaid") ? o.get("repaid").getAsBoolean() : false;
                    loans.put(l.uuid, l);
                }
            }
        } catch (Exception e) {
            DinarMod.LOGGER.error("[Dinar] Échec du chargement des données.", e);
        }
    }
}
