package com.ink.admin.service;

import com.ink.admin.entity.Channel;
import com.ink.admin.mapper.ChannelMapper;
import com.ink.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 通道配置服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChannelService {

    private final ChannelMapper channelMapper;

    /**
     * 获取通道列表（分页）
     */
    public List<Channel> getChannelList(int page, int size) {
        return channelMapper.findByPage((page - 1) * size, size);
    }

    /**
     * 搜索通道列表
     */
    public List<Channel> searchChannels(String keyword, int page, int size) {
        return channelMapper.search(keyword, (page - 1) * size, size);
    }

    /**
     * 获取通道总数
     */
    public int getChannelCount() {
        return channelMapper.count();
    }

    /**
     * 搜索通道总数
     */
    public int getChannelCountByKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return channelMapper.count();
        }
        return channelMapper.countByKeyword(keyword);
    }

    /**
     * 根据ID获取通道
     */
    public Channel getChannelById(Long id) {
        Channel channel = channelMapper.findById(id);
        if (channel == null) {
            throw new BusinessException("通道不存在");
        }
        return channel;
    }

    /**
     * 创建通道
     */
    @Transactional
    public void createChannel(Channel channel) {
        if (channelMapper.existsByCode(channel.getCode())) {
            throw new BusinessException("通道编码已存在");
        }
        channel.setCreateTime(LocalDateTime.now());
        channel.setUpdateTime(LocalDateTime.now());
        if (channel.getStatus() == null) {
            channel.setStatus(1);
        }
        if (channel.getVersion() == null) {
            channel.setVersion(0x20);
        }
        if (channel.getHeartbeatInterval() == null) {
            channel.setHeartbeatInterval(60);
        }
        if (channel.getReconnectInterval() == null) {
            channel.setReconnectInterval(10);
        }
        if (channel.getMaxReconnectInterval() == null) {
            channel.setMaxReconnectInterval(60);
        }
        if (channel.getConnectTimeout() == null) {
            channel.setConnectTimeout(5000);
        }
        if (channel.getMaxConcurrent() == null) {
            channel.setMaxConcurrent(10);
        }

        if (channelMapper.insert(channel) <= 0) {
            throw new BusinessException("创建通道失败");
        }
        log.info("通道创建成功: code={}, name={}", channel.getCode(), channel.getName());
    }

    /**
     * 更新通道
     */
    @Transactional
    public void updateChannel(Channel channel) {
        Channel existing = channelMapper.findById(channel.getId());
        if (existing == null) {
            throw new BusinessException("通道不存在");
        }
        if (!existing.getCode().equals(channel.getCode()) && channelMapper.existsByCode(channel.getCode())) {
            throw new BusinessException("通道编码已存在");
        }
        channel.setUpdateTime(LocalDateTime.now());
        if (channelMapper.update(channel) <= 0) {
            throw new BusinessException("更新通道失败");
        }
        log.info("通道更新成功: id={}, code={}", channel.getId(), channel.getCode());
    }

    /**
     * 删除通道
     */
    @Transactional
    public void deleteChannel(Long id) {
        if (channelMapper.findById(id) == null) {
            throw new BusinessException("通道不存在");
        }
        channelMapper.deleteById(id);
        log.info("通道删除成功: id={}", id);
    }

    /**
     * 启用通道
     */
    @Transactional
    public void enableChannel(Long id) {
        if (channelMapper.findById(id) == null) {
            throw new BusinessException("通道不存在");
        }
        channelMapper.updateStatus(id, 1);
        log.info("通道启用成功: id={}", id);
    }

    /**
     * 禁用通道
     */
    @Transactional
    public void disableChannel(Long id) {
        if (channelMapper.findById(id) == null) {
            throw new BusinessException("通道不存在");
        }
        channelMapper.updateStatus(id, 0);
        log.info("通道禁用成功: id={}", id);
    }
}
