-- 创建数据库
CREATE DATABASE IF NOT EXISTS ink_sms_gateway DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 使用数据库
USE ink_sms_gateway;

-- 创建管理员表
CREATE TABLE IF NOT EXISTS ink_admin_user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    phone VARCHAR(11) NOT NULL UNIQUE COMMENT '手机号',
    nickname VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    role VARCHAR(20) NOT NULL COMMENT '角色：admin-管理员，super_admin-超级管理员',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    last_login_time DATETIME DEFAULT NULL COMMENT '最后登录时间',
    last_login_ip VARCHAR(45) DEFAULT NULL COMMENT '最后登录IP',
    INDEX idx_username (username),
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_status (status),
    INDEX idx_role (role),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='管理员表';

-- 插入默认管理员账号（密码为123456）
INSERT IGNORE INTO ink_admin_user (username, password, email, phone, nickname, role, status) VALUES
('admin', '$2a$10$HELbPEARrQpCTenHqp3VbeHBxlvbkvJ8BhTTYP1j8sirl.Qyo.ftW', 'admin@ink.com', '13800000000', '超级管理员', 'super_admin', 1);

-- 创建通道配置表
CREATE TABLE IF NOT EXISTS ink_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '通道名称',
    code VARCHAR(30) NOT NULL UNIQUE COMMENT '通道编码（唯一标识）',
    host VARCHAR(100) NOT NULL COMMENT '服务器地址',
    port INT NOT NULL COMMENT '服务器端口',
    sp_id VARCHAR(30) NOT NULL COMMENT 'SP企业代码',
    shared_secret VARCHAR(100) NOT NULL COMMENT '共享密钥',
    version INT DEFAULT 32 COMMENT 'CMPP版本号（默认0x20）',
    heartbeat_interval INT DEFAULT 60 COMMENT '心跳间隔（秒）',
    reconnect_interval INT DEFAULT 10 COMMENT '重连间隔（秒）',
    max_reconnect_interval INT DEFAULT 60 COMMENT '最大重连间隔（秒）',
    connect_timeout INT DEFAULT 5000 COMMENT '连接超时（毫秒）',
    max_concurrent INT DEFAULT 10 COMMENT '最大并发数',
    cost_price DECIMAL(8,4) DEFAULT 0.03 COMMENT '通道成本价（元/条）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    description VARCHAR(255) DEFAULT NULL COMMENT '通道描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_code (code),
    INDEX idx_status (status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道配置表';

-- 创建下行短信记录表
CREATE TABLE IF NOT EXISTS ink_sms_down (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_id VARCHAR(64) DEFAULT NULL COMMENT '客户端消息ID（唯一业务标识）',
    server_msg_id VARCHAR(32) DEFAULT NULL COMMENT 'CMPP服务端返回的消息ID（状态报告匹配用）',
    sp_id VARCHAR(30) DEFAULT NULL COMMENT '发送客户标识（REST发送为REST）',
    src_id VARCHAR(21) DEFAULT NULL COMMENT '源号码（接入号）',
    dest_terminal_id VARCHAR(21) NOT NULL COMMENT '目标手机号',
    msg_content TEXT DEFAULT NULL COMMENT '短信内容',
    msg_fmt INT DEFAULT 8 COMMENT '消息格式：0-ASCII, 8-UCS2, 15-GB2312',
    service_id VARCHAR(10) DEFAULT NULL COMMENT '业务类型',
    channel_code VARCHAR(30) DEFAULT NULL COMMENT '通道编码',
    status TINYINT DEFAULT 0 COMMENT '状态：0-已提交, 1-发送成功, 2-发送失败, 3-投递成功(DELIVRD)',
    status_report VARCHAR(20) DEFAULT NULL COMMENT '状态报告结果',
    error_msg VARCHAR(255) DEFAULT NULL COMMENT '错误信息',
    signature VARCHAR(30) DEFAULT NULL COMMENT '短信签名',
    carrier VARCHAR(20) DEFAULT NULL COMMENT '运营商（移动/联通/电信）',
    fee DECIMAL(8,4) DEFAULT NULL COMMENT '客户扣费金额（元）',
    cost DECIMAL(8,4) DEFAULT NULL COMMENT '通道成本金额（元）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT NULL COMMENT '响应时间（状态更新）',
    status_report_time DATETIME DEFAULT NULL COMMENT '回执时间（状态报告到达）',
    INDEX idx_dest_terminal (dest_terminal_id),
    INDEX idx_status (status),
    INDEX idx_channel_code (channel_code),
    INDEX idx_create_time (create_time),
    INDEX idx_sp_id (sp_id),
    INDEX idx_server_msg_id (server_msg_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下行短信记录表';

-- 创建上行短信记录表
CREATE TABLE IF NOT EXISTS ink_sms_up (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    msg_id VARCHAR(64) DEFAULT NULL COMMENT 'CMPP消息ID',
    sp_id VARCHAR(30) DEFAULT NULL COMMENT '路由目标客户标识',
    src_terminal_id VARCHAR(21) NOT NULL COMMENT '源手机号',
    dest_id VARCHAR(21) DEFAULT NULL COMMENT '目的号码（接入号）',
    msg_content TEXT DEFAULT NULL COMMENT '短信内容',
    msg_fmt INT DEFAULT 8 COMMENT '消息格式：0-ASCII, 8-UCS2, 15-GB2312',
    service_id VARCHAR(10) DEFAULT NULL COMMENT '业务类型',
    is_report TINYINT DEFAULT 0 COMMENT '是否状态报告：0-否, 1-是',
    report_stat VARCHAR(20) DEFAULT NULL COMMENT '状态报告结果',
    carrier VARCHAR(20) DEFAULT NULL COMMENT '运营商（移动/联通/电信）',
    channel_code VARCHAR(30) DEFAULT NULL COMMENT '通道编码',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_src_terminal (src_terminal_id),
    INDEX idx_is_report (is_report),
    INDEX idx_channel_code (channel_code),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='上行短信记录表';

-- 创建下游客户（SP）表
CREATE TABLE IF NOT EXISTS ink_sp (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sp_id VARCHAR(30) NOT NULL UNIQUE COMMENT '客户标识（CMPP Source_Addr）',
    sp_secret VARCHAR(100) NOT NULL COMMENT '共享密钥',
    name VARCHAR(50) NOT NULL COMMENT '客户名称',
    balance DECIMAL(12,4) DEFAULT 0 COMMENT '账户余额（元）',
    unit_price DECIMAL(8,4) DEFAULT 0.05 COMMENT '售价（元/条）',
    rate_limit INT DEFAULT 20 COMMENT '每秒发送上限（0-不限制）',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    description VARCHAR(255) DEFAULT NULL COMMENT '客户描述',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sp_id (sp_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下游客户表';

-- 创建客户-通道绑定表
CREATE TABLE IF NOT EXISTS ink_sp_channel (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sp_id VARCHAR(30) NOT NULL COMMENT '客户标识',
    channel_code VARCHAR(30) NOT NULL COMMENT '通道编码',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_sp_channel (sp_id, channel_code),
    INDEX idx_sp_id (sp_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户通道绑定表';

-- 创建通道级黑名单表（退订策略）
CREATE TABLE IF NOT EXISTS ink_blacklist (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    channel_code VARCHAR(30) NOT NULL COMMENT '通道编码',
    phone VARCHAR(21) NOT NULL COMMENT '退订手机号',
    keyword VARCHAR(50) DEFAULT NULL COMMENT '触发退订的关键字',
    source_mo_id VARCHAR(64) DEFAULT NULL COMMENT '触发退订的上行消息ID',
    expire_time DATETIME NOT NULL COMMENT '过期时间（过期后自动失效）',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    UNIQUE KEY uk_channel_phone (channel_code, phone),
    INDEX idx_expire_time (expire_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通道级退订黑名单表';

-- 创建下游离线推送队列表
CREATE TABLE IF NOT EXISTS ink_push_queue (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sp_id VARCHAR(30) NOT NULL COMMENT '目标客户标识',
    msg_type VARCHAR(10) NOT NULL COMMENT '消息类型：REPORT-状态报告, MO-上行短信',
    phone VARCHAR(21) DEFAULT NULL COMMENT '相关手机号',
    content TEXT DEFAULT NULL COMMENT '上行内容（MO）',
    server_msg_id VARCHAR(32) DEFAULT NULL COMMENT '原下行服务端消息ID（REPORT）',
    stat VARCHAR(20) DEFAULT NULL COMMENT '状态报告结果（REPORT）',
    msg_fmt INT DEFAULT 8 COMMENT '消息格式（MO）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待推送, 1-已推送',
    retry_count INT DEFAULT 0 COMMENT '重试次数',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sp_status (sp_id, status),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='下游离线推送队列表';

-- 创建客户余额流水表
CREATE TABLE IF NOT EXISTS ink_sp_transaction (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sp_id VARCHAR(30) NOT NULL COMMENT '客户标识',
    type VARCHAR(10) NOT NULL COMMENT '类型：RECHARGE-充值, DEDUCT-扣费, REFUND-返还',
    amount DECIMAL(12,4) NOT NULL COMMENT '变动金额（正数入账，负数出账）',
    balance_after DECIMAL(12,4) NOT NULL COMMENT '变动后余额',
    ref_msg_id VARCHAR(64) DEFAULT NULL COMMENT '关联消息ID（扣费/返还时）',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_sp_id (sp_id),
    INDEX idx_type (type),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客户余额流水表';

-- 创建短信签名表
CREATE TABLE IF NOT EXISTS ink_signature (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    content VARCHAR(30) NOT NULL COMMENT '签名内容（如：【INK科技】）',
    sp_id VARCHAR(30) DEFAULT NULL COMMENT '归属客户标识（空=平台全局）',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待审核, 1-已通过, 2-已驳回',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_sp_id (sp_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信签名表';

-- 创建短信模板表
CREATE TABLE IF NOT EXISTS ink_template (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL COMMENT '模板名称',
    content VARCHAR(500) NOT NULL COMMENT '模板内容',
    signature_id BIGINT DEFAULT NULL COMMENT '关联签名ID',
    status TINYINT DEFAULT 0 COMMENT '状态：0-待审核, 1-已通过, 2-已驳回',
    remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_signature_id (signature_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='短信模板表';

-- 创建敏感词表
CREATE TABLE IF NOT EXISTS ink_sensitive_word (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    word VARCHAR(100) NOT NULL UNIQUE COMMENT '敏感词',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用, 1-启用',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='敏感词表';

-- 创建操作审计日志表
CREATE TABLE IF NOT EXISTS ink_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    admin_id BIGINT DEFAULT NULL COMMENT '管理员ID',
    username VARCHAR(50) DEFAULT NULL COMMENT '管理员用户名',
    module VARCHAR(30) NOT NULL COMMENT '模块：CHANNEL/SP/ADMIN/BLACKLIST/SIGNATURE/TEMPLATE/SENSITIVE',
    action VARCHAR(30) NOT NULL COMMENT '操作：CREATE/UPDATE/DELETE/ENABLE/DISABLE/RECHARGE/APPROVE/REJECT',
    target VARCHAR(100) DEFAULT NULL COMMENT '操作对象标识',
    detail VARCHAR(1000) DEFAULT NULL COMMENT '参数摘要',
    ip VARCHAR(45) DEFAULT NULL COMMENT '操作IP',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    INDEX idx_module (module),
    INDEX idx_action (action),
    INDEX idx_username (username),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='操作审计日志表';
