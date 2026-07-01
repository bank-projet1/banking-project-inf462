# Documentation API Swagger et tests - Banking Project INF462

## 1. Liens Swagger UI a partager

Ces liens seront disponibles lorsque la dependance Springdoc OpenAPI sera ajoutee dans chaque service et que les services seront demarres.

| Service | Port | Swagger UI | OpenAPI JSON |
|---|---:|---|---|
| auth-service | 8084 | http://localhost:8084/swagger-ui/index.html | http://localhost:8084/v3/api-docs |
| service-account | 8082 | http://localhost:8082/swagger-ui/index.html | http://localhost:8082/v3/api-docs |
| service-customer | 8085 | http://localhost:8085/swagger-ui/index.html | http://localhost:8085/v3/api-docs |
| transaction-service | 8086 | http://localhost:8086/swagger-ui/index.html | http://localhost:8086/v3/api-docs |
| document-intelligence-service | 8087 | http://localhost:8087/swagger-ui/index.html | http://localhost:8087/v3/api-docs |

Si les appels passent par API Gateway, les tests fonctionnels principaux peuvent aussi etre exposes via :

| Service | Exemple via gateway |
|---|---|
| auth-service | http://localhost:8083/api/auth/... |
| service-account | http://localhost:8083/api/account/... |
| service-customer | http://localhost:8083/api/customer/... |
| transaction-service | http://localhost:8083/api/transactions/... |
| document-intelligence-service | http://localhost:8083/api/documents/... |

## 2. Dependances Swagger a ajouter

Pour activer Swagger UI dans chaque microservice Spring Boot Web MVC, ajouter dans chaque `pom.xml` :

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.8.9</version>
</dependency>
```

Puis redemarrer le service et ouvrir le lien Swagger UI correspondant.

## 3. Ordre de demarrage recommande

1. Demarrer PostgreSQL et RabbitMQ :

```bash
docker compose up -d
```

2. Demarrer les services dans cet ordre :

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
cd service-customer
./mvnw spring-boot:run
```

```bash
cd service-account
./mvnw spring-boot:run
```

```bash
cd transaction-service
./mvnw spring-boot:run
```

```bash
cd document-intelligence-service
./mvnw spring-boot:run
```

3. Verifier Eureka :

```text
http://localhost:8081
```

Les services attendus sont :

```text
AUTH-SERVICE
SERVICE-CUSTOMER
SERVICE-ACCOUNT
TRANSACTION-SERVICE
DOCUMENT-INTELLIGENCE-SERVICE
```

## 4. auth-service - Port 8084

Role : inscription, connexion, generation de JWT et consultation des utilisateurs.

### GET - verifier le service

```http
GET http://localhost:8084/
```

Reponse attendue :

```json
{
  "service": "auth-service",
  "port": "8084",
  "status": "UP"
}
```

### GET - verifier la configuration

```http
GET http://localhost:8084/config
```

### POST - inscription

```http
POST http://localhost:8084/register
Content-Type: application/json
```

```json
{
  "fullName": "Alice Client",
  "email": "alice@example.com",
  "password": "secret123",
  "role": "CLIENT"
}
```

### POST - connexion

```http
POST http://localhost:8084/login
Content-Type: application/json
```

```json
{
  "email": "alice@example.com",
  "password": "secret123"
}
```

Reponse attendue : un token JWT.

### GET - liste des utilisateurs

```http
GET http://localhost:8084/users
```

### GET - consulter un utilisateur

```http
GET http://localhost:8084/users/1
```

### PUT - modifier un utilisateur

```http
PUT http://localhost:8084/users/1
Content-Type: application/json
```

```json
{
  "fullName": "Alice Client Updated",
  "email": "alice.updated@example.com",
  "password": "newSecret123",
  "role": "CLIENT"
}
```

Remarque : si le champ `password` est vide ou absent, le mot de passe existant est conserve.

### DELETE - supprimer un utilisateur

```http
DELETE http://localhost:8084/users/1
```

## 5. service-customer - Port 8085

Role : gestion des clients bancaires.

### POST - creer un client

```http
POST http://localhost:8085/customers
Content-Type: application/json
```

```json
{
  "firstName": "Alice",
  "lastName": "Talla",
  "email": "alice.talla@example.com",
  "phone": "+237690000001",
  "address": "Yaounde"
}
```

### GET - lister les clients

```http
GET http://localhost:8085/customers
```

### GET - consulter un client

```http
GET http://localhost:8085/customers/1
```

### PUT - modifier un client

```http
PUT http://localhost:8085/customers/1
Content-Type: application/json
```

```json
{
  "firstName": "Alice",
  "lastName": "Talla",
  "email": "alice.talla@example.com",
  "phone": "+237690000002",
  "address": "Douala"
}
```

### DELETE - supprimer un client

```http
DELETE http://localhost:8085/customers/1
```

## 6. service-account - Port 8082

Role : creation, consultation, suppression des comptes et mise a jour du solde.

### POST - creer un compte

```http
POST http://localhost:8082/accounts
Content-Type: application/json
```

```json
{
  "accountNumber": "ACC-001",
  "balance": 10000,
  "customerId": 1,
  "status": "ACTIVE",
  "currency": "XAF",
  "accountType": "CURRENT"
}
```

### GET - lister les comptes

```http
GET http://localhost:8082/accounts
```

### GET - consulter un compte

```http
GET http://localhost:8082/accounts/1
```

### GET - consulter le solde

```http
GET http://localhost:8082/accounts/1/balance
```

### PUT - mettre a jour le solde

Cette route est critique, car `transaction-service` l'appelle avec Feign.

```http
PUT http://localhost:8082/accounts/update-balance?accountId=1&amount=5000
```

Pour un retrait, envoyer un montant negatif :

```http
PUT http://localhost:8082/accounts/update-balance?accountId=1&amount=-2000
```

### DELETE - supprimer un compte

```http
DELETE http://localhost:8082/accounts/1
```

### CRUD manquant a prevoir

Pour un CRUD compte complet, il reste a ajouter :

```http
PUT /accounts/{id}
```

## 7. transaction-service - Port 8086

Role : gerer les depots, retraits, transferts et historique des transactions.

Important : avant de tester ce service, `SERVICE-ACCOUNT` doit etre visible dans Eureka.

### POST - depot

```http
POST http://localhost:8086/api/transactions/deposit?accountId=1&amount=5000
```

Effet attendu :

- cree une transaction de type `DEPOSIT`;
- appelle `service-account` sur `PUT /accounts/update-balance`;
- augmente le solde du compte.

### POST - retrait

```http
POST http://localhost:8086/api/transactions/withdrawal?accountId=1&amount=2000
```

Effet attendu :

- cree une transaction de type `WITHDRAWAL`;
- appelle `service-account`;
- diminue le solde du compte.

### POST - transfert

```http
POST http://localhost:8086/api/transactions/transfer?sourceAccountId=1&destinationAccountId=2&amount=3000
```

Effet attendu :

- debite le compte source;
- credite le compte destination;
- cree une transaction de type `TRANSFER`.

### GET - historique d'un compte

```http
GET http://localhost:8086/api/transactions/history/1
```

### GET - lister toutes les transactions

```http
GET http://localhost:8086/api/transactions
```

### GET - consulter une transaction

```http
GET http://localhost:8086/api/transactions/1
```

### PUT - modifier une transaction

Cette route permet de corriger l'enregistrement d'une transaction. Elle ne doit pas etre utilisee pour executer un vrai depot, retrait ou transfert, car ces operations doivent passer par les routes metier afin de mettre a jour les soldes.

```http
PUT http://localhost:8086/api/transactions/1
Content-Type: application/json
```

```json
{
  "sourceAccountId": null,
  "destinationAccountId": 1,
  "amount": 7000,
  "type": "DEPOSIT",
  "timestamp": "2026-06-27T15:30:00"
}
```

### DELETE - supprimer une transaction

Cette route supprime l'enregistrement transactionnel. Elle ne restaure pas automatiquement le solde du compte.

```http
DELETE http://localhost:8086/api/transactions/1
```

## 8. document-intelligence-service - Port 8087

Role : analyse OCR/IA des documents clients, detection du type de document, extraction de champs et verification basique.

### POST - analyser un document

Cette route recoit un fichier en `multipart/form-data`. Elle sert par exemple a analyser une CNI camerounaise, un passeport, un justificatif de domicile, une fiche de paie, un releve bancaire ou un contrat de travail.

```http
POST http://localhost:8087/api/documents/analyze
Content-Type: multipart/form-data
```

Champs `form-data` Postman :

```text
file: fichier image ou PDF a analyser
documentType: CAMEROON_CNI
```

Le champ `documentType` est optionnel. Si on ne l'envoie pas, le service essaie de classifier automatiquement le document.

Exemple `curl` :

```bash
curl -X POST http://localhost:8087/api/documents/analyze \
  -F "file=@cameroon_cni_test.png" \
  -F "documentType=CAMEROON_CNI"
```

Exemple via API Gateway :

```bash
curl -X POST http://localhost:8083/api/documents/analyze \
  -F "file=@cameroon_cni_test.png" \
  -F "documentType=CAMEROON_CNI"
```

Reponse attendue :

```json
{
  "documentType": "CAMEROON_CNI",
  "documentStatus": "VALID",
  "country": "Cameroon",
  "countryCode": "CM",
  "idNumber": "123456789",
  "surname": "NOM",
  "givenNames": "PRENOMS",
  "faceDetected": true,
  "faceCount": 1,
  "extractedFields": {},
  "verificationResults": {},
  "processingNotes": "OCR and document intelligence analysis completed."
}
```

Les valeurs exactes dependent de la qualite du fichier envoye et de ce que l'OCR reussit a lire.

### Erreurs possibles

```json
{
  "error": "ocr_processing_failure",
  "message": "Unable to process uploaded document"
}
```

```json
{
  "error": "file_too_large",
  "message": "Uploaded file exceeds the maximum allowed size."
}
```

## 9. Scenario de test complet recommande

1. Creer un utilisateur dans `auth-service`.
2. Se connecter et recuperer le JWT.
3. Creer un client dans `service-customer`.
4. Creer deux comptes dans `service-account`.
5. Verifier que `SERVICE-ACCOUNT` et `TRANSACTION-SERVICE` sont visibles dans Eureka.
6. Faire un depot avec `transaction-service`.
7. Verifier le solde dans `service-account`.
8. Faire un retrait.
9. Verifier encore le solde.
10. Faire un transfert entre deux comptes.
11. Consulter l'historique des transactions.
12. Analyser une piece d'identite avec `document-intelligence-service`.

## 10. Erreurs frequentes

### Erreur 503 dans transaction-service

```text
Load balancer does not contain an instance for the service service-account
```

Cause : `service-account` n'est pas demarre ou n'est pas enregistre dans Eureka.

Correction :

```text
1. Demarrer service-account.
2. Verifier http://localhost:8081.
3. Attendre quelques secondes.
4. Relancer le test transaction-service.
```

### Erreur DataSource

```text
Failed to determine a suitable driver class
```

Cause : la configuration PostgreSQL n'est pas chargee depuis `cloud-conf`.

Correction :

```text
1. Verifier le fichier de configuration dans cloud-conf.
2. Pousser la configuration sur GitHub.
3. Redemarrer service-config.
4. Redemarrer le microservice concerne.
```
