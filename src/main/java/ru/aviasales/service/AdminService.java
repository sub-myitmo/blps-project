package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.aviasales.dal.model.Client;
import ru.aviasales.dal.model.Moderator;
import ru.aviasales.dal.model.UserAccount;
import ru.aviasales.dal.model.UserRole;
import ru.aviasales.dal.repository.ClientRepository;
import ru.aviasales.dal.repository.ModeratorRepository;
import ru.aviasales.dal.repository.UserAccountRepository;
import ru.aviasales.service.dto.AccountResponse;
import ru.aviasales.service.dto.ManagerRequest;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final ClientRepository clientRepository;
    private final ModeratorRepository moderatorRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AccountResponse createManager(ManagerRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        Moderator moderator = new Moderator();
        moderator.setName(request.getName());
        moderator.setApiKey("manager-" + UUID.randomUUID());
        moderator = moderatorRepository.save(moderator);

        UserAccount account = new UserAccount();
        account.setUsername(request.getUsername());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setRole(UserRole.MANAGER);
        account.setModerator(moderator);
        return AccountResponse.fromEntity(userAccountRepository.save(account));
    }

    @Transactional
    public void deleteClient(Long clientId) {
        Client client = clientRepository.findActiveById(clientId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Client not found"));

        client.setDeletedAt(LocalDateTime.now());
        userAccountRepository.findByClientIdWithOwners(clientId)
                .forEach(account -> account.setEnabled(false));
    }

    @Transactional
    public void deleteManager(Long moderatorId) {
        Moderator moderator = moderatorRepository.findById(moderatorId)
                .filter(value -> value.getDeletedAt() == null)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Manager not found"));

        moderator.setDeletedAt(LocalDateTime.now());
        userAccountRepository.findByModeratorIdWithOwners(moderatorId)
                .forEach(account -> account.setEnabled(false));
    }
}
