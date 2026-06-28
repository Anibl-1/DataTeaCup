package com.dataplatform.system.controller;

import com.dataplatform.common.PageResult;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.util.LogUtil;
import com.dataplatform.data.dto.UserCreateDTO;
import com.dataplatform.data.dto.UserUpdateDTO;
import com.dataplatform.system.entity.User;
import com.dataplatform.system.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@Tag(name = "用户管理", description = "用户CRUD及角色分配接口")
@RestController
@RequestMapping("/user")
@RequirePermission("user:read")
public class UserController {
    
    private static final String MODULE = LogUtil.USER;
    
    @Autowired
    private IUserService userService;

    @Operation(summary = "获取用户列表", description = "分页获取用户列表，支持筛选")
    @ApiResponse(responseCode = "200", description = "成功")
    @RequirePermission("user:read")
    @GetMapping("/list")
    public Result<PageResult<User>> getUserList(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String filters) {
        log.debug("[{}] 查询用户列表 | page={}, pageSize={}", MODULE, page, pageSize);
        List<User> list = userService.getUserList(page, pageSize, filters);
        long total = userService.getUserCount(filters);
        log.debug("[{}] 查询用户列表完成 | total={}", MODULE, total);
        return Result.success(new PageResult<>(list, total));
    }

    @Operation(summary = "创建用户", description = "创建新用户")
    @ApiResponse(responseCode = "200", description = "创建成功")
    @RequirePermission("user:manage")
    @OperationLog(module = "用户管理", type = OperationLog.OperationType.CREATE, description = "创建用户")
    @PostMapping("/create")
    public Result<Void> createUser(@Validated @RequestBody UserCreateDTO userDTO) {
        log.info("[{}] 创建用户 | username={}", MODULE, userDTO.getUsername());
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        userService.createUser(user);
        log.info("[{}] 创建用户成功 | username={}", MODULE, userDTO.getUsername());
        return Result.success();
    }

    @Operation(summary = "更新用户", description = "更新用户信息")
    @ApiResponse(responseCode = "200", description = "更新成功")
    @RequirePermission("user:manage")
    @OperationLog(module = "用户管理", type = OperationLog.OperationType.UPDATE, description = "更新用户")
    @PutMapping("/update")
    public Result<Void> updateUser(@Validated @RequestBody UserUpdateDTO userDTO) {
        log.info("[{}] 更新用户 | id={}", MODULE, userDTO.getId());
        User user = new User();
        BeanUtils.copyProperties(userDTO, user);
        userService.updateUser(user);
        log.info("[{}] 更新用户成功 | id={}", MODULE, userDTO.getId());
        return Result.success();
    }

    @Operation(summary = "删除用户", description = "根据ID删除用户")
    @ApiResponse(responseCode = "200", description = "删除成功")
    @RequirePermission("user:manage")
    @OperationLog(module = "用户管理", type = OperationLog.OperationType.DELETE, description = "删除用户")
    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteUser(@Parameter(description = "用户ID") @PathVariable Long id) {
        log.info("[{}] 删除用户 | id={}", MODULE, id);
        userService.deleteUser(id);
        log.info("[{}] 删除用户成功 | id={}", MODULE, id);
        return Result.success();
    }
    
    @RequirePermission("user:manage")
    @OperationLog(module = "用户管理", type = OperationLog.OperationType.UPDATE, description = "分配用户角色")
    @PostMapping("/{userId}/assignRoles")
    public Result<Void> assignRoles(@PathVariable Long userId, @RequestBody List<Long> roleIds) {
        log.info("[{}] 分配角色 | userId={}, roleCount={}", MODULE, userId, roleIds != null ? roleIds.size() : 0);
        userService.assignRoles(userId, roleIds);
        log.info("[{}] 分配角色成功 | userId={}", MODULE, userId);
        return Result.success();
    }
    
    @RequirePermission("user:read")
    @GetMapping("/{userId}/roles")
    public Result<List<com.dataplatform.system.entity.Role>> getUserRoles(@PathVariable Long userId) {
        log.debug("[{}] 查询用户角色 | userId={}", MODULE, userId);
        List<com.dataplatform.system.entity.Role> roles = userService.getUserRoles(userId);
        return Result.success(roles);
    }
    
    @RequirePermission("user:manage")
    @OperationLog(module = "用户管理", type = OperationLog.OperationType.UPDATE, description = "重置用户密码", saveParams = false)
    @PostMapping("/resetPassword")
    public Result<Void> resetPassword(@Validated @RequestBody com.dataplatform.data.dto.ResetPasswordDTO dto) {
        log.info("[{}] 重置密码 | userId={}", MODULE, dto.getUserId());
        userService.resetPassword(dto.getUserId());
        log.info("[{}] 重置密码成功 | userId={}", MODULE, dto.getUserId());
        return Result.success();
    }

    @Operation(summary = "批量删除用户", description = "根据ID列表批量删除用户")
    @RequirePermission("user:manage")
    @OperationLog(module = "用户管理", type = OperationLog.OperationType.DELETE, description = "批量删除用户")
    @PostMapping("/batch-delete")
    public Result<Map<String, Object>> batchDelete(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            log.warn("[{}] 批量删除失败 | 参数为空", MODULE);
            return Result.error(400, "用户ID列表不能为空");
        }
        log.info("[{}] 批量删除用户 | count={}", MODULE, ids.size());
        int success = 0;
        int failed = 0;
        List<String> errors = new java.util.ArrayList<>();
        for (Long id : ids) {
            try {
                userService.deleteUser(id);
                success++;
            } catch (Exception e) {
                failed++;
                errors.add("ID " + id + ": " + e.getMessage());
                log.warn("[{}] 批量删除单条失败 | id={}, error={}", MODULE, id, e.getMessage());
            }
        }
        log.info("[{}] 批量删除完成 | success={}, failed={}", MODULE, success, failed);
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("success", success);
        result.put("failed", failed);
        if (!errors.isEmpty() && errors.size() <= 5) {
            result.put("errors", errors);
        }
        return Result.success(result);
    }

    @Operation(summary = "批量更新用户状态", description = "批量启用或禁用用户")
    @RequirePermission("user:manage")
    @OperationLog(module = "用户管理", type = OperationLog.OperationType.UPDATE, description = "批量更新用户状态")
    @PostMapping("/batch-status")
    public Result<Void> batchUpdateStatus(@RequestBody Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        List<Number> ids = (List<Number>) body.get("ids");
        Number statusNum = (Number) body.get("status");
        if (ids == null || ids.isEmpty()) {
            log.warn("[{}] 批量更新状态失败 | ID列表为空", MODULE);
            return Result.error(400, "用户ID列表不能为空");
        }
        if (statusNum == null) {
            log.warn("[{}] 批量更新状态失败 | 状态值为空", MODULE);
            return Result.error(400, "状态值不能为空");
        }
        int status = statusNum.intValue();
        if (status != 0 && status != 1) {
            log.warn("[{}] 批量更新状态失败 | 状态值无效: {}", MODULE, status);
            return Result.error(400, "状态值无效，仅支持0(禁用)或1(启用)");
        }
        log.info("[{}] 批量更新状态 | count={}, status={}", MODULE, ids.size(), status == 1 ? "启用" : "禁用");
        for (Number id : ids) {
            User user = new User();
            user.setId(id.longValue());
            user.setStatus(status);
            userService.updateUser(user);
        }
        log.info("[{}] 批量更新状态完成 | count={}", MODULE, ids.size());
        return Result.success();
    }

    @Operation(summary = "导出用户列表", description = "导出全部用户为JSON")
    @RequirePermission("user:export")
    @GetMapping("/export")
    public Result<List<User>> exportUsers(@RequestParam(required = false) String filters) {
        log.info("[{}] 导出用户列表", MODULE);
        List<User> list = userService.getUserList(1, 10000, filters);
        list.forEach(u -> u.setPassword(null));
        log.info("[{}] 导出用户列表完成 | count={}", MODULE, list.size());
        return Result.success(list);
    }
}
