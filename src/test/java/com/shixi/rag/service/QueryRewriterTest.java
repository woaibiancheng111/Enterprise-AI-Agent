package com.shixi.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QueryRewriterTest {

    private final QueryRewriter queryRewriter = new QueryRewriter();

    @Test
    void rewriteExpandsEnterpriseHrSynonyms() {
        String rewritten = queryRewriter.rewrite("年假怎么申请");

        assertTrue(rewritten.contains("年假"));
        assertTrue(rewritten.contains("带薪年假"));
        assertTrue(rewritten.contains("年度假期"));
    }

    @Test
    void recognizeIntentClassifiesApplicationAndFeedback() {
        assertEquals(QueryRewriter.IntentType.APPLICATION, queryRewriter.recognizeIntent("我想申请报销"));
        assertEquals(QueryRewriter.IntentType.FEEDBACK, queryRewriter.recognizeIntent("我要投诉会议室一直没人处理"));
    }

    @Test
    void decomposeSplitsCompoundQuestion() {
        List<String> subQueries = queryRewriter.decompose("年假怎么申请？报销需要什么材料？");

        assertEquals(2, subQueries.size());
        assertTrue(subQueries.get(0).contains("年假"));
        assertTrue(subQueries.get(1).contains("报销"));
    }
}
