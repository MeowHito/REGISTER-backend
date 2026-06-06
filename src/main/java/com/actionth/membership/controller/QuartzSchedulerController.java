package com.actionth.membership.controller;

import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.actionth.membership.model.dto.JobScheduleDto;
import com.actionth.membership.model.dto.JobScheduleUpdateDto;
import com.actionth.membership.model.request.UpdateJobScheduleRequest;
import com.actionth.membership.service.QuartzSchedulerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controller for managing Quartz Scheduler configurations
 */
@Slf4j
@RestController
@RequestMapping("/api/scheduler")
@Tag(name = "Scheduler Management", description = "APIs for managing Quartz job schedules and configurations")
public class QuartzSchedulerController {

    @Autowired
    private QuartzSchedulerService quartzSchedulerService;

    /**
     * Update job schedule
     * POST /api/scheduler/update-schedule
     * 
     * Request body example:
     * {
     *   "jobName": "updateOverduePaymentsJob",
     *   "cronExpression": "0 0 2 * * ?",
     *   "timezone": "Asia/Bangkok"
     * }
     */
    @Operation(
        summary = "อัปเดตตารางเวลาของ Job",
        description = "อัปเดต cron expression และ timezone ของ Quartz job ที่กำลังทำงานอยู่",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "อัปเดตตารางเวลาสำเร็จ",
                content = @Content(schema = @Schema(implementation = JobScheduleUpdateDto.class))
            ),
            @ApiResponse(
                responseCode = "400", 
                description = "ข้อมูลไม่ถูกต้องหรือไม่พบ Job ที่ระบุ",
                content = @Content(schema = @Schema(implementation = JobScheduleUpdateDto.class))
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "เกิดข้อผิดพลาดภายในระบบ",
                content = @Content(schema = @Schema(implementation = JobScheduleUpdateDto.class))
            )
        }
    )
    @PostMapping("/update-schedule")
    public ResponseEntity<JobScheduleUpdateDto> updateSchedule(@Valid @RequestBody UpdateJobScheduleRequest request) {
        try {
            JobScheduleUpdateDto result = quartzSchedulerService.updateJobSchedule(
                    request.getJobName(),
                    request.getJobGroup(),
                    request.getCronExpression(),
                    request.getTimezone()
            );
            
            if (result.isSuccess()) {
                return ResponseEntity.ok(result);
            } else {
                return ResponseEntity.badRequest().body(result);
            }
            
        } catch (SchedulerException e) {
            log.error("Failed to update schedule", e);
            JobScheduleUpdateDto errorResponse = JobScheduleUpdateDto.builder()
                    .success(false)
                    .message("เกิดข้อผิดพลาดในการอัปเดตตารางเวลา: " + e.getMessage())
                    .build();
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }

    /**
     * Get current schedule for a job
     * GET /api/scheduler/schedule/{jobName}
     */
    @Operation(
        summary = "ดึงข้อมูลตารางเวลาปัจจุบันของ Job",
        description = "ดึงข้อมูล cron expression, timezone และเวลาที่จะรันครั้งถัดไปของ Job ที่ระบุ",
        responses = {
            @ApiResponse(
                responseCode = "200", 
                description = "ดึงข้อมูลสำเร็จ",
                content = @Content(schema = @Schema(implementation = JobScheduleDto.class))
            ),
            @ApiResponse(
                responseCode = "404", 
                description = "ไม่พบ Job ที่ระบุ"
            ),
            @ApiResponse(
                responseCode = "500", 
                description = "เกิดข้อผิดพลาดภายในระบบ"
            )
        }
    )
    @GetMapping("/schedule/{jobName}")
    public ResponseEntity<JobScheduleDto> getSchedule(
            @Parameter(description = "ชื่อของ Job (เช่น updateOverduePaymentsJob)", required = true)
            @PathVariable String jobName,
            @Parameter(description = "Group ของ Job (ถ้าไม่ระบุจะใช้ default group)")
            @RequestParam(required = false) String jobGroup) {
        try {
            JobScheduleDto schedule = quartzSchedulerService.getCurrentSchedule(jobName, jobGroup);
            
            if (schedule != null) {
                return ResponseEntity.ok(schedule);
            } else {
                return ResponseEntity.notFound().build();
            }
            
        } catch (SchedulerException e) {
            log.error("Failed to get schedule", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
