package ru.aviasales.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.stereotype.Component;

import javax.security.auth.Subject;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.NameCallback;
import javax.security.auth.callback.PasswordCallback;
import javax.security.auth.callback.UnsupportedCallbackException;
import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.Configuration;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Map;

@Component
public class JaasAuthenticationProvider implements AuthenticationProvider {

    private static final String LOGIN_CONTEXT_NAME = "AdvertisingSystem";
    private static final Configuration CONFIGURATION = new JaasConfiguration();

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String username = String.valueOf(authentication.getName());
        String password = String.valueOf(authentication.getCredentials());
        Subject subject = new Subject();

        try {
            LoginContext loginContext = new LoginContext(
                    LOGIN_CONTEXT_NAME,
                    subject,
                    new UsernamePasswordCallbackHandler(username, password),
                    CONFIGURATION
            );
            loginContext.login();
        } catch (LoginException e) {
            throw new BadCredentialsException("Invalid credentials", e);
        }

        UserPrincipal principal = subject.getPrincipals(UserPrincipal.class).stream()
                .findFirst()
                .orElseThrow(() -> new BadCredentialsException("Invalid credentials"));

        return new UsernamePasswordAuthenticationToken(
                principal,
                null,
                principal.getAuthorities()
        );
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }

    private static class JaasConfiguration extends Configuration {

        @Override
        public AppConfigurationEntry[] getAppConfigurationEntry(String name) {
            return new AppConfigurationEntry[]{
                    new AppConfigurationEntry(
                            DbLoginModule.class.getName(),
                            AppConfigurationEntry.LoginModuleControlFlag.REQUIRED,
                            Map.of()
                    )
            };
        }
    }

    private static class UsernamePasswordCallbackHandler implements CallbackHandler {
        private final String username;
        private final String password;

        private UsernamePasswordCallbackHandler(String username, String password) {
            this.username = username;
            this.password = password;
        }

        @Override
        public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
            for (Callback callback : callbacks) {
                if (callback instanceof NameCallback nameCallback) {
                    nameCallback.setName(username);
                } else if (callback instanceof PasswordCallback passwordCallback) {
                    passwordCallback.setPassword(password.toCharArray());
                } else {
                    throw new UnsupportedCallbackException(callback);
                }
            }
        }
    }
}
