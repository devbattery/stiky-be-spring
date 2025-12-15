package wonjun.stiky.member.repository;

import static wonjun.stiky.generated.Tables.MEMBER;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import wonjun.stiky.generated.tables.records.MemberRecord;
import wonjun.stiky.member.domain.Member;

@Repository
@RequiredArgsConstructor
public class MemberRepository {

    private final DSLContext dsl;

    public Optional<Member> findByEmail(String email) {
        return dsl.selectFrom(MEMBER)
                .where(MEMBER.EMAIL.eq(email))
                .fetchOptionalInto(Member.class);
    }

    public Long save(Member member) {
        if (member.getId() == null) {
            return insert(member);
        }

        return update(member);
    }

    private Long insert(Member member) {
        MemberRecord record = dsl.insertInto(MEMBER)
                .set(MEMBER.EMAIL, member.getEmail())
                .set(MEMBER.PASSWORD, member.getPassword())
                .set(MEMBER.NICKNAME, member.getNickname())
                .set(MEMBER.ROLE, member.getRole())
                .set(MEMBER.PROVIDER, member.getProvider())
                .set(MEMBER.PROVIDER_ID, member.getProviderId())
                .returning(MEMBER.ID)
                .fetchOne();

        return record.getId();
    }

    private Long update(Member member) {
        dsl.update(MEMBER)
                .set(MEMBER.PASSWORD, member.getPassword())
                .set(MEMBER.NICKNAME, member.getNickname())
                .set(MEMBER.ROLE, member.getRole())
                .set(MEMBER.PROVIDER, member.getProvider())
                .set(MEMBER.PROVIDER_ID, member.getProviderId())
                .where(MEMBER.ID.eq(member.getId()))
                .execute();

        return member.getId();
    }

}