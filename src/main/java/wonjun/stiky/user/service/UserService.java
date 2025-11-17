package wonjun.stiky.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import wonjun.stiky.user.controller.dto.request.CreateUserRequest;
import wonjun.stiky.user.domain.User;
import wonjun.stiky.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public Long createUser(CreateUserRequest createUserRequest) {
        User user = new User(createUserRequest.getName(), createUserRequest.getEmail(), createUserRequest.getAge());
        return userRepository.createUser(user);
    }

}
