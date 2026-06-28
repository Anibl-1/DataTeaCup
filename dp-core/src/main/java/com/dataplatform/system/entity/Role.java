package com.dataplatform.system.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Role {
    private Long id;
    private String roleName;
    private String roleCode;
    private String description;
    private Date createTime;
    private Date updateTime;
}
