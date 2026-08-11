package com.ink.admin.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 敏感词实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SensitiveWord {

    private Long id;

    /** 敏感词 */
    private String word;

    /** 状态：0-禁用, 1-启用 */
    private Integer status;

    /** 创建时间 */
    private LocalDateTime createTime;
}
