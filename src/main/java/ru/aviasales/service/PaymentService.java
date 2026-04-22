package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.service.dto.PaymentRequest;
import ru.aviasales.dal.model.Client;
import ru.aviasales.dal.model.Transaction;
import ru.aviasales.dal.repository.ClientRepository;
import ru.aviasales.dal.repository.TransactionRepository;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final ClientRepository clientRepository;
    private final TransactionRepository transactionRepository;

    @Transactional
    public BigDecimal deposit(String apiKey, PaymentRequest request) {
        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Client not found"));

        client.setBalance(client.getBalance().add(request.getAmount()));
        clientRepository.save(client);

        Transaction transaction = new Transaction();
        transaction.setClientId(client.getId());
        transaction.setAmount(request.getAmount());
        transaction.setType(Transaction.TransactionType.DEPOSIT);
        transaction.setDescription(request.getDescription());
        transactionRepository.save(transaction);

        return client.getBalance();
    }

    @Transactional
    public BigDecimal getBalance(String apiKey) {
        Client client = clientRepository.findByApiKey(apiKey)
                .orElseThrow(() -> new RuntimeException("Client not found"));
        return client.getBalance();
    }
}
