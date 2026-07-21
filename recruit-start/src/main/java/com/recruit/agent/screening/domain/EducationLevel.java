package com.recruit.agent.screening.domain;

public enum EducationLevel {
    UNKNOWN(0),
    HIGH_SCHOOL(1),
    COLLEGE(2),
    BACHELOR(3),
    MASTER(4),
    DOCTOR(5);

    private final int rank;

    EducationLevel(int rank) {
        this.rank = rank;
    }

    public boolean meets(EducationLevel required) {
        return this.rank >= required.rank;
    }
}
