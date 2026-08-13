# Dinar Economy

Mod d'économie et de caliphat pour **Minecraft 1.21.1 (Fabric)**.

La monnaie du serveur est le **Dinar (D)**.

## Installation

1. Installez **Fabric Loader** pour Minecraft 1.21.1.
2. Déposez `fabric-api` (version 1.21.1) et `dinar-1.1.0.jar` dans le dossier `mods/`.
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

### Banque

| Commande | Description |
| --- | --- |
| `/bank balance` | Voir votre solde bancaire |
| `/bank balance <joueur>` | Solde bancaire d'un autre joueur |
| `/bank deposit <montant>` | Déposer de l'argent en banque |
| `/bank withdraw <montant>` | Retirer de l'argent de la banque |

### Prêts

| Commande | Description |
| --- | --- |
| `/loan take <montant> <taux> <durée>` | Contracter un prêt (taux en décimal, ex: 0.1 = 10%) |
| `/loan repay <montant>` | Rembourser tout ou partie de votre prêt |
| `/loan info` | Voir les détails de votre prêt |
| `/loan info <joueur>` | Voir le prêt d'un autre joueur |
| `/loan list` | Lister tous les prêts en cours |
| `/loan help` | Aide sur les prêts |

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

#### Banque & Prêts

- `/bank deposit|withdraw` — Gestion bancaire
- `/loan take|repay` — Système de prêts

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

## Données

- Économie : `world/dinar/data.json` (inclut banque et prêts)
- Gouvernement : `world/dinar/government.json`
- Sauvegarde automatique toutes les 5 minutes.

## Compilation

```bash
gradlew build
```

**Prérequis : JDK 21.**
