package com.seckill.config;

import com.alibaba.csp.sentinel.adapter.spring.webmvc.callback.RequestOriginParser;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.param.ParamFlowRuleManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;

/**
 * Sentinel 限流配置
 * 面试考点：QPS限流 + IP热点参数限流，拦截高频恶意请求
 */
@Configuration
public class SentinelConfig {

    /**
     * 配置热点参数限流规则：基于IP限流（单个IP每秒最多10个请求）
     */
    @PostConstruct
    public void initParamFlowRules() {
        ParamFlowRule rule = new ParamFlowRule("doSeckill")
                .setParamIdx(0)          // 第0个args参数是IP
                .setCount(10)            // 单个IP每秒最多10个请求
                .setDurationInSec(1)
                .setGrade(RuleConstant.FLOW_GRADE_QPS);
        ParamFlowRuleManager.loadRules(Collections.singletonList(rule));
    }

    /**
     * 请求来源解析（Sentinel授权规则使用，可选）
     */
    @Bean
    public RequestOriginParser requestOriginParser() {
        return new RequestOriginParser() {
            @Override
            public String parseOrigin(HttpServletRequest request) {
                String ip = request.getHeader("X-Forwarded-For");
                if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("Proxy-Client-IP");
                }
                if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getHeader("WL-Proxy-Client-IP");
                }
                if (!StringUtils.hasText(ip) || "unknown".equalsIgnoreCase(ip)) {
                    ip = request.getRemoteAddr();
                }
                return ip != null ? ip.split(",")[0] : "unknown";
            }
        };
    }
}