package com.recruit.agent.knowledge.infrastructure;

import com.recruit.agent.knowledge.application.KnowledgeQueryRewriter;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class RecruitmentKnowledgeQueryRewriter implements KnowledgeQueryRewriter {

    @Override
    public List<String> rewrite(String query, int maxQueries) {
        if (query == null || query.isBlank() || maxQueries <= 0) {
            return List.of();
        }
        Set<String> queries = new LinkedHashSet<>();
        String normalized = query.trim();
        queries.add(normalized);
        for (String expansion : expansions(normalized)) {
            queries.add(expansion);
            if (queries.size() >= maxQueries) {
                break;
            }
        }
        return List.copyOf(queries);
    }

    private List<String> expansions(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        List<String> values = new ArrayList<>();
        if (containsAny(lower, "薪", "工资", "待遇", "offer", "pay", "salary")) {
            values.add(query + " 薪资范围 薪酬承诺 offer 条款 人工确认");
            values.add("招聘沟通中薪资待遇如何回复 不能承诺的合规边界");
        }
        if (containsAny(lower, "面试", "预约", "时间", "安排", "interview", "schedule")) {
            values.add(query + " 面试流程 预约时间 候选人确认 待跟进");
            values.add("面试安排规则 改期 取消 下一次跟进时间");
        }
        if (containsAny(lower, "进度", "结果", "状态", "反馈", "status", "result")) {
            values.add(query + " 候选人状态 面试反馈 录用进度 跟进记录");
            values.add("候选人询问招聘进度时如何查询和回复");
        }
        if (containsAny(lower, "隐私", "手机号", "邮箱", "身份证", "简历", "privacy", "pii")) {
            values.add(query + " 简历隐私 联系方式 脱敏 合规 审计");
            values.add("候选人个人信息保护 简历原文 联系方式保存规则");
        }
        if (containsAny(lower, "岗位", "职责", "要求", "技术栈", "jd", "java", "spring", "rag", "向量")) {
            values.add(query + " 岗位职责 任职要求 技术栈 加分项");
            values.add("岗位 JD 技能要求 项目经验 匹配规则");
        }
        if (containsAny(lower, "远程", "居家", "城市", "到岗", "remote", "hybrid")) {
            values.add(query + " 远程办公 城市要求 到岗时间 工作地点");
            values.add("工作地点政策 远程混合办公 候选人所在地");
        }
        if (values.isEmpty()) {
            values.add(query + " 招聘政策 候选人沟通 处理规则");
            values.add("招聘智能助手知识库 FAQ 流程 合规");
        }
        return values;
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }
}
