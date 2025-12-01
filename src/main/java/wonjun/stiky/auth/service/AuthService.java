package wonjun.stiky.auth.service;

import static wonjun.stiky.global.exception.ErrorCode.EMAIL_ALREADY_EXISTS;
import static wonjun.stiky.global.exception.ErrorCode.INVALID_TOKEN;
import static wonjun.stiky.global.exception.ErrorCode.LOGIN_FAILED;

import jakarta.servlet.http.HttpServletResponse;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wonjun.stiky.auth.config.JwtTokenProvider;
import wonjun.stiky.auth.controller.dto.TokenDto;
import wonjun.stiky.auth.controller.dto.request.LoginRequest;
import wonjun.stiky.auth.controller.dto.request.SignupRequest;
import wonjun.stiky.auth.controller.dto.response.SignupResponse;
import wonjun.stiky.global.exception.CustomException;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.service.MemberQueryService;

@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final MemberQueryService memberQueryService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    @Value("${cookie.domain}")
    private String cookieUrl;

    public SignupResponse signup(SignupRequest request) {
        if (memberQueryService.fetchByEmailOpt(request.getEmail()).isPresent()) {
            throw new CustomException(EMAIL_ALREADY_EXISTS);
        }

        Member member = Member.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .nickname(request.getNickname())
                .role("ROLE_USER")
                .provider("local")
                .build();

        Member savedMember = memberQueryService.save(member);
        return SignupResponse.of(savedMember.getId());
    }

    public TokenDto login(LoginRequest request) {
        Member member = memberQueryService.fetchByEmail(request.getEmail());

        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new CustomException(LOGIN_FAILED);
        }

        TokenDto tokenDto = jwtTokenProvider.generateToken(member.getEmail(), member.getRole());

        redisTemplate.opsForValue()
                .set("RT:" + member.getEmail(), tokenDto.getRefreshToken(), 7, TimeUnit.DAYS);

        return tokenDto;
    }

    public TokenDto reissue(String refreshToken) {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new CustomException(INVALID_TOKEN);
        }

        String email = jwtTokenProvider.getEmail(refreshToken);
        String storedRefreshToken = (String) redisTemplate.opsForValue().get("RT:" + email);

        if (!refreshToken.equals(storedRefreshToken)) {
            throw new CustomException(INVALID_TOKEN);
        }

        Member member = memberQueryService.fetchByEmail(email);
        TokenDto newToken = jwtTokenProvider.generateToken(member.getEmail(), member.getRole());

        redisTemplate.opsForValue()
                .set("RT:" + email, newToken.getRefreshToken(), 7, TimeUnit.DAYS);

        return newToken;
    }

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie.ResponseCookieBuilder cookieBuilder = ResponseCookie.from("refresh_token", refreshToken)
                .httpOnly(true)
                .secure(true)
                .path("/")
                .maxAge(7 * 24 * 60 * 60)
                .sameSite("None");
        cookieBuilder.domain(cookieUrl);
        response.addHeader(HttpHeaders.SET_COOKIE, cookieBuilder.build().toString());
    }

}
