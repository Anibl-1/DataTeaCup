package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.entity.ReportSchedule;
import com.dataplatform.data.service.ReportScheduleService;
import cn.dev33.satoken.stp.StpUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 报表定时推送Controller
 */
@Slf4j
@RestController
@RequestMapping("/report/schedule")
@RequirePermission("report:manage")
public class ReportScheduleController {

    private final ReportScheduleService scheduleService;

    public ReportScheduleController(ReportScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @GetMapping("/list")
    public Result<List<ReportSchedule>> list() {
        return Result.success(scheduleService.findAll());
    }

    @GetMapping("/{id}")
    public Result<ReportSchedule> getById(@PathVariable Long id) {
        ReportSchedule schedule = scheduleService.findById(id);
        if (schedule == null) {
            return Result.error(404, "推送任务不存在");
        }
        return Result.success(schedule);
    }

    @GetMapping("/by-report/{reportId}")
    public Result<List<ReportSchedule>> getByReportId(@PathVariable Long reportId) {
        return Result.success(scheduleService.findByReportId(reportId));
    }

    @PostMapping
    public Result<ReportSchedule> create(@RequestBody ReportSchedule schedule) {
        if (schedule.getReportId() == null) {
            return Result.error(400, "报表ID不能为空");
        }
        if (schedule.getCronExpression() == null || schedule.getCronExpression().isBlank()) {
            return Result.error(400, "Cron表达式不能为空");
        }
        if (schedule.getRecipients() == null || schedule.getRecipients().isBlank()) {
            return Result.error(400, "收件人不能为空");
        }
        schedule.setCreateBy(Long.parseLong(StpUtil.getLoginId().toString()));
        ReportSchedule created = scheduleService.create(schedule);
        return Result.success(created);
    }

    @PutMapping("/{id}")
    public Result<ReportSchedule> update(@PathVariable Long id, @RequestBody ReportSchedule schedule) {
        schedule.setId(id);
        ReportSchedule updated = scheduleService.update(schedule);
        return Result.success(updated);
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        scheduleService.delete(id);
        return Result.success(null);
    }

    @PostMapping("/{id}/trigger")
    public Result<Void> triggerManually(@PathVariable Long id) {
        try {
            scheduleService.triggerManually(id);
            return Result.success(null);
        } catch (Exception e) {
            return Result.error(500, "手动执行失败: " + e.getMessage());
        }
    }
}
