package wonjun.stiky.auth.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.SerializationUtils;

@Component
@RequiredArgsConstructor
public class RedisOAuth2AuthorizationRequestRepository implements
        AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    private static final String KEY_PREFIX = "OAUTH2_AUTH_REQUEST:";
    private static final long EXPIRE_SECONDS = 180;

    private final RedisTemplate<String, Object> redisTemplate;

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        return findAuthorizationRequest(stateFrom(request));
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest, HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            removeAuthorizationRequest(request, response);
            return;
        }

        String state = authorizationRequest.getState();
        if (state == null) {
            return;
        }

        redisTemplate.opsForValue()
                .set(buildKey(state), serialize(authorizationRequest), EXPIRE_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        String state = stateFrom(request);
        if (state == null) {
            return null;
        }

        String key = buildKey(state);
        Object serialized = redisTemplate.opsForValue().get(key);
        redisTemplate.delete(key);
        return deserialize(serialized);
    }

    private OAuth2AuthorizationRequest findAuthorizationRequest(String state) {
        if (state == null) {
            return null;
        }

        Object serialized = redisTemplate.opsForValue().get(buildKey(state));
        return deserialize(serialized);
    }

    private String stateFrom(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return request.getParameter(OAuth2ParameterNames.STATE);
    }

    private String buildKey(String state) {
        return KEY_PREFIX + state;
    }

    private String serialize(OAuth2AuthorizationRequest authorizationRequest) {
        byte[] bytes = Objects.requireNonNull(SerializationUtils.serialize(authorizationRequest));
        return Base64.getEncoder().encodeToString(bytes);
    }

    private OAuth2AuthorizationRequest deserialize(Object serialized) {
        if (!(serialized instanceof String value)) {
            return null;
        }

        byte[] bytes = Base64.getDecoder().decode(value);
        return (OAuth2AuthorizationRequest) SerializationUtils.deserialize(bytes);
    }

}
