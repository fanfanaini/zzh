package com.recruit.agent.auth.infrastructure.persistence;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.recruit.agent.auth.domain.UserAccount;
import com.recruit.agent.auth.domain.UserAccountRepository;
import com.recruit.agent.common.id.TimestampIdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@Profile("mysql")
public class MyBatisUserAccountRepository implements UserAccountRepository {

    private final UserAccountMapper mapper;
    private final UserAccountPoConverter converter = new UserAccountPoConverter();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public MyBatisUserAccountRepository(UserAccountMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserAccount save(UserAccount user) {
        if (mapper.selectById(user.id()) == null) {
            mapper.insert(converter.toPo(user));
        } else {
            mapper.updateById(converter.toPo(user));
        }
        return user;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(mapper.selectOne(Wrappers.<UserAccountPo>lambdaQuery()
                        .eq(UserAccountPo::getUsername, username)))
                .map(converter::toDomain);
    }

    @Override
    public long nextUserId() {
        return idGenerator.nextId();
    }
}
