# Guide d'integration de auth-service

## 1. Objectif

`auth-service` est le microservice responsable de l'inscription, de la connexion et de la generation des tokens JWT pour la plateforme bancaire.

Il utilise :

- PostgreSQL pour stocker les utilisateurs ;
- Spring Data JPA pour l'acces aux donnees ;
- Spring Security pour le hachage des mots de passe ;
- JWT pour produire un token apres connexion ;
- Spring Cloud Config pour charger sa configuration depuis `cloud-conf` ;
- Eureka Client pour s'enregistrer dans `service-registry` ;
- API Gateway pour exposer les routes sous `/api/auth/**`.

## 2. Services Docker ajoutes

Le fichier `docker-compose.yml` lance deux services techniques :

- `postgres` sur le port local `5433`, redirige vers le port interne `5432` du conteneur ;
- `rabbitmq` sur les ports `5672` et `15672`.

Au premier demarrage, PostgreSQL cree automatiquement les bases suivantes depuis `docker/postgres/init/01-create-databases.sql` :

- `auth_db`
- `customer_db`
- `account_db`
- `transaction_db`
- `loan_db`
- `notification_db`
- `audit_db`

## 3. Demarrer PostgreSQL et RabbitMQ

Depuis la racine du projet :

```bash
docker compose up -d
```

Verifier les conteneurs :

```bash
docker compose ps
```

Interface RabbitMQ :

```text
http://localhost:15672
```

Identifiants par defaut RabbitMQ :

```text
guest / guest
```

## 4. Ordre de demarrage des services Spring

Demarrer dans cet ordre :

```text
1. service-config      -> port 8080
2. service-registry    -> port 8081
3. auth-service        -> port 8084
4. service-account     -> port 8082
5. service-gateway     -> port 8083
```

Commandes possibles :

```bash
cd service-config
./mvnw spring-boot:run
```

```bash
cd service-registry
./mvnw spring-boot:run
```

```bash
cd auth-service
./mvnw spring-boot:run
```

```bash
cd service-gateway
./mvnw spring-boot:run
```

## 5. Configuration distante

Le fichier `cloud-conf/auth-service.properties` contient :

- le port `8084` ;
- l'URL Eureka ;
- la connexion PostgreSQL vers `auth_db` ;
- la configuration JPA ;
- la cle JWT de developpement.

La connexion PostgreSQL locale utilise :

```text
jdbc:postgresql://localhost:5433/auth_db
```

Le port `5433` evite un conflit avec un PostgreSQL deja installe localement sur `5432`.

Important : comme `cloud-conf` est un depot separe, il faut pousser ce fichier dans le depot GitHub `cloud-conf` pour que `service-config` le serve officiellement.

Test attendu apres push :

```text
http://localhost:8080/auth-service/default
```

## 6. Routes Gateway

La route ajoutee dans `cloud-conf/service-gateway.properties` est :

```properties
spring.cloud.gateway.server.webflux.routes[1].id=auth-service-route
spring.cloud.gateway.server.webflux.routes[1].uri=lb://auth-service
spring.cloud.gateway.server.webflux.routes[1].predicates[0]=Path=/api/auth/**
spring.cloud.gateway.server.webflux.routes[1].filters[0]=StripPrefix=2
```

Cela signifie :

```text
http://localhost:8083/api/auth/register
```

est transforme en :

```text
http://auth-service/register
```

## 7. Tests REST

### Inscription

```bash
curl -X POST http://localhost:8083/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Alice Client","email":"alice@example.com","password":"secret123","role":"CLIENT"}'
```

### Connexion

```bash
curl -X POST http://localhost:8083/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"alice@example.com","password":"secret123"}'
```

Resultat attendu :

```json
{
  "token": "...",
  "tokenType": "Bearer",
  "userId": 1,
  "fullName": "Alice Client",
  "email": "alice@example.com",
  "role": "CLIENT"
}
```

### Liste des utilisateurs

```bash
curl http://localhost:8083/api/auth/users
```

### Configuration du service

```bash
curl http://localhost:8083/api/auth/config
```

## 8. Verification Eureka

Ouvrir :

```text
http://localhost:8081
```

Verifier que `AUTH-SERVICE` apparait avec le statut `UP`.

## 9. Arreter l'infrastructure Docker

```bash
docker compose stop
```

Pour tout supprimer, y compris les donnees PostgreSQL :

```bash
docker compose down -v
```

Attention : `down -v` supprime les bases et les donnees.

## 10. Prochaine etape

La prochaine amelioration logique consiste a :

- valider les JWT dans `service-gateway` ;
- proteger les routes sensibles ;
- ajouter les roles `CLIENT`, `OPERATOR`, `ADMIN` ;
- connecter `service-customer` et `service-account` a l'identite de l'utilisateur connecte.
