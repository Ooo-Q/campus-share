package com.campusshare.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;
import lombok.extern.slf4j.Slf4j;
import io.jsonwebtoken.JwtException;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final List<String> WHITELIST = Arrays.asList(
            "/auth/login",
            "/auth/register",
            "/files/public/**",
            "/actuator/**",
            "/error"
    );

    private final JwtService jwtService;
    private final AntPathMatcher matcher = new AntPathMatcher();

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected boolean shouldNotFilter(@NonNull HttpServletRequest request) {
        String method = request.getMethod();
        if (method != null && HttpMethod.OPTIONS.matches(method)) {
            return true;
        }
        String path = request.getRequestURI();
        if (path == null) {
            return false;
        }
        final String finalPath = path;
        return WHITELIST.stream()
                .anyMatch(pattern -> pattern != null && matcher.match(pattern, finalPath));
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7).trim();

                if (token.isEmpty()) {
                    log.warn("检测到空的JWT令牌，请求路径: {}", request.getRequestURI());
                } else {
                    try {
                        UserPrincipal principal = jwtService.parseToken(token);
                        UserContext.set(principal);
                        log.debug("JWT令牌解析成功，用户ID: {}, 路径: {}", principal.getId(), request.getRequestURI());
                    } catch (JwtException | IllegalArgumentException e) {
                        log.warn("JWT令牌解析失败: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
                        throw e;
                    }
                }
            } else if (authHeader != null) {
                log.warn("Authorization头格式不正确: {}, 请求路径: {}", authHeader.length() > 50 ? authHeader.substring(0, 50) + "..." : authHeader, request.getRequestURI());
            }
            filterChain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("JWT认证失败: {}, 请求路径: {}", e.getMessage(), request.getRequestURI());
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"success\":false,\"message\":\"令牌无效或已过期\"}");
        } catch (Exception ex) {
            log.error("JWT过滤器发生未预期的异常: {}, 请求路径: {}", ex.getMessage(), request.getRequestURI(), ex);
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
