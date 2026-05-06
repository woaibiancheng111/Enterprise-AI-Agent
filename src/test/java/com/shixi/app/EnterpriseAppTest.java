package com.shixi.app;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

@SpringBootTest
@Disabled("Requires a live DashScope key and MySQL; run manually for end-to-end demo verification.")
class EnterpriseAppTest {

    @Autowired
    private EnterpriseApp enterpriseApp;

    @Test
    void testChat() {
        String chatId = UUID.randomUUID().toString();

        String message = "你好，我是张三";
        String answer = enterpriseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        message = "我想了解年假制度";
        answer = enterpriseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);

        message = "我刚才问的是哪类制度？";
        answer = enterpriseApp.doChat(message, chatId);
        Assertions.assertNotNull(answer);
    }

    @Test
    void doChat() {
        String chatId = "125bbaff-bc10-46d4-9ec1-8b9decd6ebc4_4cbf7518";
        String message = "请问报销流程是什么？";
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
        String message = "我是张三，技术部，想申请下周一到周三的年假";
        EnterpriseApp.EmployeeTicket ticket = enterpriseApp.doChatWithReport(message, chatId);
        Assertions.assertNotNull(ticket);
    }

    @Test
    void doChatWithKnowledgeBase() {
        String chatId = UUID.randomUUID().toString();
        String message = "年假需要提前多久申请？";
        String answer = enterpriseApp.doChatWithKnowledgeBase(message, chatId);
        Assertions.assertNotNull(answer);

    }
}
