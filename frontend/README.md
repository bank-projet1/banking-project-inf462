# Frontend Banking Project

Interface web locale pour le projet bancaire complet. L'application affiche
d'abord une page de connexion, puis redirige l'utilisateur vers son espace selon
son role `ADMIN`, `CLIENT` ou `OPERATOR`.

## Lancer

```bash
cd frontend
npm run dev
```

Ouvrir ensuite `http://127.0.0.1:5173`.

## Proxies

Le serveur front expose deux entrees:

- `/api/*` vers la gateway, par defaut `http://localhost:8083`
- `/services/:service/*` vers les microservices directs

Services directs par defaut:

| Cle | URL |
| --- | --- |
| `config` | `http://localhost:8080` |
| `registry` | `http://localhost:8081` |
| `account` | `http://localhost:8082` |
| `gateway` | `http://localhost:8083` |
| `auth` | `http://localhost:8084` |
| `loan` | `http://localhost:8086` |

Variables disponibles:

```bash
API_PROXY_TARGET=http://localhost:8083 npm run dev
AUTH_SERVICE_URL=http://localhost:8084 ACCOUNT_SERVICE_URL=http://localhost:8082 npm run dev
```

## Ecrans disponibles

- Connexion via `auth-service`
- Administrateur: supervision globale, utilisateurs, comptes, credits, services et API
- Client: comptes, demande de pret, suivi des credits et remboursements
- Operateur financier: analyse des prets, validation/rejet et suivi des comptes
- Credits: liste, recherche, workflow, remboursement, detail et echeancier


mvn -q -DskipTests package