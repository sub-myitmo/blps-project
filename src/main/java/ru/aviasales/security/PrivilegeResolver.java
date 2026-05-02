package ru.aviasales.security;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.aviasales.dal.model.UserRole;
import ru.aviasales.dal.repository.PrivilegeRepository;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PrivilegeResolver {

    private final PrivilegeRepository privilegeRepository;
    private final ConcurrentHashMap<UserRole, Set<String>> cache = new ConcurrentHashMap<>();

    @Transactional(readOnly = true)
    public Set<String> resolve(UserRole role) {
        if (role == null) {
            return Set.of();
        }
        return cache.computeIfAbsent(role, r -> Set.copyOf(privilegeRepository.findCodesByRole(r)));
    }

    public void invalidate(UserRole role) {
        if (role == null) {
            cache.clear();
        } else {
            cache.remove(role);
        }
    }

    public void invalidateAll() {
        cache.clear();
    }
}
