package com.recruit.agent.job.infrastructure.persistence;

import com.recruit.agent.common.json.StringSetJsonCodec;

import java.util.Set;

public class JobJsonCodecSpec {

    public static void main(String[] args) {
        shouldEncodeAndDecodeStringSet();
        System.out.println("JobJsonCodecSpec passed");
    }

    private static void shouldEncodeAndDecodeStringSet() {
        StringSetJsonCodec codec = new StringSetJsonCodec();
        Set<String> skills = Set.of("java", "spring boot", "mysql");

        String json = codec.encode(skills);
        Set<String> decoded = codec.decode(json);

        assertEquals(skills, decoded, "技能集合编码再解码后应保持一致");
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ", expected=" + expected + ", actual=" + actual);
        }
    }
}
