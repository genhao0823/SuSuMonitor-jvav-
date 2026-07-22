package com.susumonitor.server.module.server.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 接收服务器分页、关键词和排序查询参数。
 */
// 自动生成当前 DTO 的 getter、setter、toString、equals 和 hashCode 方法。
@Data
public class ServerQueryRequest {

    // 限制页码最小值为 1。
    @Min(1)
    private Integer page = 1;

    // 限制每页数量最小值为 1。
    @Min(1)
    // 限制每页数量最大值为 100，避免单次查询返回过多数据。
    @Max(100)
    // 将 Java 的 pageSize 属性映射为接口 JSON 字段 page_size。
    @JsonProperty("page_size")
    private Integer pageSize = 20;

    // 限制搜索关键词最大长度为 100 个字符，允许不填写。
    @Size(max = 100)
    private String keyword;

    // 排序字段只允许映射层支持的固定白名单，避免动态 SQL 注入。
    @Pattern(regexp = "^(id|name|host|status|created_at|updated_at)$")
    // 将 Java 的 sortBy 属性映射为接口 JSON 字段 sort_by。
    @JsonProperty("sort_by")
    private String sortBy = "id";

    // 排序方向只允许小写 asc 或 desc。
    @Pattern(regexp = "^(asc|desc)$")
    // 将 Java 的 sortOrder 属性映射为接口 JSON 字段 sort_order。
    @JsonProperty("sort_order")
    private String sortOrder = "desc";
}
