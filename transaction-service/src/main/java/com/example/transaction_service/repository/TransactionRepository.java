package com.example.transaction_service.repository;
import com.example.transaction_service.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    
    // Cette méthode magique de Spring Data va générer la requête SQL pour récupérer 
    // toutes les transactions liées à un compte (qu'il soit émetteur ou récepteur)
    List<Transaction> findBySourceAccountIdOrDestinationAccountIdOrderByTimestampDesc(Long sourceId, Long destId);

    List<Transaction> findAllByOrderByTimestampDesc();
}
