package com.ink.admin.audit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ink.admin.entity.AuditLog;
import com.ink.admin.mapper.AuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.annotation.Annotation;
import java.time.LocalDateTime;

/**
 * 审计日志切面
 * 拦截标注 @Audit 的 Controller 写操作方法，操作成功后记录审计日志
 * 管理员身份从网关透传的 X-Username / X-User-Id 请求头获取
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogMapper auditLogMapper;
    private final ObjectMapper objectMapper;

    /** detail 字段最大长度（对应 ink_audit_log.detail VARCHAR(1000)） */
    private static final int MAX_DETAIL_LENGTH = 1000;

    @AfterReturning(pointcut = "@annotation(audit)", returning = "result")
    public void record(JoinPoint joinPoint, Audit audit, Object result) {
        try {
            HttpServletRequest request = currentRequest();

            AuditLog auditLog = new AuditLog()
                    .setModule(audit.module())
                    .setAction(audit.action())
                    .setUsername(request != null ? request.getHeader("X-Username") : null)
                    .setAdminId(parseAdminId(request))
                    .setTarget(resolveTarget(joinPoint))
                    .setDetail(buildDetail(joinPoint))
                    .setIp(resolveIp(request))
                    .setCreateTime(LocalDateTime.now());

            auditLogMapper.insert(auditLog);
        } catch (Exception e) {
            // 审计失败不影响主流程
            log.warn("审计日志记录失败: module={}, action={}, error={}", audit.module(), audit.action(), e.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        ServletRequestAttributes attrs =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        return attrs != null ? attrs.getRequest() : null;
    }

    private Long parseAdminId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String userId = request.getHeader("X-User-Id");
        if (userId == null || userId.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 提取操作对象：优先取 @PathVariable 标注的参数
     */
    private String resolveTarget(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Annotation[][] paramAnnotations = signature.getMethod().getParameterAnnotations();
        Object[] args = joinPoint.getArgs();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            for (Annotation annotation : paramAnnotations[i]) {
                if (annotation instanceof PathVariable && args[i] != null) {
                    if (sb.length() > 0) {
                        sb.append(",");
                    }
                    sb.append(args[i]);
                }
            }
        }
        return sb.length() > 0 ? sb.toString() : null;
    }

    /**
     * 构建参数摘要（脱敏后截断）
     */
    private String buildDetail(JoinPoint joinPoint) {
        try {
            Object[] args = joinPoint.getArgs();
            if (args == null || args.length == 0) {
                return null;
            }
            String json = objectMapper.writeValueAsString(args);
            // 敏感字段脱敏
            json = json.replaceAll("\"(password|sharedSecret|spSecret|newPassword|oldPassword)\"\\s*:\\s*\"[^\"]*\"",
                    "\"$1\":\"***\"");
            return json.length() > MAX_DETAIL_LENGTH ? json.substring(0, MAX_DETAIL_LENGTH) : json;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析客户端IP（优先 X-Forwarded-For 首段）
     */
    private String resolveIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
