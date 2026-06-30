# Frontend React - Banking Project INF462

Interface React pour tester les microservices deja implementes :

- auth-service
- service-customer
- service-account
- transaction-service

## Demarrage

Depuis la racine du projet :

```bash
cd frontend
npm install
npm run dev
```

URL :

```text
http://localhost:5173
```

## Ordre recommande des services

```text
docker compose up -d
service-config      -> 8080
service-registry    -> 8081
auth-service        -> 8084
service-customer    -> 8085
service-account     -> 8082
transaction-service -> 8086
service-gateway     -> 8083
frontend            -> 5173
```

## Proxy local

Le frontend utilise Vite pour proxifier les appels API :

```text
/auth-api        -> http://localhost:8084
/customer-api    -> http://localhost:8085
/account-api     -> http://localhost:8082
/transaction-api -> http://localhost:8086
/gateway-api     -> http://localhost:8083
```

Ce choix evite les erreurs CORS pendant les tests locaux.
