package com.recruit.agent.auth.domain;

import java.util.Optional;

public interface UserAccountRepository {

    UserAccount save(UserAccount user);

    Optional<UserAccount> findByUsername(String username);

    long nextUserId();
}
