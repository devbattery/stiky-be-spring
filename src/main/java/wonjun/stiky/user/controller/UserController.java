package wonjun.stiky.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wonjun.stiky.user.controller.dto.request.CreateUserRequest;
import wonjun.stiky.user.controller.dto.response.CreateUserResponse;
import wonjun.stiky.user.service.UserService;

@Tag(name = "UserController", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @Operation(summary = "사용자 생성", description = "새로운 사용자를 생성합니다.")
    @PostMapping("/api/users")
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
        Long userId = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateUserResponse.of(userId));
    }

}
