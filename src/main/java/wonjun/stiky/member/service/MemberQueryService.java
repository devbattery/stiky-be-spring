package wonjun.stiky.member.service;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wonjun.stiky.global.exception.CustomException;
import wonjun.stiky.global.exception.ErrorCode;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.repository.MemberRepository;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MemberQueryService {

    private final MemberRepository memberRepository;

    public Member fetchByEmail(String email) {
        return memberRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));
    }

    public Optional<Member> fetchByEmailOpt(String email) {
        return memberRepository.findByEmail(email);
    }

    @Transactional
    public Long save(Member member) {
        return memberRepository.save(member);
    }

}