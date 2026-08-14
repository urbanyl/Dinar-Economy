![Dinar Economy](image-Photoroom.png)

Mod d'économie et de caliphat pour **Minecraft 1.21.1 (Fabric)**.

La monnaie du serveur est le **Dinar (D)**.

## Installation
1. Installez **Fabric Loader** pour Minecraft 1.21.1.
2. Déposez `fabric-api` (version 1.21.1) et `dinar-1.2.0.jar` dans le dossier `mods/`.
3. (Optionnel) **Text Placeholder API** de Patbox + **TAB** pour les placeholders.

## Commandes joueurs

### Économie

| Commande | Description |
| --- | --- |
| `/bal` ou `/balance` ou `/money` | Votre solde |
| `/bal <joueur>` | Solde d'un autre joueur |
| `/pay <joueur> <montant> [raison]` | Envoyer de l'argent |
| `/dmd <joueur> <montant> [message]` | Demander de l'argent |
| `/dmd accept <id>` | Accepter une demande |
| `/dmd deny <id>` | Refuser une demande |
| `/dmd` ou `/dmd list` | Lister vos demandes |
| `/baltop [page]` | Classement des plus riches |

### Banque & Prêts

| Commande | Description |
| --- | --- |
| `/bank balance` | Votre solde bancaire (avec intérêts) |
| `/bank balance <joueur>` | Solde bancaire d'un joueur |
| `/bank deposit <montant>` | Déposer en banque |
| `/bank withdraw <montant>` | Retirer de la banque |
| `/loan take <montant> <taux> <durée>` | Contracter un prêt |
| `/loan repay <montant>` | Rembourser un prêt |
| `/loan info` | Détails de votre prêt |
| `/loan list` | Tous les prêts en cours |
| `/loan help` | Aide sur les prêts |

### Shops de joueurs

| Commande | Description |
| --- | --- |
| `/shop create <item> <prix_achat> <prix_vente> <stock_max>` | Créer un shop |
| `/shop list [page]` | Voir tous les shops |
| `/shop info <id>` | Détails d'un shop |
| `/shop buy <id> <quantite>` | Acheter depuis un shop |
| `/shop sell <id> <quantite>` | Vendre à un shop |
| `/shop remove <id>` | Supprimer votre shop |
| `/shop help` | Aide shops |

### Auction House

| Commande | Description |
| --- | --- |
| `/ah sell <prix> [quantite]` | Mettre en vente (tenir l'item en main) |
| `/ah buy <id>` | Acheter une vente |
| `/ah list [page]` | Voir les ventes |
| `/ah cancel <id>` | Annuler votre vente |
| `/ah info <id>` | Détails d'une vente |
| `/ah help` | Aide auction house |

### Entreprises

| Commande | Description |
| --- | --- |
| `/entreprise create <nom>` | Créer une entreprise |
| `/entreprise info [nom]` | Infos sur une entreprise |
| `/entreprise list` | Toutes les entreprises |
| `/entreprise invite <joueur>` | Inviter un membre |
| `/entreprise kick <joueur>` | Expulser un membre |
| `/entreprise depot <montant>` | Déposer au trésor |
| `/entreprise withdraw <montant>` | Retirer du trésor |
| `/entreprise members [nom]` | Lister les membres |
| `/entreprise delete <nom>` | Dissoudre l'entreprise |
| `/entreprise help` | Aide entreprises |

### Contrats

| Commande | Description |
| --- | --- |
| `/contract create <joueur> <type> <details> [montant]` | Créer un contrat |
| `/contract sign <id>` | Signer un contrat |
| `/contract cancel <id>` | Annuler un contrat |
| `/contract list` | Vos contrats |
| `/contract pending` | Contrats en attente |
| `/contract info <id>` | Détails d'un contrat |
| `/contract help` | Aide contrats |

Types de contrats : `vente`, `service`, `location`, `pret`

### Caliphat (gouvernement)

| Commande | Description |
| --- | --- |
| `/caliphat info` | Info sur le calife en cours |
| `/loi liste` | Voir toutes les lois |
| `/loi livre` | Ouvrir le livre des lois adoptées (GUI) |
| `/loi voter` | Voter sur une loi en cours (GUI) |
| `/loi voter <id> <0\|1>` | Voter (0=NON, 1=OUI) |
| `/loi info <id>` | Détails d'une loi |
| `/loi decret` | Voir le décret en cours |
| `/loi calife` | Info du calife |
| `/amende <joueur> <montant> <raison>` | Infliger une amende (calife/OP) |
| `/dinar help` | Panel d'aide avec toutes les commandes |
| `/dinar about` | Informations sur le mod |

### Commandes admin (OP / permission 2)

#### Économie — `/eco`

- `/eco give|take|set|reset <joueur> <montant>`
- `/eco resetall` — Réinitialiser tous les comptes
- `/eco panel` — Panel graphique admin
- `/eco treasury [add|take <montant>]`
- `/eco history <joueur>` — Historique
- `/eco reload|save`

#### Caliphat — `/caliphat`

- `/caliphat set <joueur>` — Nommer un calife
- `/caliphat remove` — Retirer le calife
- `/caliphat loi proposer <titre> <contenu>` — Proposer une loi (vote)
- `/caliphat loi promulguer <titre> <contenu>` — Promulguer directement
- `/caliphat loi voter <id>` — Ouvrir un vote
- `/caliphat loi liste` — Toutes les lois
- `/caliphat loi info <id>` — Détails d'une loi
- `/caliphat decret <texte>` — Publier un décret
- `/caliphat config titre on|off` — Titres à l'écran (adopté/rejeté)
- `/caliphat config duree_vote <sec>` — Durée du vote (défaut: 300s)
- `/caliphat config votes_requis <n>` — Votes nécessaires (défaut: 3)

#### Salaires — `/salary`

- `/salary set <joueur> <montant> [intervalle_secondes]`
- `/salary remove|list|payall|info <joueur>`

#### Taxes — `/tax`

- `/tax global|salary <pourcent>`
- `/tax set <joueur> <pourcent>` — Taxe personnelle
- `/tax remove|list|info <joueur>`

## Scoreboard

Le scoreboard latéral affiche le solde de chaque joueur.
Customisable dans `config/dinar.json` :

```json
"scoreboard": {
  "enabled": false,
  "title": "Dinar",
  "showRank": true,
  "showTreasury": true,
  "showLaws": true
}
```

## Placeholders

- `%dinar:balance%` — Solde formaté
- `%dinar:balance_raw%` — Solde brut
- `%dinar:balance_int%` — Solde entier
- `%dinar:rank%` — Rang au classement
- `%dinar:treasury%` — Trésorerie
- `%dinar:leader%` — Nom du calife
- `%dinar:laws%` — Nombre de lois adoptées
- `%dinar:decree%` — Décret en cours
- `%dinar:is_leader%` — true/false si le joueur est calife
- `%dinar:currency%` — Symbole monétaire
- `%dinar:currency_name%` — Nom de la monnaie

## Configuration

Options dans `config/dinar.json` :

| Option | Défaut | Description |
| --- | --- | --- |
| `currencyName` | Dinar | Nom de la monnaie |
| `currencySymbol` | D | Symbole monétaire |
| `startingBalance` | 0 | Solde de départ |
| `globalTransactionTax` | 0 | Taxe sur les transactions |
| `salaryTax` | 0 | Taxe sur les salaires |
| `bankInterestRate` | 0.02 | Taux d'intérêt bancaire (2%/cycle) |
| `bankInterestIntervalTicks` | 72000 | Intervalle des intérêts (1h) |
| `allowNegative` | false | Autoriser les soldes négatifs |
| `suffixFormat` | true | Format K/M/B pour les montants |

## Données

- Économie : `world/dinar/data.json` (comptes, banque, prêts)
- Shops : `world/dinar/shops.json`
- Entreprises : `world/dinar/companies.json`
- Auction House : `world/dinar/auctions.json`
- Contrats : `world/dinar/contracts.json`
- Gouvernement : `world/dinar/government.json`
- Sauvegarde automatique toutes les 5 minutes.

## Compilation

```bash
gradlew build
```

**Prérequis : JDK 21.**
