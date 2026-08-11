package com.ink.admin.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 短信签名实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Signature {

    private Long id;

    /** 签名内容（如：【INK科技】） */
    private String content;

    /** 归属客户标识（空=平台全局） */
    private String spId;

    /** 状态：0-待审核, 1-已通过, 2-已驳回 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
