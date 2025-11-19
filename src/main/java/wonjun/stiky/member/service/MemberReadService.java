package wonjun.stiky.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberReadService {

    private final MemberRepository memberRepository;

    public Member fetchByEmail(String email) {
        return memberRepository.findByEmail(email);
    }

}
