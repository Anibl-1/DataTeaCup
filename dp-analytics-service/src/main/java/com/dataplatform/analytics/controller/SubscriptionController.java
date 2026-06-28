package com.dataplatform.analytics.controller;

import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.subscription.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 璁㈤槄涓庢帹閫丄PI
 * 闇€姹? 25.6
 */
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@RequirePermission("report:read")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    public Result<ReportSubscription> subscribe(@RequestBody ReportSubscription sub) {
        return Result.success(subscriptionService.subscribe(sub));
    }

    @DeleteMapping("/{subscriptionId}")
    public Result<Void> unsubscribe(@PathVariable String subscriptionId) {
        subscriptionService.unsubscribe(subscriptionId);
        return Result.success();
    }

    @GetMapping("/user/{userId}")
    public Result<List<ReportSubscription>> getUserSubscriptions(@PathVariable String userId) {
        return Result.success(subscriptionService.getUserSubscriptions(userId));
    }

    @PostMapping("/{subscriptionId}/push")
    public Result<PushLog> triggerPush(@PathVariable String subscriptionId) {
        return Result.success(subscriptionService.executePush(subscriptionId));
    }

    @GetMapping("/{subscriptionId}/logs")
    public Result<List<PushLog>> getPushLogs(
            @PathVariable String subscriptionId,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(subscriptionService.getPushLogs(subscriptionId, limit));
    }
}
