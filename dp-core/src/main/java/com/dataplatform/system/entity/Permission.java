package com.dataplatform.system.entity;

import lombok.Data;
import java.util.Date;

@Data
public class Permission {
    private Long id;
    private String permissionName;
    private String permissionCode;
    private String description;
    private Date createTime;
}
