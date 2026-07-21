package com.recruit.agent.common.json;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class StringSetJsonCodecTest {

    @Test
    void encodesAndDecodesStringSet() {
        StringSetJsonCodec codec = new StringSetJsonCodec();
        Set<String> values = Set.of("java", "spring boot", "mysql");

        assertThat(codec.decode(codec.encode(values))).isEqualTo(values);
    }
}
