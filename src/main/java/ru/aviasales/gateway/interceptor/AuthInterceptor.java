package ru.aviasales.gateway.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import ru.aviasales.dal.repository.ClientRepository;
import ru.aviasales.dal.repository.ModeratorRepository;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final ClientRepository clientRepository;
    private final ModeratorRepository moderatorRepository;

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) {
        String path = request.getRequestURI();
        String apiKey = request.getHeader("Authorization");

        if (apiKey == null || apiKey.isEmpty()) {
            throw new RuntimeException("API key is required");
        }

        if (path.startsWith("/api/client") || path.startsWith("/api/payment")) {
            if (!clientRepository.existsByApiKey(apiKey)) {
                throw new RuntimeException("Invalid client API key");
            }
        } else if (path.startsWith("/api/moderator")) {
            if (!moderatorRepository.existsByApiKey(apiKey)) {
                throw new RuntimeException("Invalid moderator API key");
            }
        }

        return true;
    }
}
