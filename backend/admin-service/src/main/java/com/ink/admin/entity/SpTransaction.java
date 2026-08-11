package com.ink.admin.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 客户余额流水实体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class SpTransaction {

    private Long id;

    /** 客户标识 */
    private String spId;

    /** 类型：RECHARGE-充值, DEDUCT-扣费, REFUND-返还 */
    private String type;

    /** 变动金额（正数入账，负数出账） */
    private BigDecimal amount;

    /** 变动后余额 */
    private BigDecimal balanceAfter;

    /** 关联消息ID（扣费/返还时） */
    private String refMsgId;

    /** 备注 */
    private String remark;

    /** 创建时间 */
    private LocalDateTime createTime;
}
