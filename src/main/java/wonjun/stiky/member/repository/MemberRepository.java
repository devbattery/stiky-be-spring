package wonjun.stiky.member.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.repository.jpa.MemberJpaRepository;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final MemberJpaRepository memberJpaRepository;

    public Member findByEmail(String email) {
        return memberJpaRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("TODO"));
    }

    public Member save(Member member) {
        return memberJpaRepository.save(member);
    }

}
