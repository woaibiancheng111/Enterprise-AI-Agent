package com.shixi.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()) || isPublicPath(request)) {
            return true;
        }

        String token = extractToken(request);
        CurrentUserContext.set(jwtTokenService.parse(token));
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }

    private String extractToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        String accessToken = request.getParameter("access_token");
        if (accessToken != null && !accessToken.isBlank()) {
            return accessToken.trim();
        }
        throw new UnauthorizedException("请先登录");
    }

    private boolean isPublicPath(HttpServletRequest request) {
        String path = normalizePath(request);
        return path.equals("/auth/login")
                || path.equals("/enterprise/health")
                || path.equals("/mcp/message")
                || path.equals("/sse")
                || path.startsWith("/swagger-ui")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/webjars")
                || path.equals("/doc.html")
                || path.equals("/favicon.ico")
                || path.equals("/actuator/health");
    }

    private String normalizePath(HttpServletRequest request) {
        String path = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (contextPath != null && !contextPath.isBlank() && path.startsWith(contextPath)) {
            path = path.substring(contextPath.length());
        }
        if (path.startsWith("/api/")) {
            path = path.substring("/api".length());
        }
        return path.isBlank() ? "/" : path;
    }
}
