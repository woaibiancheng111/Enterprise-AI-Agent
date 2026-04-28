package com.shixi.business.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TableName("tool_call_logs")
public class ToolCallLogEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String toolName;

    private String domain;

    private String actorId;

    private String targetEmployeeId;

    private String inputJson;

    private String resultSummary;

    private boolean success;

    private LocalDateTime createdAt;
}
