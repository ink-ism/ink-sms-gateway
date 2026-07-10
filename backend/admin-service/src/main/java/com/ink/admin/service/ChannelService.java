package com.ink.admin.service;

import com.ink.admin.entity.Channel;
import com.ink.admin.mapper.ChannelMapper;
import com.ink.common.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    /**
     * 测试单个通道的连接状态
     */
    public Map<String, Object> testConnection(Long id) {
        Channel channel = channelMapper.findById(id);
        if (channel == null) {
            throw new BusinessException("通道不存在");
        }
        return doTcpTest(channel);
    }

    /**
     * 批量测试所有通道的连接状态
     */
    public Map<Long, Map<String, Object>> testAllConnections(List<Long> ids) {
        Map<Long, Map<String, Object>> results = new HashMap<>();
        for (Long id : ids) {
            Channel channel = channelMapper.findById(id);
            if (channel != null) {
                results.put(id, doTcpTest(channel));
            }
        }
        return results;
    }

    /**
     * TCP 连接测试
     */
    private Map<String, Object> doTcpTest(Channel channel) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", channel.getId());
        result.put("code", channel.getCode());

        if (channel.getStatus() != null && channel.getStatus() == 0) {
            result.put("connected", false);
            result.put("reason", "通道已禁用");
            return result;
        }

        long start = System.currentTimeMillis();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(channel.getHost(), channel.getPort()), 2000);
            long elapsed = System.currentTimeMillis() - start;
            result.put("connected", true);
            result.put("reason", "TCP连接成功");
            result.put("latency", elapsed);
        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;
            result.put("connected", false);
            result.put("reason", e.getMessage() != null ? e.getMessage() : "连接失败");
            result.put("latency", elapsed);
        }
        return result;
    }
}
