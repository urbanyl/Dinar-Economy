# Dinar Economy

Mod d'économie complet pour **Minecraft 1.21.1 (Fabric)**.

La monnaie du serveur est le **Dinar (D)**.

## Installation

1. Installez **Fabric Loader** pour Minecraft 1.21.1.
2. Déposez `fabric-api` (version 1.21.1) et `dinar-1.0.0.jar` dans le dossier `mods/`.
3. (Optionnel) **Text Placeholder API** de Patbox + **TAB** (version Fabric) pour utiliser les placeholders.

## Commandes joueurs

| Commande | Description |
| --- | --- |
| `/bal` ou `/balance` ou `/money` | Votre solde |
| `/bal <joueur>` | Solde d'un autre joueur |
| `/pay <joueur> <montant> [raison]` ou `/send` | Envoyer de l'argent (taxe éventuelle déduite) |
| `/dmd <joueur> <montant> [message]` | Demander de l'argent à un joueur |
| `/dmd accept <id>` | Accepter une demande reçue |
| `/dmd deny <id>` | Refuser une demande |
| `/dmd` ou `/dmd list` | Lister vos demandes en attente |
| `/baltop [page]` | Classement des comptes les plus riches |

## Commandes admin (OP / permission niveau 2)

### Économie — `/eco`
- `/eco give <joueur> <montant>`
- `/eco take <joueur> <montant>`
- `/eco set <joueur> <montant>`
- `/eco reset <joueur>`
- `/eco resetall`
- `/eco panel` — **panel graphique** (ajouter / retirer / taxe / salaire)
- `/eco treasury` — trésorerie
- `/eco treasury add|take <montant>`
- `/eco history <joueur>` — historique des transactions
- `/eco reload` — recharger la config
- `/eco save` — forcer la sauvegarde

### Salaires — `/salary`
- `/salary set <joueur> <montant> [intervalle_secondes]` — salaire périodique (défaut : 3600s)
- `/salary remove <joueur>`
- `/salary list`
- `/salary payall` — payer tout de suite tous les salaires
- `/salary info <joueur>`

### Taxes — `/tax`
- `/tax global <pourcent>` — taxe sur toutes les transactions (prélevée sur le destinataire, versée à la trésorerie)
- `/tax salary <pourcent>` — taxe sur les salaires
- `/tax set <joueur> <pourcent>` — taxe personnelle d'un joueur (remplace la taxe globale pour lui)
- `/tax remove <joueur>`
- `/tax list`
- `/tax info <joueur>`

### Divers
- `/dinar scoreboard on|off|status` — affiche les soldes dans le scoreboard latéral
- `/dinar about`

## Placeholders pour TAB

Installez **Text Placeholder API** et **TAB**, puis utilisez :

- `%dinar:balance%` — solde formaté (ex : `12,5K D`)
- `%dinar:balance_raw%` — solde brut (`12345.67`)
- `%dinar:balance_int%` — solde entier
- `%dinar:rank%` — position au classement
- `%dinar:treasury%` — trésorerie du serveur
- `%dinar:currency%` — symbole monétaire (`D`)
- `%dinar:currency_name%` — nom de la monnaie (`Dinar`)

Exemple dans `config/tab/tabConfig.yml` :
```yaml
tablist-name-formatting:
  - "%dinar:balance%"
```

## Scoreboard

Le scoreboard latéral affiche le solde de chaque joueur, mis à jour automatiquement.
Activez-le avec `/dinar scoreboard on` (ou dans la config).

## Config

Fichier `config/dinar.json` :

```json
{
  "currencyName": "Dinar",
  "currencySymbol": "D",
  "startingBalance": 0,
  "globalTransactionTax": 0.0,
  "salaryTax": 0.0,
  "salaryCheckIntervalTicks": 100,
  "requestExpirySeconds": 120,
  "allowNegative": false,
  "suffixFormat": true,
  "autoSaveIntervalTicks": 6000,
  "historySize": 20,
  "scoreboard": {
    "enabled": false,
    "updateIntervalTicks": 40,
    "title": "Dinar"
  }
}
```

## Données

Les comptes, salaires, taxes et la trésorerie sont sauvegardés dans le dossier du monde : `world/dinar/data.json` (sauvegarde automatique toutes les 5 minutes et à l'arrêt du serveur).

## Compilation

```
gradlew build
```

Le mod se trouve dans `build/libs/dinar-1.0.0.jar`.

**Prérequis : JDK 21.**
