package com.recruit.agent.auth.infrastructure;

import com.recruit.agent.auth.domain.UserAccount;
import com.recruit.agent.auth.domain.UserAccountRepository;
import com.recruit.agent.auth.domain.UserRole;
import com.recruit.agent.common.id.TimestampIdGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Repository
@Profile("local")
public class InMemoryUserAccountRepository implements UserAccountRepository {

    private final Map<String, UserAccount> users = new ConcurrentHashMap<>();
    private final TimestampIdGenerator idGenerator = new TimestampIdGenerator();

    public InMemoryUserAccountRepository(PasswordEncoder passwordEncoder) {
        UserAccount admin = new UserAccount(
                1L,
                "admin",
                passwordEncoder.encode("admin123"),
                Set.of(UserRole.ADMIN, UserRole.HR),
                true,
                Instant.now()
        );
        users.put(admin.username(), admin);
    }

    @Override
    public UserAccount save(UserAccount user) {
        users.put(user.username(), user);
        return user;
    }

    @Override
    public Optional<UserAccount> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }

    @Override
    public long nextUserId() {
        return idGenerator.nextId();
    }
}
