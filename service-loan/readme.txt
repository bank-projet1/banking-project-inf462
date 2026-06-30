# Service de pret

Port local: 8087

## Flux metier attendu

1. Le client cree une demande de pret.
2. Le client ou l'operateur attache un document justificatif.
3. `service-loan` appelle `service-ai` pour l'analyse OCR et la decision IA.
4. Le dossier peut passer en revue uniquement si une analyse documentaire existe.
5. L'operateur approuve ou rejette le pret.
6. Si le pret est approuve, le service genere l'echeancier et accepte les remboursements.

## Creer une demande de pret

curl -X POST http://localhost:8087/api/loans \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"accountId":10,"amount":500000,"interestRate":8.5,"durationMonths":24}'

## Analyser un document du dossier de pret

curl -X POST http://localhost:8087/api/loans/1/documents/analyze \
  -F "documentType=SALARY" \
  -F "file=@/chemin/vers/bulletin.png"

Types utiles: CNI, PASSPORT, DOMICILE, SALARY, BANK_STATEMENT, CONTRACT, ADMIN, LOAN_APPLICATION.

## Passer le dossier en revue

curl -X PUT http://localhost:8087/api/loans/1/review

Cette etape exige une analyse OCR deja enregistree sur le pret.

## Approuver ou rejeter

curl -X PUT http://localhost:8087/api/loans/1/approve
curl -X PUT http://localhost:8087/api/loans/1/reject

Un dossier dont la decision IA vaut REJECTED ne peut pas etre approuve.

## Echeancier de remboursement

curl http://localhost:8087/api/loans/1/schedule

## Rembourser

curl -X POST http://localhost:8087/api/loans/1/repay \
  -H "Content-Type: application/json" \
  -d '{"amount":25000}'

## Communication inter-services

- `service-loan` verifie `service-account` et `service-customer` lors de la creation de pret.
- `service-loan` appelle `service-ai` pour l'extraction OCR, le score documentaire et la decision IA.
- `service-loan` appelle `transaction-service` pour le versement du pret et les remboursements.
- `service-loan` envoie des notifications a `notification-service`.

## Build

mvn clean package -DskipTests
