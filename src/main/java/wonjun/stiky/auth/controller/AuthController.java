package wonjun.stiky.auth.controller;

import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final RedisTemplate<String, Object> redisTemplate;

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
