package com.example.tasktracker.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AuthRateLimitFilter extends OncePerRequestFilter {
    private static final int MAX_TRACKED_KEYS = 10_000;

    private final RateLimitProperties properties;
    private final Clock clock;
    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    public AuthRateLimitFilter(RateLimitProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !properties.enabled() || !"POST".equals(request.getMethod()) || limitFor(request.getRequestURI()) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Limit limit = limitFor(request.getRequestURI());
        long now = clock.millis();
        String key = request.getRequestURI() + "|" + request.getRemoteAddr();
        Window window = windows.compute(key, (ignored, current) ->
                current == null || current.isOver(now) ? new Window(now + limit.period().toMillis(), 1) : current.next());
        if(window.count() > limit.maxRequests()){
            reject(response, request.getRequestURI(), Math.max(1, (window.resetAt() - now + 999) / 1000));
            return;
        }
        if(windows.size() > MAX_TRACKED_KEYS){
            windows.values().removeIf(entry -> entry.isOver(now));
        }
        filterChain.doFilter(request, response);
    }

    private Limit limitFor(String path){
        return switch (path) {
            case "/auth/login" -> new Limit(properties.loginPerMinute(), Duration.ofMinutes(1));
            case "/auth/refresh" -> new Limit(properties.refreshPerMinute(), Duration.ofMinutes(1));
            case "/auth/register" -> new Limit(properties.registerPerHour(), Duration.ofHours(1));
            default -> null;
        };
    }

    private static void reject(HttpServletResponse response, String path, long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.getWriter().write("""
                {"type":"about:blank","title":"Too Many Requests","status":429,\
                "detail":"Too many attempts. Try again in %d seconds.","instance":"%s"}"""
                .formatted(retryAfterSeconds, path));
    }

    private record Limit(int maxRequests, Duration period) {
    }

    private record Window(long resetAt, int count) {
        boolean isOver(long now){
            return now >= resetAt;
        }

        Window next(){
            return new Window(resetAt, count + 1);
        }
    }
}
