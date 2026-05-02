package ru.aviasales.service;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.aviasales.dal.model.Client;
import ru.aviasales.dal.model.UserAccount;
import ru.aviasales.dal.model.UserRole;
import ru.aviasales.dal.repository.ClientRepository;
import ru.aviasales.dal.repository.UserAccountRepository;
import ru.aviasales.security.JwtService;
import ru.aviasales.security.PrivilegeResolver;
import ru.aviasales.security.UserPrincipal;
import ru.aviasales.service.dto.ClientRegistrationRequest;
import ru.aviasales.service.dto.LoginRequest;
import ru.aviasales.service.dto.LoginResponse;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientRepository clientRepository;
    private final UserAccountRepository userAccountRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final PrivilegeResolver privilegeResolver;

    @Transactional
    public LoginResponse registerClient(ClientRegistrationRequest request) {
        if (userAccountRepository.existsByUsername(request.getUsername())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists");
        }

        Client client = new Client();
        client.setName(request.getName());
        client.setApiKey("client-" + UUID.randomUUID());
        client.setBalance(BigDecimal.ZERO);
        client = clientRepository.save(client);

        UserAccount account = new UserAccount();
        account.setUsername(request.getUsername());
        account.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        account.setRole(UserRole.CLIENT);
        account.setClient(client);
        account = userAccountRepository.save(account);

        UserPrincipal principal = UserPrincipal.fromAccount(account,
                privilegeResolver.resolve(account.getRole()));
        String token = jwtService.buildToken(principal);
        return LoginResponse.fromToken(token, jwtService.getExpiration(token), principal);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.buildToken(principal);
        Instant expiresAt = jwtService.getExpiration(token);
        return LoginResponse.fromToken(token, expiresAt, principal);
    }
}
