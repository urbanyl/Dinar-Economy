package fr.dinar.lang;

import fr.dinar.DinarMod;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Système de traduction du mod Dinar.
 *
 * <p>Les messages français sont utilisés comme clés : en mode français, ils sont renvoyés
 * tels quels. En mode anglais (config {@code lang = "en"}), le catalogue {@link #EN} est
 * consulté pour renvoyer la traduction anglaise correspondante. Tout message absent du
 * catalogue reste en français (mode par défaut).</p>
 */
public final class DinarLang {

    private static final Map<String, String> EN = new HashMap<>();

    static {
        // ---- Global ----
        EN.put("§cCette commande doit être exécutée par un joueur.", "§cThis command must be run by a player.");
        EN.put("§cJoueur introuvable : §e%s", "§cPlayer not found: §e%s");
        EN.put("§cRéservé aux joueurs.", "§cPlayers only.");
        EN.put("§cCommande joueur uniquement.", "§cPlayer-only command.");
        EN.put("§cMontant invalide.", "§cInvalid amount.");
        EN.put("§cLe montant doit être positif.", "§cThe amount must be positive.");

        // ---- DinarMod / chat ----
        EN.put("§6§lBienvenue §r§7sur le serveur RP !", "§6§lWelcome §r§7to the RP server!");
        EN.put("§7Créez votre compte : §a/register <mot de passe> §7— vous serez ensuite invité à définir votre identité RP.", "§7Create your account: §a/register <password> §7— you will then be asked to set your RP identity.");
        EN.put("§6§lConnexion §r§7» §fIdentifiez-vous : §a/login <mot de passe>", "§6§lLogin §r§7» §fPlease log in: §a/login <password>");
        EN.put("§d✉ §fVous avez §e%s §flettre(s) non lue(s) §7(§f/courrier liste§7)", "§d✉ §fYou have §e%s §funread letter(s) §7(§f/courrier liste§7)");
        EN.put("§c🔒 Vous devez être connecté pour parler : §a/login <mot de passe>", "§c🔒 You must be logged in to chat: §a/login <password>");
        EN.put("§6Complétez votre identité pour parler : §f/identite prenom <prénom> §7puis §f/identite metier <métier>", "§6Complete your identity to chat: §f/identite prenom <first name> §7then §f/identite metier <job>");

        // ---- Comptes (register/login/logout) ----
        EN.put("§cUn compte existe déjà. Utilisez §f/login <mot de passe>§c.", "§cAn account already exists. Use §f/login <password>§c.");
        EN.put("§cMot de passe trop court (§f4 caractères minimum§c).", "§cPassword too short (§f4 characters minimum§c).");
        EN.put("§cMot de passe trop long (§f64 caractères maximum§c).", "§cPassword too long (§f64 characters maximum§c).");
        EN.put("§cLe mot de passe ne doit pas contenir d'espaces.", "§cThe password must not contain spaces.");
        EN.put("§cAucun compte. Créez-en un : §f/register <mot de passe>§c.", "§cNo account. Create one: §f/register <password>§c.");
        EN.put("§cMot de passe incorrect.", "§cIncorrect password.");
        EN.put("§cVous n'êtes pas connecté.", "§cYou are not logged in.");
        EN.put("§c🔒 Connectez-vous pour jouer : §a/login <mot de passe> §7ou créez un compte : §a/register <mot de passe>", "§c🔒 Log in to play: §a/login <password> §7or create an account: §a/register <password>");
        EN.put("§a§lCOMPTE CRÉÉ §r§7» §fCompte enregistré pour §e%s§f. Bienvenue !", "§a§lACCOUNT CREATED §r§7» §fAccount registered for §e%s§f. Welcome!");
        EN.put("§a§lCONNECTÉ §r§7» §fBienvenue §e%s§f.", "§a§lLOGGED IN §r§7» §fWelcome §e%s§f.");
        EN.put("§7Vous êtes déconnecté. §f/login <mot de passe> §7pour revenir.", "§7You are logged out. §f/login <password> §7to come back.");
        EN.put("§6Votre identité n'est pas encore définie :", "§6Your identity is not set yet:");
        EN.put("§f/identite prenom <prénom RP> §7puis §f/identite metier <métier>", "§f/identite prenom <RP first name> §7then §f/identite metier <job>");
        EN.put("§6📛 §fVous avez déjà votre carte d'identité.", "§6📛 §fYou already have your ID card.");
        EN.put("§6📛 §fVotre carte d'identité a été délivrée.", "§6📛 §fYour ID card has been issued.");
        EN.put("§7Vous avez déjà votre carte d'identité.", "§7You already have your ID card.");
        EN.put("§6📛 §fCarte d'identité donnée.", "§6📛 §fID card given.");
        EN.put("§cConnectez-vous d'abord (§f/login§c).", "§cLog in first (§f/login§c).");
        EN.put("§cComplétez votre identité d'abord : §f/identite prenom <prénom> §7puis §f/identite metier <métier>§c.", "§cComplete your identity first: §f/identite prenom <first name> §7then §f/identite metier <job>§c.");
        EN.put("§aCarte d'identité posée à vos pieds §7(inventaire plein)§a.", "§aID card dropped at your feet §7(inventory full)§a.");
        EN.put("§c🔒 Connectez-vous pour jouer : §a/login <mot de passe>", "§c🔒 Log in to play: §a/login <password>");
        EN.put("§aCompte créé. Connecté avec succès !", "§aAccount created. Logged in!");
        EN.put("§cCe nom de compte est déjà pris.", "§cThis account name is already taken.");
        EN.put("§cConnecté avec succès !", "§cLogged in!");
        EN.put("§cMot de passe incorrect.", "§cIncorrect password.");
        EN.put("§cAucun compte ne porte ce nom.", "§cNo account with that name.");
        EN.put("§cVous êtes déjà connecté.", "§cYou are already logged in.");
        EN.put("§cVous devez être connecté pour utiliser cette commande.", "§cYou must be logged in to use this command.");
        EN.put("§cUtilisation : §f/register <mot de passe>", "§cUsage: §f/register <password>");
        EN.put("§cMot de passe : 4 à 64 caractères, sans espaces.", "§cPassword: 4 to 64 characters, no spaces.");
        EN.put("§cUtilisation : §f/login <mot de passe>", "§cUsage: §f/login <password>");
        EN.put("§7Déconnecté.", "§7Logged out.");
        EN.put("§7Vous n'êtes pas connecté.", "§7You are not logged in.");

        // ---- Identité ----
        EN.put("§cLe prénom RP ne peut pas être vide.", "§cThe RP first name cannot be empty.");
        EN.put("§cPrénom RP trop long (§f24 caractères max§c).", "§cRP first name too long (§f24 characters max§c).");
        EN.put("§cLe métier ne peut pas être vide.", "§cThe job cannot be empty.");
        EN.put("§cMétier trop long (§f40 caractères max§c).", "§cJob too long (§f40 characters max§c).");
        EN.put("§aPrénom RP défini : §e%s§a.", "§aRP first name set: §e%s§a.");
        EN.put("§aMétier défini : §e%s§a.", "§aJob set: §e%s§a.");
        EN.put("§7Il vous manque : %s", "§7You still need: %s");
        EN.put("§a§lIDENTITÉ COMPLÈTE §r§7» §fVotre nom RP sera : %s", "§a§lIDENTITY COMPLETE §r§7» §fYour RP name will be: %s");
        EN.put("§6§l[RP] §r§7» §fUn nouveau citoyen a rejoint la ville : %s", "§6§l[RP] §r§7» §fA new citizen has joined the city: %s");
        EN.put("§6§lVotre identité RP", "§6§lYour RP identity");
        EN.put("§7Prénom RP : §f%s", "§7RP first name: §f%s");
        EN.put("§7Métier : §f%s", "§7Job: §f%s");
        EN.put("§c(non défini)", "§c(not set)");
        EN.put("§7Affiché dans le chat : %s", "§7Shown in chat: %s");
        EN.put("§7Carte : §f/carte §7| §f/carte donner <joueur>", "§7Card: §f/carte §7| §f/carte donner <player>");
        EN.put("§7Complétez : §f/identite prenom <prénom RP> §7puis §f/identite metier <métier>", "§7Complete: §f/identite prenom <RP first name> §7then §f/identite metier <job>");
        EN.put("§cCette carte d'identité est illisible.", "§cThis ID card is unreadable.");
        EN.put("§cCette carte n'est plus valide.", "§cThis card is no longer valid.");
        EN.put("§6§lCarte d'identité §r§7» %s", "§6§lID Card §r§7» %s");
        EN.put("§7Pseudo : §f%s", "§7Username: §f%s");
        EN.put("§7N° d'identité : §f%s", "§7ID number: §f%s");
        EN.put("§6Carte d'identité §7» §e%s", "§6ID Card §7» §e%s");
        EN.put("§7N° : §f%s", "§7No.: §f%s");
        EN.put("§7Carte officielle de la ville.", "§7Official city ID card.");
        EN.put("§7Présentez-la lors des contrôles RP.", "§7Show it during RP checks.");
        EN.put("§cComplétez d'abord votre identité : §f/identite", "§cComplete your identity first: §f/identite");
        EN.put("§cLe joueur §e%s §cn'est pas en ligne.", "§cThe player §e%s §cis not online.");
        EN.put("§cImpossible de créer votre carte.", "§cUnable to create your card.");
        EN.put("§aCarte posée aux pieds de §e%s §7(inventaire plein)§a.", "§aCard dropped at the feet of §e%s §7(inventory full)§a.");
        EN.put("§aVotre carte d'identité a été donnée à §e%s§a.", "§aYour ID card was given to §e%s§a.");
        EN.put("§6📛 §e%s §fa vous remis sa carte d'identité.", "§6📛 §e%s §fgave you his/her ID card.");
        EN.put("§cVous n'avez pas de carte d'identité.", "§cYou do not have an ID card.");

        // ---- Économie ----
        EN.put("Le montant doit être positif.", "The amount must be positive.");
        EN.put("Impossible de se payer soi-même.", "You cannot pay yourself.");
        EN.put("Solde insuffisant.", "Insufficient balance.");
        EN.put("%s → %s : %s (net %s)%s", "%s → %s: %s (net %s)%s");
        EN.put(" («%s»)", " (\"%s\")");
        EN.put("%s a reçu %s (salaire)", "%s received %s (salary)");
        EN.put("§a[Salaire] §fVous avez reçu §e%s §f(salaire).", "§a[Salary] §fYou received §e%s §f(salary).");
        EN.put("%s a déposé %s à la banque", "%s deposited %s in the bank");
        EN.put("%s a retiré %s de la banque", "%s withdrew %s from the bank");
        EN.put("§a[Banque] §fIntérêts : §e+%s §7(solde bancaire : §e%s§7)", "§a[Bank] §fInterest: §e+%s §7(bank balance: §e%s§7)");
        EN.put("Intérêts bancaires versés : %s", "Bank interest paid out: %s");
        EN.put("%s a contracté un prêt de %s", "%s took out a loan of %s");
        EN.put("%s a remboursé %s de son prêt", "%s repaid %s of their loan");

        // ---- Gouvernement ----
        EN.put("§6§lCaliphat §r§7» §e%s §7a été nommé §6Calife §7du serveur.", "§6§lCaliphate §r§7» §e%s §7was named §6Caliph §7of the server.");
        EN.put("§6§lNouveau Calife", "§6§lNew Caliph");
        EN.put("%s a été nommé Calife du serveur", "%s was named Caliph of the server");
        EN.put("§6§lCaliphat §r§7» §c%s §7n'est plus Calife.", "§6§lCaliphate §r§7» §c%s §7is no longer Caliph.");
        EN.put("%s n'est plus Calife", "%s is no longer Caliph");
        EN.put("§6§lCaliphat §r§7» §e%s §apropose une loi §e» §f%s", "§6§lCaliphate §r§7» §e%s §aproposes a law §e» §f%s");
        EN.put("%s propose la loi «%s» : %s", "%s proposes the law \"%s\": %s");
        EN.put("§6§lCaliphat §r§7» §e%s §apromulgue la loi §e» §f%s", "§6§lCaliphate §r§7» §e%s §apromulgates the law §e» §f%s");
        EN.put("§6§lLoi Promulguée", "§6§lLaw Enacted");
        EN.put("%s promulgue la loi «%s»", "%s promulgates the law \"%s\"");
        EN.put("§6§lCaliphat §r§7» §cLa loi §e» §f%s §ca été rejetée.", "§6§lCaliphate §r§7» §cThe law §e» §f%s §cwas rejected.");
        EN.put("La loi «%s» a été rejetée", "The law \"%s\" was rejected");
        EN.put("§6§lCaliphat §r§7» §eVote ouvert §7pour la loi §f» %s §7(/loi voter)", "§6§lCaliphate §r§7» §eVote opened §7for law §f» %s §7(/loi voter)");
        EN.put("Vote ouvert pour la loi «%s»", "Vote opened for law \"%s\"");
        EN.put("§6§lCaliphat §r§7» §e%s §avote %s §7pour §f» %s", "§6§lCaliphate §r§7» §e%s §avotes %s §7for §f» %s");
        EN.put("§6§lCaliphat §r§7» §aLa loi §e» §f%s §aest §6ADOPTÉE §7(%s OUI, %s NON)", "§6§lCaliphate §r§7» §aThe law §e» §f%s §awas §6ADOPTED §7(%s YES, %s NO)");
        EN.put("§6§lLoi Adoptée", "§6§lLaw Adopted");
        EN.put("La loi «%s» a été adoptée (%s OUI / %s NON)", "The law \"%s\" was adopted (%s YES / %s NO)");
        EN.put("§6§lCaliphat §r§7» §cLa loi §e» §f%s §cest §cREJETÉE §7(%s OUI, %s NON)", "§6§lCaliphate §r§7» §cThe law §e» §f%s §cwas §cREJECTED §7(%s YES, %s NO)");
        EN.put("§c§lLoi Rejetée", "§c§lLaw Rejected");
        EN.put("La loi «%s» a été rejetée (%s OUI / %s NON)", "The law \"%s\" was rejected (%s YES / %s NO)");
        EN.put("§6§lCaliphat §r§7» §e%s §apublie un décret §7» §f%s", "§6§lCaliphate §r§7» §e%s §apublishes a decree §7» §f%s");
        EN.put("§6§lDécret du Calife", "§6§lCaliph's Decree");
        EN.put("%s publie le décret «%s»", "%s publishes the decree \"%s\"");

        // ---- Admin webhook ----
        EN.put("§cURL invalide : elle doit commencer par §fhttps://discord.com/api/webhooks/§c.", "§cInvalid URL: it must start with §fhttps://discord.com/api/webhooks/§c.");
        EN.put("§aWebhook Discord défini : §f%s", "§aDiscord webhook set: §f%s");
        EN.put("§aWebhook Discord activé.", "§aDiscord webhook enabled.");
        EN.put("§aWebhook Discord désactivé.", "§aDiscord webhook disabled.");
        EN.put("§cTitre trop long (§f60 caractères max§c).", "§cTitle too long (§f60 characters max§c).");
        EN.put("§aTitre du webhook défini : §e%s", "§aWebhook title set: §e%s");
        EN.put("§cAucun webhook configuré (§f/dinar webhook set <url>§c).", "§cNo webhook configured (§f/dinar webhook set <url>§c).");
        EN.put("§cLe webhook est désactivé (§f/dinar webhook enable§c).", "§cThe webhook is disabled (§f/dinar webhook enable§c).");
        EN.put("§aMessage de test envoyé sur le webhook Discord.", "§aTest message sent to the Discord webhook.");
        EN.put("§6§lWebhook Discord", "§6§lDiscord Webhook");
        EN.put("§7État : §a✅ actif", "§7Status: §a✅ active");
        EN.put("§7État : §c❌ inactif", "§7Status: §c❌ inactive");
        EN.put("§7Activé : §aoui", "§7Enabled: §ayes");
        EN.put("§7Activé : §cnon", "§7Enabled: §cno");
        EN.put("§7Titre : §e%s", "§7Title: §e%s");
        EN.put("§7URL : §f%s", "§7URL: §f%s");
        EN.put("§7URL : §c(non définie)", "§7URL: §c(not set)");
        EN.put("✅ **Test** : le webhook est fonctionnel.", "✅ **Test**: the webhook is working.");
        EN.put("Catégories", "Categories");
        EN.put("Police, amendes, justice, prison, courrier, économie…", "Police, fines, justice, prison, mail, economy…");
        EN.put("§aConfiguration Dinar rechargée.", "§aDinar configuration reloaded.");
        EN.put("§aScoreboard Dinar %s.", "§aDinar scoreboard %s.");
        EN.put("activé.", "enabled.");
        EN.put("désactivé.", "disabled.");
        EN.put("§6§lScoreboard Dinar §r§7» §e%s", "§6§lDinar Scoreboard §r§7» §e%s");
        EN.put("activé", "enabled");
        EN.put("désactivé", "disabled");
        EN.put("§6§lDinar Economy §r§7v%s §7- Mod d'économie et de caliphat.", "§6§lDinar Economy §r§7v%s §7- Economy and caliphate mod.");
        EN.put("§e/dinar help §7pour les commandes", "§e/dinar help §7for commands");
        EN.put("§7Calife : §e%s", "§7Caliph: §e%s");
        EN.put("§7Lois adoptées : §e%s", "§7Adopted laws: §e%s");
        EN.put("§7Trésorerie : §e%s", "§7Treasury: §e%s");
        EN.put("§7Comptes : §e%s", "§7Accounts: §e%s");
        EN.put("§e/bal §7- Voir votre solde", "§e/bal §7- See your balance");
        EN.put("§e/pay <joueur> <montant> §7- Envoyer de l'argent", "§e/pay <player> <amount> §7- Send money");
        EN.put("§e/dmd <joueur> <montant> §7- Demander de l'argent", "§e/dmd <player> <amount> §7- Request money");
        EN.put("§e/baltop §7- Classement", "§e/baltop §7- Leaderboard");
        EN.put("§e/bank balance §7- Solde bancaire", "§e/bank balance §7- Bank balance");
        EN.put("§e/bank deposit <montant> §7- Déposer en banque", "§e/bank deposit <amount> §7- Deposit to bank");
        EN.put("§e/bank withdraw <montant> §7- Retirer de la banque", "§e/bank withdraw <amount> §7- Withdraw from bank");
        EN.put("§e/loan take <montant> <taux> <durée> §7- Prêt", "§e/loan take <amount> <rate> <duration> §7- Loan");
        EN.put("§e/loan repay <montant> §7- Rembourser un prêt", "§e/loan repay <amount> §7- Repay a loan");
        EN.put("§e/loi liste §7- Voir les lois", "§e/loi liste §7- View laws");
        EN.put("§e/loi livre §7- Livre des lois adoptées", "§e/loi livre §7- Book of adopted laws");
        EN.put("§e/loi voter §7- Voter sur une loi", "§e/loi voter §7- Vote on a law");
        EN.put("§e/caliphat info §7- Info calife", "§e/caliphat info §7- Caliph info");

        // ---- Auction House (/ah) ----
        EN.put("§cLe prix doit être positif.", "§cThe price must be positive.");
        EN.put("§cVous devez tenir un item en main.", "§cYou must hold an item in your hand.");
        EN.put("§aMise en vente §e#%s §7: §f%s x%s §7pour §e%s §7(24h)", "§aListed §e#%s §7: §f%s x%s §7for §e%s §7(24h)");
        EN.put("§cVente introuvable ou expirée.", "§cListing not found or expired.");
        EN.put("§cVous ne pouvez pas acheter votre propre vente.", "§cYou cannot buy your own listing.");
        EN.put("§cSolde insuffisant. Il vous faut §e%s", "§cInsufficient balance. You need §e%s");
        EN.put("§aAchat §e#%s §7: §f%s x%s §7à §e%s §7pour §e%s", "§aBought §e#%s §7: §f%s x%s §7from §e%s §7for §e%s");
        EN.put("§e%s §aachète votre vente §e#%s §7: §f%s x%s §7pour §e%s", "§e%s §abought your listing §e#%s §7: §f%s x%s §7for §e%s");
        EN.put("§6§l═══ Auction House (§e%s/%s§6) ═══", "§6§l═══ Auction House (§e%s/%s§6) ═══");
        EN.put("§7Aucune vente en cours.", "§7No active listings.");
        EN.put("§e#%s §f%s x%s §7par §e%s §7pour §e%s §7(§e%s§7)", "§e#%s §f%s x%s §7by §e%s §7for §e%s §7(§e%s§7)");
        EN.put("§6§l════════════════════════", "§6§l════════════════════════");
        EN.put("§cImpossible d'annuler cette vente.", "§cUnable to cancel this listing.");
        EN.put("§aVente §e#%s §aannulée.", "§aListing §e#%s §acancelled.");
        EN.put("§cVente introuvable : #%s", "§cListing not found: #%s");
        EN.put("§6§lVente #%s", "§6§lListing #%s");
        EN.put("§7Item : §e%s x%s", "§7Item: §e%s x%s");
        EN.put("§7Vendeur : §e%s", "§7Seller: §e%s");
        EN.put("§7Prix : §e%s", "§7Price: §e%s");
        EN.put("§7Expire dans : §e%s", "§7Expires in: §e%s");
        EN.put("§6§l═══ Auction House ═══", "§6§l═══ Auction House ═══");
        EN.put("§e/ah sell <prix> [quantite]", "§e/ah sell <price> [quantity]");
        EN.put("§e/ah buy <id>", "§e/ah buy <id>");
        EN.put("§e/ah list [page]", "§e/ah list [page]");
        EN.put("§e/ah cancel <id>", "§e/ah cancel <id>");
        EN.put("§e/ah info <id>", "§e/ah info <id>");

        // ---- Balance (/bal) ----
        EN.put("§6§lDinar §r§7» §fVotre solde : §e%s §7(§8#%s§7)", "§6§lDinar §r§7» §fYour balance: §e%s §7(§8#%s§7)");
        EN.put("§6§lDinar §r§7» §e%s §fa un solde de §e%s §7(§8#%s§7)", "§6§lDinar §r§7» §e%s §fhas a balance of §e%s §7(§8#%s§7)");

        // ---- Baltop ----
        EN.put("§6§l=== Classement des comptes (§e%s/%s§6) ===", "§6§l=== Account leaderboard (§e%s/%s§6) ===");
        EN.put("§8#%s §e%s §7- §e%s", "§8#%s §e%s §7- §e%s");

        // ---- Banque (/bank) ----
        EN.put("§cLe montant doit être supérieur à zéro.", "§cThe amount must be greater than zero.");
        EN.put("§6§lBanque §r§7» §eSolde bancaire : §f%s §7| §ePortefeuille : §f%s", "§6§lBank §r§7» §eBank balance: §f%s §7| §eWallet: §f%s");
        EN.put("§6§lBanque §r§7» §e%s §fa en banque : §f%s", "§6§lBank §r§7» §e%s §fhas in the bank: §f%s");
        EN.put("§cSolde insuffisant. Vous avez §e%s", "§cInsufficient balance. You have §e%s");
        EN.put("§aDépôt de §e%s §affectué. §eSolde bancaire : §f%s", "§aDeposited §e%s§a. §eBank balance: §f%s");
        EN.put("§cSolde bancaire insuffisant. Vous avez §e%s", "§cInsufficient bank balance. You have §e%s");
        EN.put("§aRetrait de §e%s §affectué. §eSolde bancaire : §f%s", "§aWithdrew §e%s§a. §eBank balance: §f%s");
        EN.put("§6§l══════ Banque ══════", "§6§l══════ Bank ══════");
        EN.put("§e/bank balance §7- Voir votre solde bancaire", "§e/bank balance §7- See your bank balance");
        EN.put("§e/bank deposit <montant> §7- Déposer de l'argent", "§e/bank deposit <amount> §7- Deposit money");
        EN.put("§e/bank withdraw <montant> §7- Retirer de l'argent", "§e/bank withdraw <amount> §7- Withdraw money");
        EN.put("§e/bank balance <joueur> §7- Solde bancaire d'un joueur", "§e/bank balance <player> §7- A player's bank balance");
        EN.put("§6§l════════════════════", "§6§l════════════════════");

        // ---- Caliphat ----
        EN.put("§6§l══════ Caliphat ══════", "§6§l══════ Caliphate ══════");
        EN.put("§e/caliphat info §7- Info sur le calife", "§e/caliphat info §7- Caliph info");
        EN.put("§e/caliphat set <joueur> §7- Nommer un calife", "§e/caliphat set <player> §7- Appoint a caliph");
        EN.put("§e/caliphat remove §7- Retirer le calife", "§e/caliphat remove §7- Remove the caliph");
        EN.put("§e/caliphat loi proposer <titre> <contenu> §7- Proposer une loi", "§e/caliphat loi proposer <title> <content> §7- Propose a law");
        EN.put("§e/caliphat loi promulguer <titre> <contenu> §7- Promulguer", "§e/caliphat loi promulguer <title> <content> §7- Enact a law");
        EN.put("§e/caliphat loi voter <id> §7- Ouvrir un vote", "§e/caliphat loi voter <id> §7- Open a vote");
        EN.put("§e/caliphat loi liste §7- Toutes les lois", "§e/caliphat loi liste §7- All laws");
        EN.put("§e/caliphat loi info <id> §7- Détails d'une loi", "§e/caliphat loi info <id> §7- Law details");
        EN.put("§e/caliphat decret <texte> §7- Publier un décret", "§e/caliphat decret <text> §7- Publish a decree");
        EN.put("§e/caliphat config titre on|off §7- Titres à l'écran", "§e/caliphat config titre on|off §7- On-screen titles");
        EN.put("§e/caliphat config duree_vote <sec> §7- Durée du vote", "§e/caliphat config duree_vote <sec> §7- Vote duration");
        EN.put("§e/caliphat config votes_requis <n> §7- Votes nécessaires", "§e/caliphat config votes_requis <n> §7- Required votes");
        EN.put("§a§lCalife nommé : §e%s", "§a§lCaliph appointed: §e%s");
        EN.put("§aCalife retiré.", "§aCaliph removed.");
        EN.put("§6§lCaliphat §r§7» §7Aucun calife n'est nommé.", "§6§lCaliphate §r§7» §7No caliph has been appointed.");
        EN.put("§6§lCaliphat §r§7» §eCalife : §f%s", "§6§lCaliphate §r§7» §eCaliph: §f%s");
        EN.put("§7Dernier décret : §f%s", "§7Latest decree: §f%s");
        EN.put("§aLoi proposée §7» §f%s §7(#%s)", "§aLaw proposed §7» §f%s §7(#%s)");
        EN.put("§aLoi promulguée §7» §f%s §7(#%s)", "§aLaw enacted §7» §f%s §7(#%s)");
        EN.put("§aVote lancé pour la loi #%s", "§aVote opened for law #%s");
        EN.put("§cImpossible de lancer le vote (loi introuvable ou déjà en cours).", "§cUnable to open the vote (law not found or vote already in progress).");
        EN.put("§7Aucune loi enregistrée.", "§7No law registered.");
        EN.put("§6§l══════ Lois ══════", "§6§l══════ Laws ══════");
        EN.put("§7#%s §f%s %s §7(par §e%s§7)", "§7#%s §f%s %s §7(by §e%s§7)");
        EN.put("§6§l════════════════", "§6§l════════════════");
        EN.put("§7Aucune loi en attente.", "§7No pending law.");
        EN.put("§6§l══════ Lois en attente ══════", "§6§l══════ Pending laws ══════");
        EN.put("§7#%s §f%s §7(par §e%s§7)", "§7#%s §f%s §7(by §e%s§7)");
        EN.put("§6§l══════════════════════════", "§6§l══════════════════════════");
        EN.put("§6§l══════ Loi #%s ══════", "§6§l══════ Law #%s ══════");
        EN.put("§eTitre : §f%s", "§eTitle: §f%s");
        EN.put("§eContenu : §f%s", "§eContent: §f%s");
        EN.put("§eAuteur : §f%s", "§eAuthor: §f%s");
        EN.put("§eStatut : %s", "§eStatus: %s");
        EN.put("§eVotes : §a%s OUI §7/ §c%s NON", "§eVotes: §a%s YES §7/ §c%s NO");
        EN.put("§cLoi introuvable : #%s", "§cLaw not found: #%s");
        EN.put("§aVote enregistré : %s", "§aVote recorded: %s");
        EN.put("§cVote impossible (loi introuvable, déjà voté, ou vote non ouvert).", "§cUnable to vote (law not found, already voted, or vote not open).");
        EN.put("§7Aucun décret en vigueur.", "§7No decree in effect.");
        EN.put("§6§lDécret §r§7» §f%s", "§6§lDecree §r§7» §f%s");
        EN.put("§7Décret : §f%s", "§7Decree: §f%s");
        EN.put("§aDécret publié.", "§aDecree published.");
        EN.put("§aDurée de vote : §e%s secondes", "§aVote duration: §e%s seconds");
        EN.put("§aVotes requis : §e%s", "§aRequired votes: §e%s");
        EN.put("§aTitres à l'écran %s.", "§aOn-screen titles %s.");
        EN.put("activés", "enabled");
        EN.put("désactivés", "disabled");

        // ---- Entreprises (/entreprise) ----
        EN.put("§6§l═══ Entreprises ═══", "§6§l═══ Companies ═══");
        EN.put("§e/entreprise create <nom>", "§e/entreprise create <name>");
        EN.put("§e/entreprise info [nom]", "§e/entreprise info [name]");
        EN.put("§e/entreprise list", "§e/entreprise list");
        EN.put("§e/entreprise invite <joueur>", "§e/entreprise invite <player>");
        EN.put("§e/entreprise kick <joueur>", "§e/entreprise kick <player>");
        EN.put("§e/entreprise depot <montant>", "§e/entreprise depot <amount>");
        EN.put("§e/entreprise withdraw <montant>", "§e/entreprise withdraw <amount>");
        EN.put("§e/entreprise members [nom]", "§e/entreprise members [name]");
        EN.put("§e/entreprise delete <nom>", "§e/entreprise delete <name>");
        EN.put("§6§l═══════════════════", "§6§l═══════════════════");
        EN.put("§cLe nom doit faire entre 2 et 20 caractères.", "§cThe name must be between 2 and 20 characters.");
        EN.put("§cUne entreprise avec ce nom existe déjà.", "§cA company with that name already exists.");
        EN.put("§aEntreprise §e%s §acréée !", "§aCompany §e%s §acreated!");
        EN.put("§7Vous n'avez pas d'entreprise.", "§7You do not own a company.");
        EN.put("§cVous n'avez pas d'entreprise.", "§cYou do not own a company.");
        EN.put("§cEntreprise introuvable : §e%s", "§cCompany not found: §e%s");
        EN.put("§6§l═══ %s ═══", "§6§l═══ %s ═══");
        EN.put("§7Fondateur : §e%s", "§7Founder: §e%s");
        EN.put("§7Membres : §e%s", "§7Members: §e%s");
        EN.put("§7Trésor : §e%s", "§7Treasury: §e%s");
        EN.put("§6§l═══ Entreprises (%s) ═══", "§6§l═══ Companies (%s) ═══");
        EN.put("§e#%s §f%s §7par §e%s §7| §e%s §7membres | §e%s", "§e#%s §f%s §7by §e%s §7| §e%s §7members | §e%s");
        EN.put("§cSeul le fondateur peut inviter.", "§cOnly the founder can invite.");
        EN.put("§cCe joueur est déjà membre.", "§cThat player is already a member.");
        EN.put("§a%s §aajouté à §e%s", "§a%s §aadded to §e%s");
        EN.put("§aVous avez été ajouté à l'entreprise §e%s§a.", "§aYou were added to the company §e%s§a.");
        EN.put("§cSeul le fondateur peut expulser.", "§cOnly the founder can kick.");
        EN.put("§cVous ne pouvez pas vous expulser vous-même.", "§cYou cannot kick yourself.");
        EN.put("§c%s §cexpulsé de §e%s", "§c%s §ckicked from §e%s");
        EN.put("§cSolde insuffisant.", "§cInsufficient balance.");
        EN.put("§aDépôt de §e%s §7dans §e%s §7→ §e%s", "§aDeposited §e%s §7into §e%s §7→ §e%s");
        EN.put("§cVous n'êtes pas membre de cette entreprise.", "§cYou are not a member of this company.");
        EN.put("§cTrésor insuffisant.", "§cInsufficient treasury.");
        EN.put("§aRetrait de §e%s §7de §e%s §7→ §e%s", "§aWithdrew §e%s §7from §e%s §7→ §e%s");
        EN.put("§6§lMembres de %s (%s)", "§6§lMembers of %s (%s)");
        EN.put("§6★ %s", "§6★ %s");
        EN.put("§e%s", "§e%s");
        EN.put("§cVous n'êtes pas le fondateur.", "§cYou are not the founder.");
        EN.put("§aEntreprise §e%s §adissoute.", "§aCompany §e%s §adissolved.");

        // ---- Contrats (/contract) ----
        EN.put("§cTypes valides : vente, service, location, pret", "§cValid types: sale, service, lease, loan");
        EN.put("§7Types : vente, service, location, pret", "§7Types: sale, service, lease, loan");
        EN.put("§cVous ne pouvez pas créer un contrat avec vous-même.", "§cYou cannot create a contract with yourself.");
        EN.put("§aContrat §e#%s §acréé avec §e%s §7(type: §e%s§7, details: §f%s§7, montant: §e%s§7)", "§aContract §e#%s §acreated with §e%s §7(type: §e%s§7, details: §f%s§7, amount: §e%s§7)");
        EN.put("§aContrat §e#%s §acréé avec §e%s §7(type: §e%s§7, details: §f%s§7)", "§aContract §e#%s §acreated with §e%s §7(type: §e%s§7, details: §f%s§7)");
        EN.put("§e%s §6vous propose un contrat §e#%s §7(%s)\n", "§e%s §6proposes a contract §e#%s §7(%s)\n");
        EN.put("§7%s\n", "§7%s\n");
        EN.put("§a§n/contract sign %s §8§o(pour accepter)", "§a§n/contract sign %s §8§o(to accept)");
        EN.put("   §c§n/contract cancel %s §8§o(pour refuser)", "   §c§n/contract cancel %s §8§o(to refuse)");
        EN.put("§cContrat introuvable : #%s", "§cContract not found: #%s");
        EN.put("§cCe contrat n'est plus en attente.", "§cThis contract is no longer pending.");
        EN.put("§cCe contrat ne vous est pas destiné.", "§cThis contract is not addressed to you.");
        EN.put("§aContrat §e#%s §asigné !", "§aContract §e#%s §asigned!");
        EN.put("§e%s §aa signé le contrat §e#%s§a.", "§e%s §asigned the contract §e#%s§a.");
        EN.put("§cCe contrat ne vous concerne pas.", "§cThis contract does not concern you.");
        EN.put("§cContrat §e#%s §cannulé.", "§cContract §e#%s §ccancelled.");
        EN.put("§7Aucun contrat.", "§7No contract.");
        EN.put("§6§l═══ Vos contrats (%s) ═══", "§6§l═══ Your contracts (%s) ═══");
        EN.put("§e#%s §7%s §7avec §e%s §7(%s) §e%s", "§e#%s §7%s §7with §e%s §7(%s) §e%s");
        EN.put("§e#%s §7%s §7avec §e%s §7(%s)", "§e#%s §7%s §7with §e%s §7(%s)");
        EN.put("§7Aucun contrat en attente.", "§7No pending contract.");
        EN.put("§6§l═══ Contrats en attente ═══", "§6§l═══ Pending contracts ═══");
        EN.put("§e#%s §7de §e%s §7(%s) §f%s §e%s", "§e#%s §7from §e%s §7(%s) §f%s §e%s");
        EN.put("§e#%s §7de §e%s §7(%s) §f%s", "§e#%s §7from §e%s §7(%s) §f%s");
        EN.put("§6§l═══ Contrats ═══", "§6§l═══ Contracts ═══");
        EN.put("§e/contract create <joueur> <type> <details> [montant]", "§e/contract create <player> <type> <details> [amount]");
        EN.put("§e/contract sign <id>", "§e/contract sign <id>");
        EN.put("§e/contract cancel <id>", "§e/contract cancel <id>");
        EN.put("§e/contract list", "§e/contract list");
        EN.put("§e/contract pending", "§e/contract pending");
        EN.put("§e/contract info <id>", "§e/contract info <id>");
        EN.put("§6§lContrat #%s %s", "§6§lContract #%s %s");
        EN.put("§7Créateur : §e%s", "§7Creator: §e%s");
        EN.put("§7Cible : §e%s", "§7Target: §e%s");
        EN.put("§7Type : §e%s", "§7Type: §e%s");
        EN.put("§7Détails : §f%s", "§7Details: §f%s");
        EN.put("§7Montant : §e%s", "§7Amount: §e%s");

        // ---- Courrier ----
        EN.put("§cMessage vide ou trop long (500 caractères max).", "§cMessage empty or too long (500 characters max).");
        EN.put("§d✉ §aLa lettre a été envoyée à §e%s §7avec §e%s", "§d✉ §aThe letter was sent to §e%s §7with §e%s");
        EN.put("§d✉ §aLa lettre a été envoyée à §e%s", "§d✉ §aThe letter was sent to §e%s");
        EN.put("§7Vous n'avez aucune lettre.", "§7You have no letter.");
        EN.put("§6§lBoîte aux lettres §r§7(§f%s§7)", "§6§lMailbox §r§7(§f%s§7)");
        EN.put("§f#%s §7%s§e%s%s §7» §f%s", "§f#%s §7%s§e%s%s §7» §f%s");
        EN.put("§7Lire : §f/courrier lire <id>", "§7Read: §f/courrier lire <id>");
        EN.put("§cLettre introuvable.", "§cLetter not found.");
        EN.put("§d✉ §6Lettre de §e%s §7(#%s)", "§d✉ §6Letter from §e%s §7(#%s)");
        EN.put("§7» §f%s", "§7» §f%s");
        EN.put("§a+ Vous avez récupéré §e%s §a(contenu de la lettre).", "§a+ You recovered §e%s §a(letter contents).");
        EN.put("§7Lettre supprimée.", "§7Letter deleted.");
        EN.put("§7Lettre annulée (argent éventuel récupéré).", "§7Letter cancelled (any money was recovered).");
        EN.put("§6§lCourrier §7» §f/courrier envoyer <joueur> <message> "
                + "§7| §f/courrier donner <joueur> <montant> <message> §7| §f/courrier liste §7| §f/courrier lire <id> "
                + "§7| §f/courrier supprimer <id> §7| §f/courrier annuler <id>", "§6§lMail §7» §f/courrier envoyer <player> <message> "
                + "§7| §f/courrier donner <player> <amount> <message> §7| §f/courrier liste §7| §f/courrier lire <id> "
                + "§7| §f/courrier supprimer <id> §7| §f/courrier annuler <id>");

        // ---- Dossier (justice) ----
        EN.put("§6§lDossier judiciaire §r§7» §e%s", "§6§lCriminal record §r§7» §e%s");
        EN.put("§7Casier : §c%s délit(s) §7| §e%s mandat(s) §7| §d%s jugement(s)", "§7Record: §c%s offense(s) §7| §e%s warrant(s) §7| §d%s judgment(s)");
        EN.put("§7Aucun antécédent.", "§7No prior record.");
        EN.put("§bAffaire ouverte #%s §7» §f%s §8(%s)", "§bOpen case #%s §7» §f%s §8(%s)");
        EN.put("§aDélit enregistré pour §e%s §7: §f%s", "§aOffense recorded for §e%s §7: §f%s");
        EN.put("§aMandat d'arrêt émis contre §e%s §7: §f%s", "§aArrest warrant issued against §e%s §7: §f%s");
        EN.put("§aJugement rendu pour §e%s §7: §f%s §7→ §e%s", "§aJudgment passed for §e%s §7: §f%s §7→ §e%s");
        EN.put("§aAffaire ouverte contre §e%s", "§aCase opened against §e%s");
        EN.put("§cAffaire introuvable : §e#%s", "§cCase not found: §e#%s");
        EN.put("§aAffaire #%s clôturée.", "§aCase #%s closed.");
        EN.put("§7Aucune affaire ouverte.", "§7No open case.");
        EN.put("§6§lAffaires ouvertes §r§7(§f%s§7)", "§6§lOpen cases §r§7(§f%s§7)");
        EN.put("§b#%s §7» §e%s §7— §f%s §8(%s)", "§b#%s §7» §e%s §7— §f%s §8(%s)");
        EN.put("§6§lDossier §7» §f/dossier moi §7| §f/dossier voir <joueur> "
                + "§7| §f/dossier delit <joueur> <motif> §7| §f/dossier mandat <joueur> <motif> "
                + "§7| §f/dossier jugement <joueur> <délit> <peine> §7| §f/dossier affaire <joueur> <motif> "
                + "§7| §f/dossier cloturer <id> §7| §f/dossier liste", "§6§lRecord §7» §f/dossier moi §7| §f/dossier voir <player> "
                + "§7| §f/dossier delit <player> <motive> §7| §f/dossier mandat <player> <motive> "
                + "§7| §f/dossier jugement <player> <offense> <sentence> §7| §f/dossier affaire <player> <motive> "
                + "§7| §f/dossier cloturer <id> §7| §f/dossier liste");

        // ---- Éco (/eco) ----
        EN.put("§a%s §freçoit §e%s §f→ nouveau solde : §e%s", "§a%s §freceives §e%s §f→ new balance: §e%s");
        EN.put("§aUn administrateur vous a donné §e%s§a. Nouveau solde : §e%s", "§aAn administrator gave you §e%s§a. New balance: §e%s");
        EN.put("§c%s §fperd §e%s §f→ nouveau solde : §e%s", "§c%s §floses §e%s §f→ new balance: §e%s");
        EN.put("§cUn administrateur vous a retiré §e%s§c. Nouveau solde : §e%s", "§cAn administrator took §e%s§c from you. New balance: §e%s");
        EN.put("§aSolde de §e%s §adéfini à §e%s", "§aBalance of §e%s §aset to §e%s");
        EN.put("§aUn administrateur a défini votre solde à §e%s", "§aAn administrator set your balance to §e%s");
        EN.put("§aSolde de §e%s §arétabli.", "§aBalance of §e%s §arestored.");
        EN.put("§aTous les soldes ont été réinitialisés.", "§aAll balances have been reset.");
        EN.put("§aConfig Dinar rechargée.", "§aDinar config reloaded.");
        EN.put("§aDonnées Dinar sauvegardées.", "§aDinar data saved.");
        EN.put("§6§lTrésorerie §r§7» §e%s", "§6§lTreasury §r§7» §e%s");
        EN.put("§aTrésorerie augmentée de §e%s §f→ §e%s", "§aTreasury increased by §e%s §f→ §e%s");
        EN.put("§cTrésorerie réduite de §e%s §f→ §e%s", "§cTreasury decreased by §e%s §f→ §e%s");
        EN.put("§7Aucune transaction pour §e%s§7.", "§7No transaction for §e%s§7.");
        EN.put("§6§l=== Historique de §e%s §6===", "§6§l=== History of §e%s §6===");

        // ---- Journal ----
        EN.put("§7Aucune entrée dans le journal.", "§7No journal entry.");
        EN.put("§7Catégories : §fECO, SALAIRE, AMENDE, BANQUE, JUSTICE, POLICE, PRISON, MAIL, GOUVERNEMENT", "§7Categories: §fECO, SALAIRE, AMENDE, BANQUE, JUSTICE, POLICE, PRISON, MAIL, GOUVERNEMENT");

        // ---- Prêts (/loan) ----
        EN.put("§cVous avez déjà un prêt en cours. Remboursez-le d'abord.", "§cYou already have an active loan. Repay it first.");
        EN.put("§cImpossible de créer le prêt.", "§cUnable to create the loan.");
        EN.put("§aPrêt contracté : §e%s §7(taux : §e%s%§7, intérêts : §e%s§7, total à rembourser : §e%s§7)", "§aLoan taken out: §e%s §7(rate: §e%s%§7, interest: §e%s§7, total to repay: §e%s§7)");
        EN.put("§7Vous n'avez aucun prêt en cours.", "§7You have no active loan.");
        EN.put("§cSolde insuffisant pour rembourser.", "§cInsufficient balance to repay.");
        EN.put("§aPrêt entièrement remboursé ! §e%s §apayé.", "§aLoan fully repaid! §e%s §apaid.");
        EN.put("§aRemboursement de §e%s §aeffectué. §eReste : §f%s", "§aRepaid §e%s§a. §eRemaining: §f%s");
        EN.put("§7%s §7n'a aucun prêt en cours.", "§7%s §7has no active loan.");
        EN.put("§6§lPrêt de §e%s§6", "§6§lLoan of §e%s§6");
        EN.put("§7  Montant emprunté : §e%s", "§7  Amount borrowed: §e%s");
        EN.put("§7  Taux d'intérêt : §e%s%", "§7  Interest rate: §e%s%");
        EN.put("§7  Total dû : §e%s", "§7  Total owed: §e%s");
        EN.put("§7  Remboursé : §e%s", "§7  Repaid: §e%s");
        EN.put("§7  Reste : §c%s", "§7  Remaining: §c%s");
        EN.put("§7  Temps restant : §e%s", "§7  Time remaining: §e%s");
        EN.put("§7Aucun prêt en cours.", "§7No active loan.");
        EN.put("§6§l=== Prêts en cours ===", "§6§l=== Active loans ===");
        EN.put("§e%s §7» §e%s §7restant sur §e%s", "§e%s §7» §e%s §7remaining of §e%s");
        EN.put("§6§l══════ Prêts ══════", "§6§l══════ Loans ══════");
        EN.put("§e/loan take <montant> <taux> <durée> §7- Contracter un prêt", "§e/loan take <amount> <rate> <duration> §7- Take out a loan");
        EN.put("§e/loan info §7- Votre prêt", "§e/loan info §7- Your loan");
        EN.put("§e/loan list §7- Tous les prêts", "§e/loan list §7- All loans");

        // ---- Lois (/loi) ----
        EN.put("§7Aucun vote en cours pour vous.", "§7No vote in progress for you.");
        EN.put("§6§lVote en cours §r§7» §f%s §7(#%s) §e%s", "§6§lVote in progress §r§7» §f%s §7(#%s) §e%s");

        // ---- Paiements (/pay) ----
        EN.put("§aVous avez envoyé §e%s §aà §e%s§a.%s", "§aYou sent §e%s §ato §e%s§a.%s");
        EN.put("§e%s §avous a envoyé §e%s%s%s", "§e%s §asent you §e%s%s%s");

        // ---- Police (/police) ----
        EN.put("§a%s est maintenant policier.", "§a%s is now a police officer.");
        EN.put("§a%s n'est plus policier.", "§a%s is no longer a police officer.");
        EN.put("§7Aucun policier.", "§7No police officer.");
        EN.put("§9§lPolice §r§7(§f%s§7)", "§9§lPolice §r§7(§f%s§7)");
        EN.put("§f• §e%s", "§f• §e%s");
        EN.put("§6§lPolice §7» §f/police ajouter <joueur> "
                + "§7| §f/police retirer <joueur> §7| §f/police liste", "§6§lPolice §7» §f/police ajouter <player> "
                + "§7| §f/police retirer <player> §7| §f/police liste");

        // ---- Prison ----
        EN.put("§aPosition de la prison définie.", "§aPrison location set.");
        EN.put("§c%s est déjà en prison.", "§c%s is already in prison.");
        EN.put("§a%s incarcéré pour §e%s min§a.", "§a%s imprisoned for §e%s min§a.");
        EN.put("§a%s libéré.", "§a%s released.");
        EN.put("§7La prison est vide.", "§7The prison is empty.");
        EN.put("§8[§cPrison§8] §fDétenus §7(§f%s§7)", "§8[§cPrison§8] §fDetainees §7(§f%s§7)");
        EN.put("§f• §e%s §7— reste §e%sm %ss", "§f• §e%s §7— §e%sm %ss left");
        EN.put("§6§lPrison §7» §f/prison setpos "
                + "§7| §f/prison incarcere <joueur> <minutes> §7| §f/prison libere <joueur> "
                + "§7| §f/prison info §7| §f/mandatdarret <joueur> <motif>", "§6§lPrison §7» §f/prison setpos "
                + "§7| §f/prison incarcere <player> <minutes> §7| §f/prison libere <player> "
                + "§7| §f/prison info §7| §f/mandatdarret <player> <motive>");

        // ---- Demandes d'argent (/dmd) ----
        EN.put("§cVous ne pouvez pas vous demander de l'argent à vous-même.", "§cYou cannot request money from yourself.");
        EN.put("§aDemande envoyée à §e%s §a: §e%s%s", "§aRequest sent to §e%s §a: §e%s%s");
        EN.put("§e%s §6vous demande §e%s%s\n", "§e%s §6requests §e%s%s\n");
        EN.put("§a§n/dmd accept %s §8§o(pour accepter)", "§a§n/dmd accept %s §8§o(to accept)");
        EN.put("   §c§n/dmd deny %s §8§o(pour refuser)", "   §c§n/dmd deny %s §8§o(to refuse)");
        EN.put("§cCette demande n'existe plus (expirée ou déjà traitée).", "§cThis request no longer exists (expired or already handled).");
        EN.put("§cCette demande ne vous est pas destinée.", "§cThis request is not addressed to you.");
        EN.put("§cVous avez refusé la demande de §e%s§c.", "§cYou refused §e%s§c's request.");
        EN.put("§e%s §ca refusé votre demande de §e%s§c.", "§e%s §crefused your request of §e%s§c.");
        EN.put("§e%s §aa accepté votre demande de §e%s§a.", "§e%s §aaccepted your request of §e%s§a.");
        EN.put("§7Aucune demande d'argent en attente.", "§7No pending money request.");
        EN.put("§6§l=== Demandes d'argent en attente ===", "§6§l=== Pending money requests ===");
        EN.put("§7#%s §e%s §f» §e%s%s%s", "§7#%s §e%s §f» §e%s%s%s");

        // ---- Salaires (/salary) ----
        EN.put("§aSalaire de §e%s §adéfini à §e%s §atoutes les §e%s§a.", "§aSalary of §e%s §aset to §e%s §aevery §e%s§a.");
        EN.put("§c%s n'a pas de salaire.", "§c%s has no salary.");
        EN.put("§aSalaire de §e%s §asupprimé.", "§aSalary of §e%s §aremoved.");
        EN.put("§7Aucun salaire configuré.", "§7No salary configured.");
        EN.put("§6§l=== Salaires configurés ===", "§6§l=== Configured salaries ===");
        EN.put("§e%s §7» §e%s §7/ §e%s", "§e%s §7» §e%s §7/ §e%s");
        EN.put("§a%s salaire(s) payé(s).", "§a%s salary(ies) paid.");
        EN.put("§e%s §7n'a pas de salaire.", "§e%s §7has no salary.");
        EN.put("§6§lSalaire de §e%s§6\n"
                + "§7  Montant : §e%s\n"
                + "§7  Intervalle : §e%s\n"
                + "§7  Taxe appliquée : §e%s\n"
                + "§7  Net : §e%s\n"
                + "§7  Prochain paiement dans : §e%s", "§6§lSalary of §e%s§6\n"
                + "§7  Amount: §e%s\n"
                + "§7  Interval: §e%s\n"
                + "§7  Tax applied: §e%s\n"
                + "§7  Net: §e%s\n"
                + "§7  Next payment in: §e%s");

        // ---- Shops ----
        EN.put("§cLes prix doivent être positifs.", "§cPrices must be positive.");
        EN.put("§aShop §e#%s §acréé : §f%s §7(achat: §e%s §7| vente: §e%s§7 | stock max: §e%s§7)", "§aShop §e#%s §acreated: §f%s §7(buy: §e%s §7| sell: §e%s§7 | max stock: §e%s§7)");
        EN.put("§6§l══════ Shops (§e%s/%s§6) ══════", "§6§l══════ Shops (§e%s/%s§6) ══════");
        EN.put("§7Aucun shop.", "§7No shop.");
        EN.put("§e#%s §f%s §7par §e%s §7| Achat: §e%s §7| Vente: §e%s §7| Stock: §e%s/%s", "§e#%s §f%s §7by §e%s §7| Buy: §e%s §7| Sell: §e%s §7| Stock: §e%s/%s");
        EN.put("§cShop introuvable : #%s", "§cShop not found: #%s");
        EN.put("§6§lShop #%s", "§6§lShop #%s");
        EN.put("§7Item : §e%s", "§7Item: §e%s");
        EN.put("§7Propriétaire : §e%s", "§7Owner: §e%s");
        EN.put("§7Prix d'achat : §e%s", "§7Buy price: §e%s");
        EN.put("§7Prix de vente : §e%s", "§7Sell price: §e%s");
        EN.put("§7Stock : §e%s/%s", "§7Stock: §e%s/%s");
        EN.put("§cStock insuffisant (§e%s§c disponible).", "§cInsufficient stock (§e%s§c available).");
        EN.put("§aAchat de §e%s x%s §apour §e%s", "§aBought §e%s x%s §afor §e%s");
        EN.put("§e%s §aachète §e%s x%s §adans votre shop §7(+%s)", "§e%s §abought §e%s x%s §ain your shop §7(+%s)");
        EN.put("§cStock maximal atteint (§e%s§c).", "§cMaximum stock reached (§e%s§c).");
        EN.put("§cLe propriétaire n'a pas assez d'argent pour acheter.", "§cThe owner does not have enough money to buy.");
        EN.put("§aVente de §e%s x%s §apour §e%s", "§aSold §e%s x%s §afor §e%s");
        EN.put("§cCe shop ne vous appartient pas.", "§cThis shop does not belong to you.");
        EN.put("§aShop §e#%s §asupprimé.", "§aShop §e#%s §aremoved.");
        EN.put("§6§l══════ Shops ══════", "§6§l══════ Shops ══════");
        EN.put("§e/shop create <item> <prix_achat> <prix_vente> <stock_max>", "§e/shop create <item> <buy_price> <sell_price> <max_stock>");
        EN.put("§e/shop list [page]", "§e/shop list [page]");
        EN.put("§e/shop info <id>", "§e/shop info <id>");
        EN.put("§e/shop buy <id> <quantite>", "§e/shop buy <id> <quantity>");
        EN.put("§e/shop sell <id> <quantite>", "§e/shop sell <id> <quantity>");
        EN.put("§e/shop remove <id>", "§e/shop remove <id>");

        // ---- Taxes (/tax) ----
        EN.put("§aTaxe globale sur les transactions définie à §e%s%§a.", "§aGlobal transaction tax set to §e%s%§a.");
        EN.put("§aTaxe sur les salaires définie à §e%s%§a.", "§aSalary tax set to §e%s%§a.");
        EN.put("§aTaxe personnelle de §e%s §adéfinie à §e%s%§a.", "§aPersonal tax of §e%s §aset to §e%s%§a.");
        EN.put("§c%s n'a pas de taxe personnelle.", "§c%s has no personal tax.");
        EN.put("§aTaxe personnelle de §e%s §asupprimée.", "§aPersonal tax of §e%s §aremoved.");
        EN.put("§6§l=== Taxes ===\n"
                + "§7  Globale (transactions) : §e%s%\n"
                + "§7  Salaires : §e%s%\n"
                + "§7  Trésorerie : §e%s", "§6§l=== Taxes ===\n"
                + "§7  Global (transactions): §e%s%\n"
                + "§7  Salaries: §e%s%\n"
                + "§7  Treasury: §e%s");
        EN.put("§7  Aucune taxe personnelle.", "§7  No personal tax.");
        EN.put("§7  §e%s §7» §e%s%", "§7  §e%s §7» §e%s%");
        EN.put("§6§lTaxes de §e%s§6\n"
                + "§7  Personnelle : §e%s\n"
                + "§7  Effective (transaction reçue) : §e%s%\n"
                + "§7  Effective (salaire) : §e%s%", "§6§lTaxes of §e%s§6\n"
                + "§7  Personal: §e%s\n"
                + "§7  Effective (received transaction): §e%s%\n"
                + "§7  Effective (salary): §e%s%");

        // ---- Amendes (/amende) ----
        EN.put("§c%s n'a pas assez d'argent (§e%s§c).", "§c%s does not have enough money (§e%s§c).");
        EN.put("§aAmende de §e%s §7infligée à §e%s §7pour : §f%s", "§aFine of §e%s §7issued to §e%s §7for: §f%s");
        EN.put("§c§lAMENDE §r§7» §e%s §cvous a infligé une amende de §e%s §7pour : §f%s", "§c§lFINE §r§7» §e%s §cissued you a fine of §e%s §7for: §f%s");
    }

    private DinarLang() {}

    public static boolean isEnglish() {
        return DinarMod.config != null && "en".equalsIgnoreCase(DinarMod.config.lang);
    }

    public static String t(String fr, Object... args) {
        String fmt = isEnglish() ? EN.get(fr) : null;
        if (fmt == null) {
            fmt = fr;
        }
        return format(fmt, args);
    }

    public static Text text(String fr, Object... args) {
        return Text.literal(t(fr, args));
    }

    private static String format(String fmt, Object... args) {
        if (args == null || args.length == 0) {
            return fmt;
        }
        StringBuilder sb = new StringBuilder(fmt.length() + 32);
        int i = 0;
        for (int p = 0; p < fmt.length(); p++) {
            char c = fmt.charAt(p);
            if (c == '%' && p + 1 < fmt.length() && fmt.charAt(p + 1) == 's' && i < args.length) {
                sb.append(args[i++]);
                p++;
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
