package com.recruit.agent.job.infrastructure.persistence;

import com.recruit.agent.common.json.StringSetJsonCodec;

import java.util.Set;

@Deprecated
public class JobJsonCodec {

    private final StringSetJsonCodec delegate = new StringSetJsonCodec();

    public String encodeStringSet(Set<String> values) {
        return delegate.encode(values);
    }

    public Set<String> decodeStringSet(String json) {
        return delegate.decode(json);
    }
}
