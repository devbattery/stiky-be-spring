package wonjun.stiky.user.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import wonjun.stiky.user.domain.User;
import wonjun.stiky.user.repository.jpa.UserJpaRepository;

@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final UserJpaRepository userJpaRepository;

    public Long createUser(User user) {
        return userJpaRepository.save(user).getId();
    }

}
