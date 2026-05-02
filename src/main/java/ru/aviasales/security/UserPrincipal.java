package ru.aviasales.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import ru.aviasales.dal.model.UserAccount;
import ru.aviasales.dal.model.UserRole;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

@Getter
public class UserPrincipal implements Principal {

    private final Long id;
    private final String username;
    private final UserRole role;
    private final Long clientId;
    private final Long moderatorId;
    private final Set<String> privileges;

    public UserPrincipal(Long id, String username, UserRole role, Long clientId, Long moderatorId,
                         Set<String> privileges) {
        this.id = id;
        this.username = username;
        this.role = role;
        this.clientId = clientId;
        this.moderatorId = moderatorId;
        this.privileges = privileges == null ? Set.of() : Set.copyOf(privileges);
    }

    public static UserPrincipal fromAccount(UserAccount account, Set<String> privileges) {
        Long clientId = account.getClient() != null ? account.getClient().getId() : null;
        Long moderatorId = account.getModerator() != null ? account.getModerator().getId() : null;
        return new UserPrincipal(account.getId(), account.getUsername(), account.getRole(),
                clientId, moderatorId, privileges);
    }

    @Override
    public String getName() {
        return username;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        List<GrantedAuthority> authorities = new ArrayList<>(privileges.size() + 1);
        authorities.add(new SimpleGrantedAuthority("ROLE_" + role.name()));
        for (String privilege : privileges) {
            authorities.add(new SimpleGrantedAuthority(privilege));
        }
        return authorities;
    }
}
