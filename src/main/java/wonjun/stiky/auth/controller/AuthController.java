package wonjun.stiky.auth.controller;

import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wonjun.stiky.auth.controller.dto.request.LoginRequest;
import wonjun.stiky.auth.controller.dto.request.SignupRequest;
import wonjun.stiky.auth.controller.dto.response.SignupResponse;
import wonjun.stiky.auth.service.AuthService;
import wonjun.stiky.auth.controller.dto.TokenDto;

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
    public ResponseEntity<TokenDto> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/api/auth/token")
    public ResponseEntity<?> getToken(@RequestBody Map<String, String> request) {
        String code = request.get("code");
        String redisKey = "LOGIN_CODE:" + code;

        String accessToken = (String) redisTemplate.opsForValue().get(redisKey);

        if (accessToken == null) {
            return ResponseEntity.badRequest().body("잘못되었거나 만료된 임시 코드입니다.");
        }

        redisTemplate.delete(redisKey);

        return ResponseEntity.ok(Map.of("accessToken", accessToken));
    }

}
