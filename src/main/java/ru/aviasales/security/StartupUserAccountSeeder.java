package ru.aviasales.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.dal.model.Client;
import ru.aviasales.dal.model.Moderator;
import ru.aviasales.dal.model.UserAccount;
import ru.aviasales.dal.model.UserRole;
import ru.aviasales.dal.repository.ClientRepository;
import ru.aviasales.dal.repository.ModeratorRepository;
import ru.aviasales.dal.repository.UserAccountRepository;

@Component
@RequiredArgsConstructor
public class StartupUserAccountSeeder implements ApplicationRunner {

    private final UserAccountRepository userAccountRepository;
    private final ClientRepository clientRepository;
    private final ModeratorRepository moderatorRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${security.admin.username}")
    private String adminUsername;

    @Value("${security.admin.password}")
    private String adminPassword;

    @Value("${security.legacy-account-password}")
    private String legacyAccountPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        createAdminIfMissing();
        createLegacyClientAccounts();
        createLegacyManagerAccounts();
    }

    private void createAdminIfMissing() {
        if (userAccountRepository.existsByUsername(adminUsername)) {
            return;
        }

        UserAccount account = new UserAccount();
        account.setUsername(adminUsername);
        account.setPasswordHash(passwordEncoder.encode(adminPassword));
        account.setRole(UserRole.ADMIN);
        userAccountRepository.save(account);
    }

    private void createLegacyClientAccounts() {
        for (Client client : clientRepository.findAll()) {
            if (client.getDeletedAt() != null || client.getApiKey() == null
                    || userAccountRepository.existsByUsername(client.getApiKey())) {
                continue;
            }

            UserAccount account = new UserAccount();
            account.setUsername(client.getApiKey());
            account.setPasswordHash(passwordEncoder.encode(legacyAccountPassword));
            account.setRole(UserRole.CLIENT);
            account.setClient(client);
            userAccountRepository.save(account);
        }
    }

    private void createLegacyManagerAccounts() {
        for (Moderator moderator : moderatorRepository.findAll()) {
            if (moderator.getDeletedAt() != null || moderator.getApiKey() == null
                    || userAccountRepository.existsByUsername(moderator.getApiKey())) {
                continue;
            }

            UserAccount account = new UserAccount();
            account.setUsername(moderator.getApiKey());
            account.setPasswordHash(passwordEncoder.encode(legacyAccountPassword));
            account.setRole(UserRole.MANAGER);
            account.setModerator(moderator);
            userAccountRepository.save(account);
        }
    }
}
