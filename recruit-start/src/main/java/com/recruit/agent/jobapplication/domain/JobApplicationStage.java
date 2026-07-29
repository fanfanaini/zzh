package com.recruit.agent.jobapplication.domain;

public enum JobApplicationStage {
    APPLIED,
    SCREENING,
    HR_REVIEW,
    COMMUNICATION,
    INTERVIEW,
    OFFER,
    HIRED,
    REJECTED,
    WITHDRAWN,
    CLOSED;

    public boolean terminal() {
        return this == HIRED || this == REJECTED || this == WITHDRAWN || this == CLOSED;
    }
}
