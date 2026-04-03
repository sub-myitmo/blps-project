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
    public static final String CLIENT_PATH = "/api/client";
    public static final String PAYMENT_PATH = "/api/payment";
    public static final String MODERATOR_PATH = "/api/moderator";

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

        if (path.startsWith(CLIENT_PATH) || path.startsWith(PAYMENT_PATH)) {
            if (!clientRepository.existsByApiKey(apiKey)) {
                throw new RuntimeException("Invalid client API key");
            }
        } else if (path.startsWith(MODERATOR_PATH)) {
            if (!moderatorRepository.existsByApiKey(apiKey)) {
                throw new RuntimeException("Invalid moderator API key");
            }
        }

        return true;
    }
}
