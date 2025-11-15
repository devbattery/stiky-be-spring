package wonjun.stiky.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import wonjun.stiky.user.controller.dto.request.CreateUserRequest;
import wonjun.stiky.user.controller.dto.response.CreateUserResponse;
import wonjun.stiky.user.domain.User;
import wonjun.stiky.user.repository.jpa.UserJpaRepository;
import wonjun.stiky.user.service.UserService;

@RestController
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/api/users")
    public ResponseEntity<CreateUserResponse> createUser(@RequestBody CreateUserRequest request) {
        Long userId = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(CreateUserResponse.of(userId));
    }

}
