package com.dataplatform.org.service;

import com.dataplatform.common.exception.BusinessException;
import com.dataplatform.common.exception.ErrorCode;
import com.dataplatform.org.entity.Department;
import com.dataplatform.org.mapper.DepartmentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 部门管理服务
 * 
 * @author dataplatform
 */
@Slf4j
@Service
public class DepartmentService {

    @Autowired
    private DepartmentMapper departmentMapper;

    /**
     * 获取部门列表（平铺）
     */
    public List<Department> getList(String keyword, Integer status) {
        return departmentMapper.selectList(keyword, status);
    }

    /**
     * 获取部门树结构
     */
    public List<Department> getTree(String keyword, Integer status) {
        List<Department> allDepts = departmentMapper.selectList(keyword, status);
        return buildTree(allDepts, 0L);
    }

    /**
     * 根据ID获取部门
     */
    public Department getById(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门ID不能为空");
        }
        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "部门不存在");
        }
        return dept;
    }

    /**
     * 创建部门
     */
    @Transactional
    public Department create(Department dept) {
        if (!StringUtils.hasText(dept.getDeptName())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门名称不能为空");
        }

        // 校验部门编码唯一
        if (StringUtils.hasText(dept.getDeptCode())) {
            Department existing = departmentMapper.selectByDeptCode(dept.getDeptCode());
            if (existing != null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "部门编码已存在");
            }
        }

        // 设置祖级列表
        if (dept.getParentId() == null) {
            dept.setParentId(0L);
        }
        if (dept.getParentId() != 0) {
            Department parent = departmentMapper.selectById(dept.getParentId());
            if (parent == null) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "父部门不存在");
            }
            dept.setAncestors(parent.getAncestors() + "," + parent.getId());
        } else {
            dept.setAncestors("0");
        }

        if (dept.getStatus() == null) {
            dept.setStatus(1);
        }
        if (dept.getSortOrder() == null) {
            dept.setSortOrder(0);
        }

        departmentMapper.insert(dept);
        return dept;
    }

    /**
     * 更新部门
     */
    @Transactional
    public Department update(Department dept) {
        if (dept.getId() == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门ID不能为空");
        }

        Department existing = departmentMapper.selectById(dept.getId());
        if (existing == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "部门不存在");
        }

        // 校验部门编码唯一
        if (StringUtils.hasText(dept.getDeptCode()) && !dept.getDeptCode().equals(existing.getDeptCode())) {
            Department codeExisting = departmentMapper.selectByDeptCode(dept.getDeptCode());
            if (codeExisting != null && !codeExisting.getId().equals(dept.getId())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "部门编码已存在");
            }
        }

        // 不能将父部门设为自己
        if (dept.getParentId() != null && dept.getParentId().equals(dept.getId())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "父部门不能是自己");
        }

        // 父部门变更时更新祖级列表
        if (dept.getParentId() != null && !dept.getParentId().equals(existing.getParentId())) {
            String newAncestors;
            if (dept.getParentId() == 0) {
                newAncestors = "0";
            } else {
                Department parent = departmentMapper.selectById(dept.getParentId());
                if (parent == null) {
                    throw new BusinessException(ErrorCode.PARAM_ERROR, "父部门不存在");
                }
                newAncestors = parent.getAncestors() + "," + parent.getId();
            }
            String oldAncestors = existing.getAncestors() + "," + existing.getId();
            String newChildAncestors = newAncestors + "," + existing.getId();
            departmentMapper.updateChildrenAncestors(oldAncestors, newChildAncestors);
            dept.setAncestors(newAncestors);
        }

        departmentMapper.update(dept);
        return departmentMapper.selectById(dept.getId());
    }

    /**
     * 删除部门
     */
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门ID不能为空");
        }

        Department dept = departmentMapper.selectById(id);
        if (dept == null) {
            throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND, "部门不存在");
        }

        // 检查是否有子部门
        int childCount = departmentMapper.countChildren(id);
        if (childCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "存在子部门，无法删除");
        }

        // 检查是否有用户归属该部门
        int userCount = departmentMapper.countUsersByDeptId(id);
        if (userCount > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门下存在用户，无法删除");
        }

        departmentMapper.deleteById(id);
    }

    /**
     * 更新部门状态
     */
    public void updateStatus(Long id, Integer status) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门ID不能为空");
        }
        departmentMapper.updateStatus(id, status);
    }

    /**
     * 更新部门排序
     */
    public void updateSortOrder(Long id, Integer sortOrder) {
        if (id == null) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "部门ID不能为空");
        }
        departmentMapper.updateSortOrder(id, sortOrder);
    }

    /**
     * 构建部门树
     */
    private List<Department> buildTree(List<Department> depts, Long parentId) {
        List<Department> tree = new ArrayList<>();
        for (Department dept : depts) {
            if (parentId.equals(dept.getParentId())) {
                dept.setChildren(buildTree(depts, dept.getId()));
                tree.add(dept);
            }
        }
        return tree;
    }
}
