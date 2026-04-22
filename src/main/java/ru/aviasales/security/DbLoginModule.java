package ru.aviasales.security;

import org.springframework.security.crypto.password.PasswordEncoder;
import ru.aviasales.dal.model.UserAccount;
import ru.aviasales.dal.repository.UserAccountRepository;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.FailedLoginException;
import javax.security.auth.login.LoginException;
import javax.security.auth.spi.LoginModule;
import java.io.IOException;
import java.util.Map;

public class DbLoginModule implements LoginModule {

    private Subject subject;
    private CallbackHandler callbackHandler;
    private UserPrincipal userPrincipal;
    private RolePrincipal rolePrincipal;
    private boolean authenticated;

    @Override
    public void initialize(
            Subject subject,
            CallbackHandler callbackHandler,
            Map<String, ?> sharedState,
            Map<String, ?> options
    ) {
        this.subject = subject;
        this.callbackHandler = callbackHandler;
    }

    @Override
    public boolean login() throws LoginException {
        NameCallback nameCallback = new NameCallback("username");
        PasswordCallback passwordCallback = new PasswordCallback("password", false);

        try {
            callbackHandler.handle(new Callback[]{nameCallback, passwordCallback});
        } catch (IOException | UnsupportedCallbackException e) {
            throw new LoginException("Cannot read credentials");
        }

        String username = nameCallback.getName();
        String password = new String(passwordCallback.getPassword());

        UserAccountRepository accountRepository = ApplicationContextProvider.getBean(UserAccountRepository.class);
        PasswordEncoder passwordEncoder = ApplicationContextProvider.getBean(PasswordEncoder.class);

        UserAccount account = accountRepository.findByUsernameWithOwners(username)
                .orElseThrow(() -> new FailedLoginException("Invalid credentials"));

        if (!isAccountAllowed(account) || !passwordEncoder.matches(password, account.getPasswordHash())) {
            throw new FailedLoginException("Invalid credentials");
        }

        userPrincipal = UserPrincipal.fromAccount(account);
        rolePrincipal = new RolePrincipal("ROLE_" + account.getRole().name());
        authenticated = true;
        return true;
    }

    @Override
    public boolean commit() {
        if (!authenticated) {
            return false;
        }
        subject.getPrincipals().add(userPrincipal);
        subject.getPrincipals().add(rolePrincipal);
        return true;
    }

    @Override
    public boolean abort() {
        clear();
        return true;
    }

    @Override
    public boolean logout() {
        subject.getPrincipals().remove(userPrincipal);
        subject.getPrincipals().remove(rolePrincipal);
        clear();
        return true;
    }

    private boolean isAccountAllowed(UserAccount account) {
        if (!account.isEnabled()) {
            return false;
        }
        if (account.getClient() != null && account.getClient().getDeletedAt() != null) {
            return false;
        }
        return account.getModerator() == null || account.getModerator().getDeletedAt() == null;
    }

    private void clear() {
        authenticated = false;
        userPrincipal = null;
        rolePrincipal = null;
    }
}
