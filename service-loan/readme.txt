# Service de prêt

# Créer une demande de prêt
curl -X POST http://localhost:8084/api/loans \
  -H "Content-Type: application/json" \
  -d '{"customerId":1,"accountId":10,"amount":5000,"interestRate":5,"durationMonths":12}'

# Récupérer le prêt
curl http://localhost:8084/api/loans/1

# Passer en revue
curl -X PUT http://localhost:8084/api/loans/1/review

# Approuver
curl -X PUT http://localhost:8084/api/loans/1/approve

# Rejeter
curl -X PUT http://localhost:8084/api/loans/1/reject

# Voir l'échéancier
curl http://localhost:8084/api/loans/1/schedule

# Rembourser
curl -X POST http://localhost:8084/api/loans/1/repay \
  -H "Content-Type: application/json" \
  -d '{"amount":416.67}'

# Loan inexistant → doit retourner 404 maintenant
curl http://localhost:8084/api/loans/999

# Communication inter-services
- `service-loan` vérifie `service-account` et `service-customer` lors de la création de prêt
- `service-loan` envoie une notification à `notification-service` à chaque événement important (review, approval, rejection, repayment)

# Builder + déploiement
mvn clean package -DskipTests
docker-compose up --build
