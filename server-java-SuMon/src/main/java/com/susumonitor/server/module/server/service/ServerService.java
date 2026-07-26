package com.susumonitor.server.module.server.service;

import com.susumonitor.server.common.vo.PageResult;
import com.susumonitor.server.module.server.dto.CreateServerRequest;
import com.susumonitor.server.module.server.dto.ServerQueryRequest;
import com.susumonitor.server.module.server.dto.UpdateServerRequest;
import com.susumonitor.server.module.server.vo.ServerStatusVo;
import com.susumonitor.server.module.server.vo.ServerVo;

/**
 * 定义服务器资料与状态快照的业务契约，供接口层依赖。
 */
public interface ServerService {

    /** 创建服务器。 */
    ServerVo create(CreateServerRequest request);

    /** 分页查询服务器。 */
    PageResult<ServerVo> list(ServerQueryRequest request);

    /** 查询服务器详情。 */
    ServerVo get(Long serverId);

    /** 判断服务器是否有效存在。 */
    boolean existsActive(Long serverId);

    /** 更新服务器。 */
    ServerVo update(Long serverId, UpdateServerRequest request);

    /** 软删除服务器。 */
    void delete(Long serverId);

    /** 查询服务器状态快照。 */
    ServerStatusVo status(Long serverId);
}
