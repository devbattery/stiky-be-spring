package wonjun.stiky.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.repository.MemberRepository;

@Transactional
@Service
@RequiredArgsConstructor
public class MemberQueryService {

    private final MemberRepository memberRepository;

    public Member save(Member member) {
        return memberRepository.save(member);
    }

    @Transactional(readOnly = true)
    public Member fetchByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

}
