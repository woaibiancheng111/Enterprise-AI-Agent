package com.shixi.advisor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.advisor.api.*;
import org.springframework.ai.chat.model.MessageAggregator;
import reactor.core.publisher.Flux;

/**
 * @author: shixi
 * @description:自定义日志 Advisor
 * 打印info级别日志，只输出单次用户提示词和回复
 */

@Slf4j
public class MyLogAdvisor implements CallAroundAdvisor, StreamAroundAdvisor {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public String getName() {
        return this.getClass().getSimpleName();
    }
    public int getOrder() {
        return 10;
    }

    private AdvisedRequest before(AdvisedRequest request) {
        log.info("AI request: {}", request.userText());
        return request;
    }

    private void observeAfter(AdvisedResponse advisedResponse) {
        String rawText = advisedResponse.response().getResult().getOutput().getText();
        log.info("AI response:\n{}", formatResponseText(rawText));
    }

    private String formatResponseText(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String trimmed = text.trim();
        if ((trimmed.startsWith("{") && trimmed.endsWith("}"))
                || (trimmed.startsWith("[") && trimmed.endsWith("]"))) {
            try {
                Object parsed = OBJECT_MAPPER.readValue(trimmed, Object.class);
                return OBJECT_MAPPER.writeValueAsString(parsed);
            } catch (Exception ignored) {
                // Fallback to raw model output when it is not strict JSON.
            }
        }
        return text;
    }


    public AdvisedResponse aroundCall(AdvisedRequest advisedRequest, CallAroundAdvisorChain chain) {
        advisedRequest = this.before(advisedRequest);
        AdvisedResponse advisedResponse = chain.nextAroundCall(advisedRequest);
        this.observeAfter(advisedResponse);
        return advisedResponse;
    }

    public Flux<AdvisedResponse> aroundStream(AdvisedRequest advisedRequest, StreamAroundAdvisorChain chain) {
        advisedRequest = this.before(advisedRequest);
        Flux<AdvisedResponse> advisedResponses = chain.nextAroundStream(advisedRequest);
        return new MessageAggregator().aggregateAdvisedResponse(advisedResponses, this::observeAfter);
    }
}
