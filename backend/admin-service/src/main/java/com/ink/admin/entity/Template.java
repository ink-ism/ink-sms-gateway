package com.ink.admin.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 短信模板实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class Template {

    private Long id;

    /** 模板名称 */
    private String name;

    /** 模板内容 */
    private String content;

    /** 关联签名ID（可空） */
    private Long signatureId;

    /** 关联签名内容（查询时填充，非数据库字段） */
    private String signatureContent;

    /** 状态：0-待审核, 1-已通过, 2-已驳回 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 更新时间 */
    private LocalDateTime updateTime;
}
