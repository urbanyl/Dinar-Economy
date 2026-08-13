package fr.dinar.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import fr.dinar.DinarMod;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class DinarConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public String currencyName = "Dinar";
    public String currencySymbol = "D";
    public double startingBalance = 0;
    public double globalTransactionTax = 0.0;
    public double salaryTax = 0.0;
    public int salaryCheckIntervalTicks = 100;
    public int requestExpirySeconds = 120;
    public boolean allowNegative = false;
    public boolean suffixFormat = true;
    public int autoSaveIntervalTicks = 6000;
    public int historySize = 20;
    public ScoreboardConfig scoreboard = new ScoreboardConfig();

    public static class ScoreboardConfig {
        public boolean enabled = false;
        public int updateIntervalTicks = 40;
        public String title = "Dinar";
        public boolean showRank = true;
        public boolean showTreasury = true;
        public boolean showLaws = true;
        public String line1 = "§7Solde : §e%balance%";
        public String line2 = "§7Rang : §e#%rank%";
        public String line3 = "§7Trésorerie : §e%treasury%";
        public String line4 = "§7Lois : §e%laws%";
    }

    public static DinarConfig load() {
        Path path = getPath();
        if (Files.exists(path)) {
            try {
                DinarConfig cfg = GSON.fromJson(Files.readString(path), DinarConfig.class);
                if (cfg != null) {
                    cfg.save();
                    return cfg;
                }
            } catch (Exception e) {
                DinarMod.LOGGER.error("[Dinar] Impossible de lire la config, utilisation des valeurs par défaut.", e);
            }
        }
        DinarConfig cfg = new DinarConfig();
        cfg.save();
        return cfg;
    }

    public void save() {
        try {
            Path path = getPath();
            Files.createDirectories(path.getParent());
            Files.writeString(path, GSON.toJson(this));
        } catch (IOException e) {
            DinarMod.LOGGER.error("[Dinar] Impossible d'écrire la config.", e);
        }
    }

    private static Path getPath() {
        return FabricLoader.getInstance().getConfigDir().resolve("dinar.json");
    }
}
