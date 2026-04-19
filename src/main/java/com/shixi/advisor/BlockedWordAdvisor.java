package com.shixi.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.AdvisedRequest;
import org.springframework.ai.chat.client.advisor.api.AdvisedResponse;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAroundAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAroundAdvisorChain;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Slf4j
public class BlockedWordAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private final List<String> blockedWords;

    public BlockedWordAdvisor(List<String> blockedWords) {
        this.blockedWords = blockedWords == null ? List.of() : blockedWords.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private AdvisedRequest before(AdvisedRequest request) {
        String userText = request.userText();
        if (userText == null || userText.isBlank()) {
            return request;
        }
        String normalizedText = userText.toLowerCase(Locale.ROOT);
        for (String blockedWord : blockedWords) {
            if (normalizedText.contains(blockedWord)) {
                log.warn("Blocked request for sensitive word hit: {}", blockedWord);
                throw new IllegalArgumentException("输入包含敏感词，请调整后再试。");
            }
        }
        return request;
    }

    @Override
    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        return chain.nextAroundCall(before(advisedRequest));
    }

    @Override
    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        return chain.nextAroundStream(before(advisedRequest));
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
