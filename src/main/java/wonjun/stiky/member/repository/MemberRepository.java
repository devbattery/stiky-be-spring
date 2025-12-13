package wonjun.stiky.member.repository;

import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import wonjun.stiky.member.domain.Member;

import java.util.Optional;

import static wonjun.stiky.generated.Tables.MEMBER;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final DSLContext dsl;

    public Optional<Member> findByEmail(String email) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.EMAIL.eq(email))
                .fetchOptionalInto(Member.class); // jOOQ Record -> POJO 매핑
    }

    public Member save(Member member) {
        if (member.getId() == null) {
            // INSERT
            return dsl.insertInto(MEMBER)
                    .set(MEMBER.EMAIL, member.getEmail())
                    .set(MEMBER.PASSWORD, member.getPassword())
                    .set(MEMBER.NICKNAME, member.getNickname())
                    .set(MEMBER.ROLE, member.getRole())
                    .set(MEMBER.PROVIDER, member.getProvider())
                    .set(MEMBER.PROVIDER_ID, member.getProviderId())
                    .returning() // 삽입된 데이터(ID 포함) 반환
                    .fetchOne()
                    .into(Member.class);
        } else {
            // UPDATE
            dsl.update(MEMBER)
                    .set(MEMBER.PASSWORD, member.getPassword())
                    .set(MEMBER.NICKNAME, member.getNickname())
                    .set(MEMBER.ROLE, member.getRole())
                    .set(MEMBER.PROVIDER, member.getProvider())
                    .set(MEMBER.PROVIDER_ID, member.getProviderId())
                    .where(MEMBER.ID.eq(member.getId()))
                    .execute();
            return member;
        }
    }
}