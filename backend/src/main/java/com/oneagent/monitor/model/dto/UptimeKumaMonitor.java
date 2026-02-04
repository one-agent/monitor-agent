package com.oneagent.monitor.model.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Uptime Kuma Monitor 数据模型
 * 对应 Uptime Kuma Webhook 中的 monitor 对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UptimeKumaMonitor {
    
    /**
     * 监控项 ID
     */
    @JsonProperty("id")
    private Integer id;
    
    /**
     * 监控项名称
     */
    @JsonProperty("name")
    private String name;
    
    /**
     * 监控 URL
     */
    @JsonProperty("url")
    private String url;
    
    /**
     * HTTP 方法
     */
    @JsonProperty("method")
    private String method;
    
    /**
     * 请求体
     */
    @JsonProperty("body")
    private String body;
    
    /**
     * 请求头
     */
    @JsonProperty("headers")
    private String headers;
    
    /**
     * 基础认证用户名
     */
    @JsonProperty("basic_auth_user")
    private String basicAuthUser;
    
    /**
     * 基础认证密码
     */
    @JsonProperty("basic_auth_pass")
    private String basicAuthPass;
    
    /**
     * 主机名（用于 ping 监控）
     */
    @JsonProperty("hostname")
    private String hostname;
    
    /**
     * 端口
     */
    @JsonProperty("port")
    private Integer port;
    
    /**
     * 最大重试次数
     */
    @JsonProperty("maxretries")
    private Integer maxretries;
    
    /**
     * 权重
     */
    @JsonProperty("weight")
    private Integer weight;
    
    /**
     * 是否启用
     */
    @JsonProperty("active")
    private Integer active;
    
    /**
     * 监控类型
     * http, ping, tcp, dns, push, steam, gamedig, docker, mqtt
     */
    @JsonProperty("type")
    private String type;
    
    /**
     * 检查间隔（秒）
     */
    @JsonProperty("interval")
    private Integer interval;
    
    /**
     * 重试间隔（秒）
     */
    @JsonProperty("retryInterval")
    private Integer retryInterval;
    
    /**
     * 关键字（用于内容验证）
     */
    @JsonProperty("keyword")
    private String keyword;
    
    /**
     * 忽略 TLS 错误
     */
    @JsonProperty("ignoreTls")
    private Boolean ignoreTls;
    
    /**
     * 反向监控（状态反转）
     */
    @JsonProperty("upsideDown")
    private Boolean upsideDown;
    
    /**
     * 最大重定向次数
     */
    @JsonProperty("maxredirects")
    private Integer maxredirects;
    
    /**
     * 接受的 HTTP 状态码
     */
    @JsonProperty("accepted_statuscodes")
    private List<String> acceptedStatuscodes;
    
    /**
     * DNS 解析类型
     */
    @JsonProperty("dns_resolve_type")
    private String dnsResolveType;
    
    /**
     * DNS 解析服务器
     */
    @JsonProperty("dns_resolve_server")
    private String dnsResolveServer;
    
    /**
     * DNS 上次解析结果
     */
    @JsonProperty("dns_last_result")
    private String dnsLastResult;
    
    /**
     * Push Token
     */
    @JsonProperty("pushToken")
    private String pushToken;
    
    /**
     * 通知 ID 列表
     */
    @JsonProperty("notificationIDList")
    private Map<String, Boolean> notificationIdList;
    
    /**
     * 标签
     */
    @JsonProperty("tags")
    private List<String> tags;
}