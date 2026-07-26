package com.susumonitor.server.module.server.service;

import com.susumonitor.server.module.server.dto.UpdateSshHostKeyRequest;
import com.susumonitor.server.module.server.vo.SshHostKeyVo;
import com.susumonitor.server.module.server.vo.SshTestVo;

/**
 * 定义服务器 SSH 主机身份维护与连接测试的业务契约。
 */
public interface ServerSshService {

    /** 确认或轮换服务器 SSH 主机公钥。 */
    SshHostKeyVo updateHostKey(Long serverId, UpdateSshHostKeyRequest request, Long operatorId);

    /** 使用已确认主机公钥执行 SSH 认证测试。 */
    SshTestVo testConnection(Long serverId);
}
