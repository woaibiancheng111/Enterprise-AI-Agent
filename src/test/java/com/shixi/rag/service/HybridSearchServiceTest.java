package com.shixi.rag.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class HybridSearchServiceTest {

    private final HybridSearchService service = new HybridSearchService(mock(), mock(), mock());

    @Test
    void bm25StatsUseActualAverageDocumentLength() {
        HybridSearchService.Bm25Stats stats = service.buildBm25Stats(List.of(
                "年假 申请",
                "年假 申请 流程 审批"
        ));

        assertEquals(3.0, stats.avgDocLen());
        assertEquals(2, stats.totalDocuments());
    }

    @Test
    void rareTermsReceiveHigherIdfThanCommonTerms() {
        HybridSearchService.Bm25Stats stats = service.buildBm25Stats(List.of(
                "年假 申请",
                "年假 审批",
                "病假 材料"
        ));

        assertTrue(stats.idf("病假") > stats.idf("年假"));
    }

    @Test
    void bm25RewardsMatchingDocumentMoreThanUnrelatedDocument() {
        HybridSearchService.Bm25Stats stats = service.buildBm25Stats(List.of(
                "员工请假流程 需要提交请假原因",
                "会议室投影设备维护记录"
        ));
        String[] queryTerms = service.tokenize("请假");

        double leaveScore = service.calculateBM25("员工请假流程 需要提交请假原因", queryTerms, stats);
        double unrelatedScore = service.calculateBM25("会议室投影设备维护记录", queryTerms, stats);

        assertTrue(leaveScore > 0);
        assertEquals(0.0, unrelatedScore);
        assertTrue(leaveScore > unrelatedScore);
    }
}
