package wonjun.stiky.user.repository.jpa;

import org.springframework.data.jpa.repository.JpaRepository;
import wonjun.stiky.user.domain.User;

public interface UserJpaRepository extends JpaRepository<User, Long> {

}
