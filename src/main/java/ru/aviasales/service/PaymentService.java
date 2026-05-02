package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
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
    public BigDecimal deposit(Long clientId, PaymentRequest request) {
        Client client = clientRepository.findActiveByIdForUpdate(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Client not found"));

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
    public BigDecimal getBalance(Long clientId) {
        Client client = clientRepository.findActiveById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Client not found"));
        return client.getBalance();
    }
}
