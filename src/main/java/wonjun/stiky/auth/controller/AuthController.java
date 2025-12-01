package wonjun.stiky.auth.controller;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wonjun.stiky.auth.controller.dto.TokenDto;
import wonjun.stiky.auth.controller.dto.request.LoginRequest;
import wonjun.stiky.auth.controller.dto.request.SignupRequest;
import wonjun.stiky.auth.controller.dto.response.SignupResponse;
import wonjun.stiky.auth.service.AuthService;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AuthService authService;

    @PostMapping("/api/auth/signup")
    public ResponseEntity<SignupResponse> signup(@RequestBody @Valid SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/api/auth/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest request, HttpServletResponse response) {
        TokenDto tokenDto = authService.login(request);
        authService.setRefreshTokenCookie(response, tokenDto.getRefreshToken());
        return ResponseEntity.ok(Map.of("accessToken", tokenDto.getAccessToken()));
    }

    @PostMapping("/api/auth/reissue")
    public ResponseEntity<?> reissue(@CookieValue(value = "refresh_token", required = false) String refreshToken,
                                     HttpServletResponse response) {
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Refresh Token 존재하지 않음");
        }

        TokenDto tokenDto = authService.reissue(refreshToken);
        return ResponseEntity.ok(Map.of("accessToken", tokenDto.getAccessToken()));
    }

    @PostMapping("/api/auth/token")
    public ResponseEntity<?> getToken(@RequestBody Map<String, String> request, HttpServletResponse response) {
        String code = request.get("code");
        String redisKey = "LOGIN_CODE:" + code;
        String accessToken = (String) redisTemplate.opsForValue().get(redisKey);
        String refreshToken = (String) redisTemplate.opsForValue().get(redisKey + ":RT");

        if (accessToken == null || refreshToken == null) {
            return ResponseEntity.badRequest().body("잘못되었거나 만료된 임시 코드입니다.");
        }

        redisTemplate.delete(redisKey);
        redisTemplate.delete(redisKey + ":RT");

        authService.setRefreshTokenCookie(response, refreshToken);

        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }

    @PostMapping("/api/auth/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from("refresh_token", "")
                .maxAge(0)
                .path("/")
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.ok("로그아웃 되었습니다.");
    }

}
