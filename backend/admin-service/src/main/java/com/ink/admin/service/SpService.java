package com.ink.admin.service;

import com.ink.admin.entity.Sp;
import com.ink.admin.entity.SpTransaction;
import com.ink.admin.mapper.SpMapper;
import com.ink.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 下游客户管理服务类
 * 维护 ink_sp 客户表与 ink_sp_channel 通道绑定关系
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpService {

    private final SpMapper spMapper;

    /**
     * 获取客户列表（分页，附带绑定通道）
     */
    public List<Sp> getSpList(String keyword, int page, int size) {
        List<Sp> list = (keyword != null && !keyword.isBlank())
                ? spMapper.search(keyword, (page - 1) * size, size)
                : spMapper.findByPage((page - 1) * size, size);
        list.forEach(sp -> sp.setChannelCodes(spMapper.findChannelCodes(sp.getSpId())));
        return list;
    }

    /**
     * 获取客户总数
     */
    public int getSpCount(String keyword) {
        return (keyword != null && !keyword.isBlank())
                ? spMapper.countByKeyword(keyword)
                : spMapper.count();
    }

    /**
     * 根据ID获取客户（附带绑定通道）
     */
    public Sp getSpById(Long id) {
        Sp sp = spMapper.findById(id);
        if (sp == null) {
            throw new BusinessException("客户不存在");
        }
        sp.setChannelCodes(spMapper.findChannelCodes(sp.getSpId()));
        return sp;
    }

    /**
     * 创建客户
     */
    @Transactional
    public void createSp(Sp sp) {
        if (sp.getSpId() == null || sp.getSpId().isBlank()) {
            throw new BusinessException("客户标识不能为空");
        }
        if (sp.getSpSecret() == null || sp.getSpSecret().isBlank()) {
            throw new BusinessException("共享密钥不能为空");
        }
        if (spMapper.existsBySpId(sp.getSpId())) {
            throw new BusinessException("客户标识已存在");
        }
        sp.setCreateTime(LocalDateTime.now());
        sp.setUpdateTime(LocalDateTime.now());
        if (sp.getStatus() == null) {
            sp.setStatus(1);
        }
        if (sp.getBalance() == null) {
            sp.setBalance(BigDecimal.ZERO);
        }
        if (sp.getUnitPrice() == null) {
            sp.setUnitPrice(new BigDecimal("0.05"));
        }
        if (sp.getRateLimit() == null) {
            sp.setRateLimit(20);
        }
        if (spMapper.insert(sp) <= 0) {
            throw new BusinessException("创建客户失败");
        }
        // 创建时一并绑定通道
        bindChannelsInternal(sp.getSpId(), sp.getChannelCodes());
        log.info("客户创建成功: spId={}, name={}", sp.getSpId(), sp.getName());
    }

    /**
     * 更新客户（客户标识不可修改）
     */
    @Transactional
    public void updateSp(Sp sp) {
        Sp existing = spMapper.findById(sp.getId());
        if (existing == null) {
            throw new BusinessException("客户不存在");
        }
        if (!existing.getSpId().equals(sp.getSpId())) {
            throw new BusinessException("客户标识不可修改");
        }
        sp.setUpdateTime(LocalDateTime.now());
        if (sp.getStatus() == null) {
            sp.setStatus(existing.getStatus());
        }
        if (sp.getUnitPrice() == null) {
            sp.setUnitPrice(existing.getUnitPrice());
        }
        if (sp.getRateLimit() == null) {
            sp.setRateLimit(existing.getRateLimit());
        }
        if (spMapper.update(sp) <= 0) {
            throw new BusinessException("更新客户失败");
        }
        log.info("客户更新成功: id={}, spId={}", sp.getId(), sp.getSpId());
    }

    /**
     * 删除客户（同时清理通道绑定）
     */
    @Transactional
    public void deleteSp(Long id) {
        Sp existing = spMapper.findById(id);
        if (existing == null) {
            throw new BusinessException("客户不存在");
        }
        spMapper.deleteChannelsBySpId(existing.getSpId());
        spMapper.deleteById(id);
        log.info("客户删除成功: id={}, spId={}", id, existing.getSpId());
    }

    /**
     * 启用客户
     */
    @Transactional
    public void enableSp(Long id) {
        if (spMapper.findById(id) == null) {
            throw new BusinessException("客户不存在");
        }
        spMapper.updateStatus(id, 1);
        log.info("客户启用成功: id={}", id);
    }

    /**
     * 禁用客户
     */
    @Transactional
    public void disableSp(Long id) {
        if (spMapper.findById(id) == null) {
            throw new BusinessException("客户不存在");
        }
        spMapper.updateStatus(id, 0);
        log.info("客户禁用成功: id={}", id);
    }

    /**
     * 查询客户绑定通道
     */
    public List<String> getChannelCodes(Long id) {
        Sp sp = spMapper.findById(id);
        if (sp == null) {
            throw new BusinessException("客户不存在");
        }
        return spMapper.findChannelCodes(sp.getSpId());
    }

    /**
     * 全量替换客户绑定通道
     */
    @Transactional
    public void bindChannels(Long id, List<String> channelCodes) {
        Sp sp = spMapper.findById(id);
        if (sp == null) {
            throw new BusinessException("客户不存在");
        }
        spMapper.deleteChannelsBySpId(sp.getSpId());
        bindChannelsInternal(sp.getSpId(), channelCodes);
        log.info("客户通道绑定更新: spId={}, channels={}", sp.getSpId(), channelCodes);
    }

    private void bindChannelsInternal(String spId, List<String> channelCodes) {
        if (channelCodes == null || channelCodes.isEmpty()) {
            return;
        }
        for (String code : channelCodes) {
            if (code != null && !code.isBlank()) {
                spMapper.insertChannel(spId, code.trim());
            }
        }
    }

    // ==================== 余额与流水 ====================

    /**
     * 充值/调账（金额可正可负）
     */
    @Transactional
    public BigDecimal recharge(Long id, BigDecimal amount, String remark) {
        Sp sp = spMapper.findById(id);
        if (sp == null) {
            throw new BusinessException("客户不存在");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException("充值金额不能为 0");
        }
        if (spMapper.addBalance(sp.getSpId(), amount) <= 0) {
            throw new BusinessException("充值失败");
        }
        BigDecimal balanceAfter = spMapper.findBalance(sp.getSpId());
        if (balanceAfter != null && balanceAfter.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException("调账后余额不能为负");
        }
        SpTransaction tx = new SpTransaction()
                .setSpId(sp.getSpId())
                .setType("RECHARGE")
                .setAmount(amount)
                .setBalanceAfter(balanceAfter)
                .setRemark(remark != null && !remark.isBlank() ? remark : "管理员充值/调账")
                .setCreateTime(LocalDateTime.now());
        spMapper.insertTransaction(tx);
        log.info("客户充值成功: spId={}, amount={}, balanceAfter={}", sp.getSpId(), amount, balanceAfter);
        return balanceAfter;
    }

    /**
     * 分页查询客户余额流水
     */
    public List<SpTransaction> getTransactions(Long id, int page, int size) {
        Sp sp = spMapper.findById(id);
        if (sp == null) {
            throw new BusinessException("客户不存在");
        }
        return spMapper.findTransactionsByPage(sp.getSpId(), (page - 1) * size, size);
    }

    /**
     * 查询客户流水总数
     */
    public int getTransactionCount(Long id) {
        Sp sp = spMapper.findById(id);
        if (sp == null) {
            throw new BusinessException("客户不存在");
        }
        return spMapper.countTransactions(sp.getSpId());
    }
}
