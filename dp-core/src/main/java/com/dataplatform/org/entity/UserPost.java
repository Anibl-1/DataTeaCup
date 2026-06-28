package com.dataplatform.org.entity;

import lombok.Data;

/**
 * 用户岗位关联实体
 */
@Data
public class UserPost {

    /** 用户ID */
    private Long userId;

    /** 岗位ID */
    private Long postId;
}
