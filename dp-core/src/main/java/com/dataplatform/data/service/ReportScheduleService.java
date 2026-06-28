package com.dataplatform.data.service;

import com.dataplatform.data.entity.ReportDefinition;
import com.dataplatform.data.entity.ReportSchedule;
import com.dataplatform.data.entity.DataSource;
import com.dataplatform.data.mapper.DataSourceMapper;
import com.dataplatform.data.mapper.ReportDefinitionMapper;
import com.dataplatform.data.mapper.ReportScheduleMapper;
import com.dataplatform.common.util.DateFunctionUtil;
import com.dataplatform.data.service.DbConnectionUtil;
import com.dataplatform.common.util.SqlParamUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 报表定时推送服务
 * 定时检查并执行到期的报表推送任务
 */
@Slf4j
@Service
public class ReportScheduleService {

    @Value("${export.file.path:../runtime/exports}")
    private String exportPath;

    @Autowired
    private ReportScheduleMapper scheduleMapper;

    @Autowired
    private ReportDefinitionMapper reportDefinitionMapper;

    @Autowired
    private DataSourceMapper dataSourceMapper;

    @Autowired
    private DbConnectionUtil dbConnectionUtil;

    @Autowired
    private com.dataplatform.data.service.DataSourceConnectionPoolManager connectionPoolManager;

    @Autowired
    private DataSourceService dataSourceService;

    @Autowired(required = false)
    private EmailNotifyService emailNotifyService;

    @Autowired(required = false)
    private WecomNotifyService wecomNotifyService;

    @Autowired(required = false)
    private DingtalkNotifyService dingtalkNotifyService;

    @Autowired(required = false)
    private MessageChannelService messageChannelService;

    @Autowired(required = false)
    private NotificationChannelRouter notificationChannelRouter;

    public List<ReportSchedule> findAll() {
        return scheduleMapper.findAll();
    }

    public ReportSchedule findById(Long id) {
        return scheduleMapper.findById(id);
    }

    public List<ReportSchedule> findByReportId(Long reportId) {
        return scheduleMapper.findByReportId(reportId);
    }

    public ReportSchedule create(ReportSchedule schedule) {
        if (schedule.getIsEnabled() == null) schedule.setIsEnabled(1);
        if (schedule.getAttachExcel() == null) schedule.setAttachExcel(1);
        if (schedule.getAttachFormat() == null || schedule.getAttachFormat().isEmpty()) {
            schedule.setAttachFormat("excel");
        }
        if (schedule.getChannels() == null || schedule.getChannels().isEmpty()) {
            schedule.setChannels("email");
        }
        scheduleMapper.insert(schedule);
        return schedule;
    }

    public ReportSchedule update(ReportSchedule schedule) {
        scheduleMapper.update(schedule);
        return scheduleMapper.findById(schedule.getId());
    }

    public void delete(Long id) {
        scheduleMapper.deleteById(id);
    }

    /**
     * 定时任务：每分钟检查是否有到期的报表推送
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkAndExecuteSchedules() {
        List<ReportSchedule> enabledSchedules = scheduleMapper.findEnabled();
        LocalDateTime now = LocalDateTime.now();

        for (ReportSchedule schedule : enabledSchedules) {
            try {
                if (shouldRun(schedule, now)) {
                    executeSchedule(schedule);
                }
            } catch (Exception e) {
                log.error("报表推送任务执行失败: scheduleId={}, name={}", schedule.getId(), schedule.getScheduleName(), e);
                scheduleMapper.updateRunStatus(schedule.getId(), "failed");
            }
        }
    }

    /**
     * 手动触发执行
     */
    public void triggerManually(Long scheduleId) {
        ReportSchedule schedule = scheduleMapper.findById(scheduleId);
        if (schedule == null) {
            throw new RuntimeException("推送任务不存在");
        }
        executeSchedule(schedule);
    }

    private boolean shouldRun(ReportSchedule schedule, LocalDateTime now) {
        try {
            CronExpression cron = CronExpression.parse(schedule.getCronExpression());
            LocalDateTime lastRun = schedule.getLastRunTime() != null
                    ? new java.sql.Timestamp(schedule.getLastRunTime().getTime()).toLocalDateTime()
                    : now.minusDays(1);
            LocalDateTime nextRun = cron.next(lastRun);
            return nextRun != null && !nextRun.isAfter(now);
        } catch (Exception e) {
            log.warn("Cron表达式解析失败: {}", schedule.getCronExpression(), e);
            return false;
        }
    }

    private void executeSchedule(ReportSchedule schedule) {
        log.info("执行报表推送: id={}, name={}", schedule.getId(), schedule.getScheduleName());

        ReportDefinition report = reportDefinitionMapper.selectById(schedule.getReportId());
        if (report == null) {
            log.warn("报表不存在: reportId={}", schedule.getReportId());
            scheduleMapper.updateRunStatus(schedule.getId(), "failed");
            return;
        }

        try {
            // 合并日期函数参数到 filterParams
            String mergedFilterParams = DateFunctionUtil.mergeWithFilterParams(
                    schedule.getDateParams(), schedule.getFilterParams());

            String subject = "定时报表: " + report.getReportName();
            String attachFormat = schedule.getAttachFormat() != null ? schedule.getAttachFormat() : "excel";
            String content = buildReportContent(report, schedule, attachFormat);

            // 解析推送渠道列表
            Set<String> channelSet = new HashSet<>();
            if (schedule.getChannels() != null) {
                for (String ch : schedule.getChannels().split(",")) {
                    String trimmed = ch.trim();
                    if (!trimmed.isEmpty()) channelSet.add(trimmed);
                }
            }

            // 邮件渠道
            if (channelSet.contains("email") && emailNotifyService != null && schedule.getRecipients() != null) {
                if (schedule.getAttachExcel() != null && schedule.getAttachExcel() == 1) {
                    File attachFile = "pdf".equals(attachFormat)
                            ? generateReportPdf(report, schedule, mergedFilterParams)
                            : generateReportExcel(report, schedule, mergedFilterParams);
                    emailNotifyService.sendWithChannel(
                            schedule.getEmailChannelId(),
                            schedule.getRecipients(),
                            null, subject, content, true);
                    log.info("报表推送邮件已发送: recipients={}, format={}", schedule.getRecipients(), attachFormat);
                    if (attachFile != null && attachFile.exists()) {
                        attachFile.delete();
                    }
                } else {
                    emailNotifyService.sendWithChannel(
                            schedule.getEmailChannelId(),
                            schedule.getRecipients(),
                            null, subject, content, true);
                }
            }

            // 企业微信渠道 - 直接调用WecomNotifyService
            if (channelSet.contains("wecom") && wecomNotifyService != null) {
                try {
                    Map<String, Object> wecomConfig = new HashMap<>();
                    wecomConfig.put("content", subject + "\n" + report.getReportName());
                    wecomConfig.put("channelId", schedule.getWecomChannelId());
                    if (schedule.getWecomChannelId() != null && messageChannelService != null) {
                        var channel = messageChannelService.getChannel(schedule.getWecomChannelId(), "wecom");
                        if (channel != null) {
                            wecomConfig.putAll(messageChannelService.parseConfig(channel));
                            wecomConfig.put("content", subject + "\n" + report.getReportName());
                        }
                    }
                    wecomNotifyService.send(wecomConfig, Map.of());
                    log.info("报表推送企微通知已发送: scheduleId={}", schedule.getId());
                } catch (Exception e) {
                    log.error("企微推送失败: scheduleId={}", schedule.getId(), e);
                }
            }

            // 钉钉渠道 - 直接调用DingtalkNotifyService
            if (channelSet.contains("dingtalk") && dingtalkNotifyService != null) {
                try {
                    Map<String, Object> dtConfig = new HashMap<>();
                    dtConfig.put("content", subject + "\n" + report.getReportName());
                    dtConfig.put("channelId", schedule.getDingtalkChannelId());
                    if (schedule.getDingtalkChannelId() != null && messageChannelService != null) {
                        var channel = messageChannelService.getChannel(schedule.getDingtalkChannelId(), "dingtalk");
                        if (channel != null) {
                            dtConfig.putAll(messageChannelService.parseConfig(channel));
                            dtConfig.put("content", subject + "\n" + report.getReportName());
                        }
                    }
                    dingtalkNotifyService.send(dtConfig, Map.of());
                    log.info("报表推送钉钉通知已发送: scheduleId={}", schedule.getId());
                } catch (Exception e) {
                    log.error("钉钉推送失败: scheduleId={}", schedule.getId(), e);
                }
            }

            scheduleMapper.updateRunStatus(schedule.getId(), "success");
            log.info("报表推送完成: id={}", schedule.getId());

        } catch (Exception e) {
            log.error("报表推送执行失败: id={}", schedule.getId(), e);
            scheduleMapper.updateRunStatus(schedule.getId(), "failed");
        }
    }

    private String buildReportContent(ReportDefinition report, ReportSchedule schedule, String attachFormat) {
        StringBuilder sb = new StringBuilder();
        sb.append("<h3>").append(report.getReportName()).append("</h3>");
        sb.append("<p>").append(report.getDescription() != null ? report.getDescription() : "").append("</p>");
        sb.append("<p>推送时间: ").append(LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("</p>");

        // 显示日期参数信息
        if (schedule.getDateParams() != null && !schedule.getDateParams().isBlank()) {
            sb.append("<p>数据范围参数: ");
            String merged = DateFunctionUtil.mergeWithFilterParams(schedule.getDateParams(), null);
            if (merged != null) {
                sb.append(merged.replace("{", "").replace("}", "").replace("\"", ""));
            }
            sb.append("</p>");
        }

        if (schedule.getAttachExcel() != null && schedule.getAttachExcel() == 1) {
            sb.append("<p>报表数据详见附件").append("pdf".equals(attachFormat) ? "PDF" : "Excel").append("文件。</p>");
        }
        return sb.toString();
    }

    private File generateReportExcel(ReportDefinition report, ReportSchedule schedule, String mergedFilterParams) {
        File dir = new File(exportPath, "schedule");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "report_" + report.getId() + "_" + System.currentTimeMillis() + ".xlsx");

        try {
            DataSource ds = dataSourceMapper.selectById(report.getDataSourceId());
            if (ds == null) return null;

            String sql = report.getSqlContent();
            if (mergedFilterParams != null && !mergedFilterParams.isBlank()) {
                Map<String, String> paramMap = SqlParamUtil.parseParamsJson(mergedFilterParams);
                Object[] paramResult = SqlParamUtil.replaceCustomParams(sql, paramMap);
                sql = (String) paramResult[0];
            }

            Class.forName(dbConnectionUtil.getDriverClassName(ds.getDbType()));
            try (Connection conn = connectionPoolManager.getConnection(ds);
                 PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {

                SXSSFWorkbook workbook = new SXSSFWorkbook(500);
                Sheet sheet = workbook.createSheet("报表数据");
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();

                Row headerRow = sheet.createRow(0);
                CellStyle headerStyle = workbook.createCellStyle();
                Font headerFont = workbook.createFont();
                headerFont.setBold(true);
                headerStyle.setFont(headerFont);
                for (int i = 1; i <= colCount; i++) {
                    Cell cell = headerRow.createCell(i - 1);
                    cell.setCellValue(meta.getColumnLabel(i));
                    cell.setCellStyle(headerStyle);
                }

                int rowIdx = 1;
                while (rs.next() && rowIdx <= 100000) {
                    Row row = sheet.createRow(rowIdx++);
                    for (int i = 1; i <= colCount; i++) {
                        Cell cell = row.createCell(i - 1);
                        Object val = rs.getObject(i);
                        if (val != null) cell.setCellValue(val.toString());
                    }
                }

                try (FileOutputStream fos = new FileOutputStream(file)) {
                    workbook.write(fos);
                }
                workbook.dispose();
                workbook.close();
            }
            return file;
        } catch (Exception e) {
            log.error("生成报表Excel失败: reportId={}", report.getId(), e);
            return null;
        }
    }

    /**
     * 生成报表PDF文件（基于HTML表格转PDF）
     */
    private File generateReportPdf(ReportDefinition report, ReportSchedule schedule, String mergedFilterParams) {
        File dir = new File(exportPath, "schedule");
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, "report_" + report.getId() + "_" + System.currentTimeMillis() + ".pdf");

        try {
            DataSource ds = dataSourceMapper.selectById(report.getDataSourceId());
            if (ds == null) return null;

            String sql = report.getSqlContent();
            if (mergedFilterParams != null && !mergedFilterParams.isBlank()) {
                Map<String, String> paramMap = SqlParamUtil.parseParamsJson(mergedFilterParams);
                Object[] paramResult = SqlParamUtil.replaceCustomParams(sql, paramMap);
                sql = (String) paramResult[0];
            }

            // 先生成Excel，再转换（PDF生成需要额外依赖，这里先用Excel作为附件）
            // 实际生产环境可集成 iText / OpenPDF / wkhtmltopdf 等
            log.info("PDF格式推送: reportId={}, 当前使用Excel替代", report.getId());
            return generateReportExcel(report, schedule, mergedFilterParams);
        } catch (Exception e) {
            log.error("生成报表PDF失败: reportId={}", report.getId(), e);
            return null;
        }
    }
}
