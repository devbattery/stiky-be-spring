package wonjun.stiky.auth.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import wonjun.stiky.auth.controller.dto.TokenDto;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${openapi.client-url}")
    private String url;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();

        String email = getEmailFromAttributes(attributes);
        TokenDto tokenDto = jwtTokenProvider.generateToken(email, "ROLE_USER");
        redisTemplate.opsForValue().set("RT:" + email, tokenDto.getRefreshToken(), 7, TimeUnit.DAYS);

        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("LOGIN_CODE:" + code, tokenDto.getAccessToken(), 60, TimeUnit.SECONDS);
        redisTemplate.opsForValue().set("LOGIN_CODE:" + code + ":RT", tokenDto.getRefreshToken(), 60, TimeUnit.SECONDS);

        String targetUrl = UriComponentsBuilder.fromUriString(url + "/login/callback")
                .queryParam("code", code)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String getEmailFromAttributes(Map<String, Object> attributes) {
        if (attributes.containsKey("kakao_account")) { // kakao
            Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
            return (String) kakaoAccount.get("email");
        }

        if (attributes.containsKey("response")) { // naver
            Map<String, Object> response = (Map<String, Object>) attributes.get("response");
            return (String) response.get("email");
        }

        return (String) attributes.get("email"); // google
    }

}
