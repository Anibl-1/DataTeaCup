package com.dataplatform.org.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.org.entity.Post;
import com.dataplatform.org.mapper.PostMapper;
import com.dataplatform.org.mapper.UserPostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 岗位管理服务
 */
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostMapper postMapper;
    private final UserPostMapper userPostMapper;

    /**
     * 查询所有岗位（按 sortOrder 升序）
     */
    public List<Post> list() {
        return postMapper.selectList();
    }

    /**
     * 按名称和状态筛选岗位（按 sortOrder 升序）
     */
    public List<Post> list(String postName, Integer status) {
        return postMapper.selectByCondition(postName, status);
    }

    /**
     * 根据ID查询岗位
     */
    public Post getById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位ID不能为空");
        }
        return postMapper.selectById(id);
    }

    /**
     * 根据编码查询岗位
     */
    public Post getByPostCode(String postCode) {
        if (!StringUtils.hasText(postCode)) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位编码不能为空");
        }
        return postMapper.selectByPostCode(postCode);
    }

    /**
     * 创建岗位（编码唯一性验证）
     */
    @Transactional
    public Post create(Post post) {
        if (post == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位信息不能为空");
        }
        if (!StringUtils.hasText(post.getPostCode())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位编码不能为空");
        }
        if (!StringUtils.hasText(post.getPostName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位名称不能为空");
        }
        // 编码唯一性验证
        Post existing = postMapper.selectByPostCode(post.getPostCode());
        if (existing != null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位编码已存在: " + post.getPostCode());
        }
        if (post.getStatus() == null) {
            post.setStatus(1);
        }
        if (post.getSortOrder() == null) {
            post.setSortOrder(0);
        }
        post.setCreateTime(LocalDateTime.now());
        post.setUpdateTime(LocalDateTime.now());
        postMapper.insert(post);
        return post;
    }

    /**
     * 更新岗位
     */
    @Transactional
    public Post update(Post post) {
        if (post == null || post.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位信息或ID不能为空");
        }
        Post existing = postMapper.selectById(post.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "岗位不存在");
        }
        post.setUpdateTime(LocalDateTime.now());
        postMapper.update(post);
        return postMapper.selectById(post.getId());
    }

    /**
     * 删除岗位（删除前检查用户关联）
     */
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "岗位ID不能为空");
        }
        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "岗位不存在");
        }
        // 检查是否有用户关联该岗位
        int userCount = userPostMapper.countByPostId(id);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该岗位已关联用户，无法删除");
        }
        postMapper.deleteById(id);
    }
}
