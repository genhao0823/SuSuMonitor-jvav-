package com.susumonitor.server.module.server.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

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
    private Integer pageSize = 20;

    // 限制搜索关键词最大长度为 100 个字符，允许不填写。
    @Size(max = 100)
    private String keyword;

    private String sortBy = "id";

    private String sortOrder = "desc";
}
