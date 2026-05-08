package com.shixi.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtTokenServiceTest {

    @Test
    void createAndParseTokenRoundTripsCurrentUser() {
        JwtTokenService service = new JwtTokenService(new ObjectMapper(), "test-secret", 3600);
        CurrentUser user = new CurrentUser("U001", "zhangsan", "E001", "张三", "EMPLOYEE");

        JwtTokenService.TokenIssue issue = service.createToken(user);
        CurrentUser parsed = service.parse(issue.token());

        assertEquals(user, parsed);
    }

    @Test
    void parseRejectsTamperedPayload() {
        JwtTokenService service = new JwtTokenService(new ObjectMapper(), "test-secret", 3600);
        String token = service.createToken(new CurrentUser("U001", "zhangsan", "E001", "张三", "EMPLOYEE")).token();
        String[] parts = token.split("\\.");
        String tamperedPayload = Base64.getUrlEncoder().withoutPadding()
                .encodeToString("{\"sub\":\"U999\",\"exp\":9999999999}".getBytes(StandardCharsets.UTF_8));

        String tamperedToken = parts[0] + "." + tamperedPayload + "." + parts[2];

        assertThrows(UnauthorizedException.class, () -> service.parse(tamperedToken));
    }

    @Test
    void parseRejectsExpiredToken() throws InterruptedException {
        JwtTokenService service = new JwtTokenService(new ObjectMapper(), "test-secret", 0);
        String token = service.createToken(new CurrentUser("U001", "zhangsan", "E001", "张三", "EMPLOYEE")).token();

        Thread.sleep(1100);

        assertThrows(UnauthorizedException.class, () -> service.parse(token));
    }
}
