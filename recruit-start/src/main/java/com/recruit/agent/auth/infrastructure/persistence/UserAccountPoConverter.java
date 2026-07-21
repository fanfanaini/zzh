package com.recruit.agent.auth.infrastructure.persistence;

import com.recruit.agent.auth.domain.UserAccount;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class UserAccountPoConverter {

    private final UserRoleJsonCodec roleJsonCodec = new UserRoleJsonCodec();

    public UserAccountPo toPo(UserAccount user) {
        UserAccountPo po = new UserAccountPo();
        po.setId(user.id());
        po.setUsername(user.username());
        po.setPasswordHash(user.passwordHash());
        po.setRoles(roleJsonCodec.encode(user.roles()));
        po.setEnabled(user.enabled());
        po.setCreatedAt(LocalDateTime.ofInstant(user.createdAt(), ZoneOffset.UTC));
        return po;
    }

    public UserAccount toDomain(UserAccountPo po) {
        return new UserAccount(
                po.getId(),
                po.getUsername(),
                po.getPasswordHash(),
                roleJsonCodec.decode(po.getRoles()),
                Boolean.TRUE.equals(po.getEnabled()),
                po.getCreatedAt().toInstant(ZoneOffset.UTC)
        );
    }
}
