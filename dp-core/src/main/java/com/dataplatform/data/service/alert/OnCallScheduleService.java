package com.dataplatform.data.service.alert;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 值班表配置服务
 * 根据值班表确定告警接收人
 * 需求: 15.8
 */
@Slf4j
@Service
public class OnCallScheduleService {

    /** 值班表 key=scheduleId */
    private final Map<String, OnCallSchedule> schedules = new ConcurrentHashMap<>();

    public OnCallScheduleService() {
        OnCallShift shift = new OnCallShift();
        shift.setName("Default");
        shift.setDaysOfWeek(Arrays.asList(DayOfWeek.values()));
        shift.setStartTime(LocalTime.MIN);
        shift.setEndTime(LocalTime.of(23, 59, 59));
        shift.setPersons(List.of("admin"));
        createSchedule("Default On-call", List.of(shift));
    }

    /**
     * 创建值班表
     */
    public String createSchedule(String name, List<OnCallShift> shifts) {
        String id = UUID.randomUUID().toString().substring(0, 8);
        OnCallSchedule schedule = new OnCallSchedule();
        schedule.setId(id);
        schedule.setName(name);
        schedule.setShifts(shifts);
        schedule.setEnabled(true);
        schedules.put(id, schedule);
        log.info("创建值班表: id={}, name={}, shifts={}", id, name, shifts.size());
        return id;
    }

    /**
     * 获取当前值班人员
     */
    public List<String> getCurrentOnCallPersons() {
        List<String> persons = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        DayOfWeek today = now.getDayOfWeek();
        LocalTime currentTime = now.toLocalTime();

        for (OnCallSchedule schedule : schedules.values()) {
            if (!schedule.isEnabled()) continue;
            for (OnCallShift shift : schedule.getShifts()) {
                if (shift.getDaysOfWeek().contains(today) &&
                        !currentTime.isBefore(shift.getStartTime()) &&
                        !currentTime.isAfter(shift.getEndTime())) {
                    persons.addAll(shift.getPersons());
                }
            }
        }
        return persons.isEmpty() ? getDefaultOnCallPersons() : persons;
    }

    /**
     * 获取指定日期的值班人员
     */
    public List<String> getOnCallPersons(LocalDate date, LocalTime time) {
        List<String> persons = new ArrayList<>();
        DayOfWeek dayOfWeek = date.getDayOfWeek();

        for (OnCallSchedule schedule : schedules.values()) {
            if (!schedule.isEnabled()) continue;
            for (OnCallShift shift : schedule.getShifts()) {
                if (shift.getDaysOfWeek().contains(dayOfWeek) &&
                        !time.isBefore(shift.getStartTime()) &&
                        !time.isAfter(shift.getEndTime())) {
                    persons.addAll(shift.getPersons());
                }
            }
        }
        return persons;
    }

    /**
     * 获取所有值班表
     */
    public List<OnCallSchedule> listSchedules() {
        return new ArrayList<>(schedules.values());
    }

    /**
     * 删除值班表
     */
    public boolean deleteSchedule(String scheduleId) {
        return schedules.remove(scheduleId) != null;
    }

    /**
     * 启用/禁用值班表
     */
    public void toggleSchedule(String scheduleId, boolean enabled) {
        OnCallSchedule schedule = schedules.get(scheduleId);
        if (schedule != null) {
            schedule.setEnabled(enabled);
        }
    }

    private List<String> getDefaultOnCallPersons() {
        return List.of("admin");
    }

    @Data
    public static class OnCallSchedule {
        private String id;
        private String name;
        private List<OnCallShift> shifts;
        private boolean enabled;
    }

    @Data
    public static class OnCallShift {
        private String name;              // 班次名称
        private List<DayOfWeek> daysOfWeek; // 值班日
        private LocalTime startTime;      // 开始时间
        private LocalTime endTime;        // 结束时间
        private List<String> persons;     // 值班人员
    }
}
