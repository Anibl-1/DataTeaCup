package com.dataplatform.system.controller;

import com.dataplatform.common.annotation.OperationLog;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.common.Result;
import com.dataplatform.org.entity.Post;
import com.dataplatform.org.service.PostService;
import com.dataplatform.system.entity.User;
import com.dataplatform.system.service.IUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 岗位管理控制器
 *
 * @author dataplatform
 */
@RestController
@RequestMapping("/posts")
@RequirePermission("post:read")
public class PostController {

    @Autowired
    private PostService postService;

    @Autowired
    private IUserService userService;

    /**
     * 查询岗位列表（支持按名称和状态筛选）
     */
    @GetMapping
    public Result<List<Post>> list(
            @RequestParam(required = false) String postName,
            @RequestParam(required = false) Integer status) {
        List<Post> list = postService.list(postName, status);
        return Result.success(list);
    }

    /**
     * 查询岗位详情
     */
    @GetMapping("/{id}")
    public Result<Post> getById(@PathVariable Long id) {
        Post post = postService.getById(id);
        return Result.success(post);
    }

    /**
     * 创建岗位
     */
    @RequirePermission("post:manage")
    @OperationLog(module = "岗位管理", type = OperationLog.OperationType.CREATE, description = "创建岗位")
    @PostMapping
    public Result<Post> create(@RequestBody Post post) {
        Post created = postService.create(post);
        return Result.success(created);
    }

    /**
     * 更新岗位
     */
    @RequirePermission("post:manage")
    @OperationLog(module = "岗位管理", type = OperationLog.OperationType.UPDATE, description = "更新岗位")
    @PutMapping("/{id}")
    public Result<Post> update(@PathVariable Long id, @RequestBody Post post) {
        post.setId(id);
        Post updated = postService.update(post);
        return Result.success(updated);
    }

    /**
     * 删除岗位（检查关联）
     */
    @RequirePermission("post:manage")
    @OperationLog(module = "岗位管理", type = OperationLog.OperationType.DELETE, description = "删除岗位")
    @DeleteMapping("/{id}")
    public Result<String> delete(@PathVariable Long id) {
        postService.delete(id);
        return Result.success("删除成功");
    }

    /**
     * 查询岗位下的用户列表
     */
    @GetMapping("/{id}/users")
    public Result<List<User>> getUsersByPostId(@PathVariable Long id) {
        List<User> users = userService.getUsersByPostId(id);
        // 隐藏密码
        users.forEach(u -> u.setPassword(null));
        return Result.success(users);
    }
}
