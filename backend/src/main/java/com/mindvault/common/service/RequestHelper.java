package com.mindvault.common.service;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * HTTP 请求辅助工具
 *
 * 提供从当前请求上下文中提取客户端信息的静态方法。
 * 当前仅实现获取客户端真实 IP，后续可扩展获取 User-Agent、Referer 等。
 *
 * IP 提取优先级：X-Forwarded-For → X-Real-IP → getRemoteAddr
 * 经过代理时 X-Forwarded-For 可能包含逗号分隔的多个 IP，取第一个（客户端真实 IP）。
 */
@Component
public class RequestHelper {

    /** 获取客户端真实 IP 地址 */
    public String getClientIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ip = request.getHeader("X-Forwarded-For");
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("X-Real-IP");
            }
            if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            if (ip != null && ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
            return ip;
        } catch (Exception e) {
            return null;
        }
    }
}