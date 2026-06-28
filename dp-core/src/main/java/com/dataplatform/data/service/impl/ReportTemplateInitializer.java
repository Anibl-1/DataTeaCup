package com.dataplatform.data.service.impl;

import com.dataplatform.data.entity.ReportTemplate;
import com.dataplatform.data.mapper.ReportTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 报表模板初始化器
 * 在应用启动时初始化预设报表模板
 * 需求: 11.1, 2.4
 * 
 * @author dataplatform
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReportTemplateInitializer {
    
    private final ReportTemplateMapper reportTemplateMapper;
    
    /**
     * 应用启动后初始化预设模板
     */
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeTemplates() {
        // 检查是否已有系统模板
        List<ReportTemplate> existingTemplates = reportTemplateMapper.findSystemTemplates();
        if (!existingTemplates.isEmpty()) {
            log.info("System templates already exist, skipping initialization. Count: {}", existingTemplates.size());
            return;
        }
        
        log.info("Initializing preset report templates...");
        
        // 创建销售报表模板
        createSalesTemplates();
        
        // 创建财务报表模板
        createFinanceTemplates();
        
        // 创建运营报表模板
        createOperationTemplates();
        
        // 创建库存报表模板
        createInventoryTemplates();
        
        // 创建人员报表模板
        createHrTemplates();
        
        log.info("Preset report templates initialized successfully");
    }
    
    private void createSalesTemplates() {
        // 销售日报表
        createTemplate(
            "销售日报表",
            "sales",
            "按日期统计销售数据，包含销售额、订单数、客单价等核心指标",
            """
                SELECT 
                    DATE(order_date) as sale_date,
                    COUNT(*) as order_count,
                    SUM(amount) as total_amount,
                    AVG(amount) as avg_amount,
                    COUNT(DISTINCT customer_id) as customer_count
                FROM orders 
                WHERE order_date >= '${startDate}' AND order_date <= '${endDate}'
                    AND status = 'completed'
                GROUP BY DATE(order_date)
                ORDER BY sale_date DESC
                """,
            """
                [
                    {"name": "sale_date", "label": "销售日期", "type": "date", "width": 120},
                    {"name": "order_count", "label": "订单数", "type": "number", "width": 100},
                    {"name": "total_amount", "label": "销售总额", "type": "number", "format": "currency", "width": 120},
                    {"name": "avg_amount", "label": "客单价", "type": "number", "format": "currency", "width": 100},
                    {"name": "customer_count", "label": "客户数", "type": "number", "width": 100}
                ]
                """,
            """
                [
                    {"name": "startDate", "label": "开始日期", "type": "date", "required": true, "defaultValue": "today-30"},
                    {"name": "endDate", "label": "结束日期", "type": "date", "required": true, "defaultValue": "today"}
                ]
                """
        );
        
        // 销售区域分析
        createTemplate(
            "销售区域分析",
            "sales",
            "按区域统计销售业绩，支持多维度分析",
            """
                SELECT 
                    region,
                    COUNT(*) as order_count,
                    SUM(amount) as total_amount,
                    SUM(amount) / COUNT(*) as avg_order_amount,
                    COUNT(DISTINCT customer_id) as customer_count
                FROM orders o
                JOIN customers c ON o.customer_id = c.id
                WHERE order_date >= '${startDate}' AND order_date <= '${endDate}'
                GROUP BY region
                ORDER BY total_amount DESC
                """,
            """
                [
                    {"name": "region", "label": "区域", "type": "string", "width": 100},
                    {"name": "order_count", "label": "订单数", "type": "number", "width": 100},
                    {"name": "total_amount", "label": "销售总额", "type": "number", "format": "currency", "width": 120},
                    {"name": "avg_order_amount", "label": "平均订单金额", "type": "number", "format": "currency", "width": 120},
                    {"name": "customer_count", "label": "客户数", "type": "number", "width": 100}
                ]
                """,
            """
                [
                    {"name": "startDate", "label": "开始日期", "type": "date", "required": true},
                    {"name": "endDate", "label": "结束日期", "type": "date", "required": true}
                ]
                """
        );
    }
    
    private void createFinanceTemplates() {
        // 收支明细表
        createTemplate(
            "收支明细表",
            "finance",
            "按月统计收入和支出明细，计算净利润",
            """
                SELECT 
                    DATE_FORMAT(transaction_date, '%Y-%m') as month,
                    SUM(CASE WHEN type = 'income' THEN amount ELSE 0 END) as income,
                    SUM(CASE WHEN type = 'expense' THEN amount ELSE 0 END) as expense,
                    SUM(CASE WHEN type = 'income' THEN amount ELSE -amount END) as net_profit,
                    COUNT(*) as transaction_count
                FROM financial_transactions
                WHERE transaction_date >= '${startDate}' AND transaction_date <= '${endDate}'
                GROUP BY DATE_FORMAT(transaction_date, '%Y-%m')
                ORDER BY month DESC
                """,
            """
                [
                    {"name": "month", "label": "月份", "type": "string", "width": 100},
                    {"name": "income", "label": "收入", "type": "number", "format": "currency", "width": 120},
                    {"name": "expense", "label": "支出", "type": "number", "format": "currency", "width": 120},
                    {"name": "net_profit", "label": "净利润", "type": "number", "format": "currency", "width": 120},
                    {"name": "transaction_count", "label": "交易笔数", "type": "number", "width": 100}
                ]
                """,
            """
                [
                    {"name": "startDate", "label": "开始日期", "type": "date", "required": true},
                    {"name": "endDate", "label": "结束日期", "type": "date", "required": true}
                ]
                """
        );
    }
    
    private void createOperationTemplates() {
        // 用户活跃度分析
        createTemplate(
            "用户活跃度分析",
            "operation",
            "分析用户活跃情况，包含DAU等指标",
            """
                SELECT 
                    DATE(login_time) as date,
                    COUNT(DISTINCT user_id) as dau,
                    COUNT(*) as login_count,
                    AVG(session_duration) as avg_session_duration
                FROM user_activities
                WHERE login_time >= '${startDate}' AND login_time <= '${endDate}'
                GROUP BY DATE(login_time)
                ORDER BY date DESC
                """,
            """
                [
                    {"name": "date", "label": "日期", "type": "date", "width": 120},
                    {"name": "dau", "label": "日活用户", "type": "number", "width": 100},
                    {"name": "login_count", "label": "登录次数", "type": "number", "width": 100},
                    {"name": "avg_session_duration", "label": "平均会话时长(秒)", "type": "number", "width": 140}
                ]
                """,
            """
                [
                    {"name": "startDate", "label": "开始日期", "type": "date", "required": true},
                    {"name": "endDate", "label": "结束日期", "type": "date", "required": true}
                ]
                """
        );
    }
    
    private void createInventoryTemplates() {
        // 库存盘点表
        createTemplate(
            "库存盘点表",
            "inventory",
            "当前库存状态汇总，包含库存预警",
            """
                SELECT 
                    p.product_code,
                    p.product_name,
                    p.category,
                    i.quantity as current_stock,
                    i.safety_stock,
                    CASE 
                        WHEN i.quantity <= 0 THEN '缺货'
                        WHEN i.quantity < i.safety_stock * 0.5 THEN '严重不足'
                        WHEN i.quantity < i.safety_stock THEN '库存预警'
                        ELSE '正常'
                    END as stock_status,
                    i.unit_cost,
                    i.quantity * i.unit_cost as stock_value
                FROM inventory i
                JOIN products p ON i.product_id = p.id
                ORDER BY stock_status, p.product_name
                """,
            """
                [
                    {"name": "product_code", "label": "商品编码", "type": "string", "width": 120},
                    {"name": "product_name", "label": "商品名称", "type": "string", "width": 150},
                    {"name": "category", "label": "分类", "type": "string", "width": 100},
                    {"name": "current_stock", "label": "当前库存", "type": "number", "width": 100},
                    {"name": "safety_stock", "label": "安全库存", "type": "number", "width": 100},
                    {"name": "stock_status", "label": "库存状态", "type": "string", "width": 100},
                    {"name": "unit_cost", "label": "单位成本", "type": "number", "format": "currency", "width": 100},
                    {"name": "stock_value", "label": "库存价值", "type": "number", "format": "currency", "width": 120}
                ]
                """,
            "[]"
        );
    }
    
    private void createHrTemplates() {
        // 员工花名册
        createTemplate(
            "员工花名册",
            "hr",
            "员工基本信息汇总",
            """
                SELECT 
                    e.employee_no,
                    e.name,
                    d.department_name,
                    e.position,
                    e.entry_date,
                    TIMESTAMPDIFF(MONTH, e.entry_date, NOW()) as tenure_months,
                    e.status
                FROM employees e
                JOIN departments d ON e.department_id = d.id
                ORDER BY d.department_name, e.employee_no
                """,
            """
                [
                    {"name": "employee_no", "label": "工号", "type": "string", "width": 100},
                    {"name": "name", "label": "姓名", "type": "string", "width": 100},
                    {"name": "department_name", "label": "部门", "type": "string", "width": 120},
                    {"name": "position", "label": "职位", "type": "string", "width": 120},
                    {"name": "entry_date", "label": "入职日期", "type": "date", "width": 120},
                    {"name": "tenure_months", "label": "在职月数", "type": "number", "width": 100},
                    {"name": "status", "label": "状态", "type": "string", "width": 80}
                ]
                """,
            "[]"
        );
    }
    
    private void createTemplate(String name, String category, String description,
                                 String sqlTemplate, String fieldsConfig, String paramsConfig) {
        ReportTemplate template = new ReportTemplate();
        template.setName(name);
        template.setCategory(category);
        template.setDescription(description);
        template.setSqlTemplate(sqlTemplate.trim());
        template.setFieldsConfig(fieldsConfig.trim());
        template.setParamsConfig(paramsConfig.trim());
        template.setIsSystem(true);
        template.setStatus(1);
        template.setUseCount(0);
        template.setCreateTime(LocalDateTime.now());
        template.setUpdateTime(LocalDateTime.now());
        
        reportTemplateMapper.insert(template);
        log.debug("Created preset template: {}", name);
    }
}
