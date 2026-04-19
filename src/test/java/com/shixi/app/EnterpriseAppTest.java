package com.shixi.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
class EnterpriseAppTest {

    @Autowired
    private EnterpriseApp enterpriseApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();

        String message = "你好，我是志林";
        String answer = enterpriseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        message = "我想让另一半（时曦）更健康";
        answer = enterpriseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        message = "我的另一半叫什么？";
        answer = enterpriseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChat() {
        String chatId = "125bbaff-bc10-46d4-9ec1-8b9decd6ebc4_4cbf7518";
        String message = "你好，我的头疼";
        String answer = enterpriseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void testLongChatMemory(){
        String chatId = "test1";
//        String message = "你好，我是时曦";
//        String answer = enterpriseApp.doChat(message, chatId);
//        Assertions.assertNotNull(answer);
        String message = "我是谁你知道吗？";
        String answer = enterpriseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }


    @Test
    void doChatWithReport() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我叫shixi,今天肚子疼，想要咨询一下你";
        EnterpriseApp.EmployeeTicket healthReport = enterpriseApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(healthReport);
    }

    @Test
    void doChatWithKnowledgeBase() {
        String chatId = UUID.randomUUID().toString();
        String message = "你好，我叫shixi,今天眼睛不舒服，需要请假";
        String answer = enterpriseApp.doChatWithKnowledgeBase(message, chatId);
        Assertions.assertNotNull(answer);

    }
}
