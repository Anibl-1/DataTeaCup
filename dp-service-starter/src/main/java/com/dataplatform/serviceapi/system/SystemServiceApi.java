package com.dataplatform.serviceapi.system;

import com.dataplatform.common.Result;
import com.dataplatform.serviceapi.system.dto.UserInfoDTO;
import com.dataplatform.serviceapi.system.dto.RoleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Set;

/**
 * system-service OpenFeign 接口
 *
 * <p>供其他微服务调用 system-service 获取用户/角色/权限信息。</p>
 * <p>设计文档 6.1 节同步调用链路：</p>
 * <ul>
 *   <li>collaboration-service → system-service: 用户昵称/头像/成员信息</li>
 *   <li>data-service(治理子域) → system-service: 用户/角色上下文、权限辅助</li>
 * </ul>
 */
@FeignClient(name = "dp-system-service", contextId = "systemServiceApi", path = "/")
public interface SystemServiceApi {

    // ==================== 用户信息 ====================

    @GetMapping("/user/{id}")
    Result<UserInfoDTO> getUserById(@PathVariable("id") Long id);

    @GetMapping("/user/batch")
    Result<List<UserInfoDTO>> getUsersByIds(@RequestParam("ids") List<Long> ids);

    @GetMapping("/user/current-permissions")
    Result<Set<String>> getUserPermissions(@RequestParam("userId") Long userId);

    // ==================== 角色信息 ====================

    @GetMapping("/role/by-user/{userId}")
    Result<List<RoleDTO>> getRolesByUserId(@PathVariable("userId") Long userId);
}
