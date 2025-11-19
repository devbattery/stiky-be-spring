package wonjun.stiky.member.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import wonjun.stiky.member.domain.Member;
import wonjun.stiky.member.repository.MemberRepository;

@Transactional
@Service
@RequiredArgsConstructor
public class MemberSaveService {

    private final MemberRepository memberRepository;

    public Member save(Member member) {
        return memberRepository.save(member);
    }

}
