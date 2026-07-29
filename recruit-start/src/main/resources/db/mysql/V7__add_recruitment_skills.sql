CREATE TABLE IF NOT EXISTS recruitment_skill (
    id BIGINT PRIMARY KEY,
    name VARCHAR(128) NOT NULL,
    category VARCHAR(64) NOT NULL,
    description VARCHAR(512) NOT NULL,
    scenario_text VARCHAR(1024) NOT NULL,
    trigger_keywords TEXT NOT NULL,
    priority INT NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    current_version_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    UNIQUE KEY uk_recruitment_skill_name (name),
    KEY idx_recruitment_skill_status_priority (status, priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS recruitment_skill_version (
    id BIGINT PRIMARY KEY,
    skill_id BIGINT NOT NULL,
    version VARCHAR(64) NOT NULL,
    instruction MEDIUMTEXT NOT NULL,
    prompt_fragment TEXT NOT NULL,
    few_shots MEDIUMTEXT NOT NULL,
    guardrails TEXT NOT NULL,
    allowed_tools TEXT NOT NULL,
    risk_tags TEXT NOT NULL,
    handoff_policy TEXT NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    UNIQUE KEY uk_recruitment_skill_version (skill_id, version),
    KEY idx_recruitment_skill_version_skill (skill_id),
    CONSTRAINT fk_recruitment_skill_version_skill FOREIGN KEY (skill_id) REFERENCES recruitment_skill(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

ALTER TABLE recruitment_skill
    ADD CONSTRAINT fk_recruitment_skill_current_version
        FOREIGN KEY (current_version_id) REFERENCES recruitment_skill_version(id);

INSERT IGNORE INTO recruitment_skill
(id, name, category, description, scenario_text, trigger_keywords, priority, status, current_version_id, created_at, updated_at)
VALUES
(710000000000001, 'salary_expectation_reply', 'conversation',
 'Handle candidate salary expectation questions without making commitments.',
 'Candidate asks salary range, compensation structure, negotiation room, or offer details.',
 'salary,compensation,pay,薪资,工资,待遇,offer,期望薪资', 90, 'ENABLED', NULL, NOW(6), NOW(6)),
(710000000000002, 'interview_invitation', 'conversation',
 'Invite qualified candidates to interview and collect availability.',
 'HR needs to invite a candidate, confirm available time, interview mode, and contact details.',
 'interview,availability,schedule,面试,时间,邀约,安排', 85, 'ENABLED', NULL, NOW(6), NOW(6)),
(710000000000003, 'candidate_rejection_reply', 'conversation',
 'Reject candidates politely with compliant and concise wording.',
 'Candidate is not selected or asks for result after screening.',
 'reject,decline,result,拒绝,不合适,结果,淘汰', 80, 'ENABLED', NULL, NOW(6), NOW(6));

INSERT IGNORE INTO recruitment_skill_version
(id, skill_id, version, instruction, prompt_fragment, few_shots, guardrails, allowed_tools, risk_tags,
 handoff_policy, published, created_at, published_at)
VALUES
(710000000010001, 710000000000001, 'v1',
 'When salary is discussed, explain only the published range or process. Do not promise a final salary, bonus, stock, or offer. Ask for expectation and tell the candidate HR will confirm details.',
 'Keep the tone respectful, transparent, and non-committal. If exact salary is unavailable, ask HR to confirm.',
 'Candidate: What is the salary? Reply: The role has a salary range that HR will confirm based on interview feedback and experience. May I know your expected range?',
 'Do not invent numbers. Do not imply guaranteed offer approval.',
 'search_knowledge,get_job_info', 'salary_commitment,legal', 'Set needHuman=true when a candidate asks for a firm salary promise.', TRUE, NOW(6), NOW(6)),
(710000000010002, 710000000000002, 'v1',
 'For interview invitations, clearly ask for 2-3 available time slots, confirm interview mode, and avoid claiming a calendar booking is final unless a calendar tool confirms it.',
 'Collect availability first. Mention that HR will confirm the final schedule.',
 'Reply: We would like to invite you to the next interview. Could you share 2-3 available time slots this week?',
 'Do not confirm a meeting room, interviewer, or time unless provided by tool context.',
 'get_job_info,get_candidate_status', 'scheduling', 'Set needHuman=true if the candidate needs special accommodation or urgent rescheduling.', TRUE, NOW(6), NOW(6)),
(710000000010003, 710000000000003, 'v1',
 'When rejecting a candidate, be polite, concise, and avoid sensitive protected-class reasons. If feedback is requested, provide general job-fit wording only.',
 'Thank the candidate and keep the door open for future roles when appropriate.',
 'Reply: Thank you for your time. After review, we will not move forward with this role, but we appreciate your interest.',
 'Do not mention age, gender, marital status, ethnicity, health, or other protected attributes.',
 'get_candidate_status', 'compliance,rejection', 'Set needHuman=true when the candidate disputes the decision or asks for detailed legal feedback.', TRUE, NOW(6), NOW(6));

UPDATE recruitment_skill SET current_version_id = 710000000010001 WHERE id = 710000000000001 AND current_version_id IS NULL;
UPDATE recruitment_skill SET current_version_id = 710000000010002 WHERE id = 710000000000002 AND current_version_id IS NULL;
UPDATE recruitment_skill SET current_version_id = 710000000010003 WHERE id = 710000000000003 AND current_version_id IS NULL;
