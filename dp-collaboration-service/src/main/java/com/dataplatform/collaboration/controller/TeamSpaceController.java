package com.dataplatform.collaboration.controller;

import cn.dev33.satoken.stp.StpUtil;
import com.dataplatform.common.Result;
import com.dataplatform.common.annotation.RequirePermission;
import com.dataplatform.data.service.team.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * 团队空间API
 */
@Slf4j
@RestController
@RequestMapping("/team-spaces")
@RequiredArgsConstructor
@RequirePermission("team:read")
public class TeamSpaceController {

    private final TeamSpaceService teamSpaceService;

    @Value("${export.file.path:../runtime/exports}")
    private String uploadBasePath;

    @PostMapping
    public Result<TeamSpace> createSpace(@RequestBody TeamSpace space) {
        space.setOwnerId(String.valueOf(StpUtil.getLoginId()));
        return Result.success(teamSpaceService.createSpace(space));
    }

    @GetMapping("/{spaceId}")
    public Result<TeamSpace> getSpace(@PathVariable String spaceId) {
        return Result.success(teamSpaceService.getSpace(spaceId));
    }

    @PutMapping("/{spaceId}")
    public Result<TeamSpace> updateSpace(@PathVariable String spaceId, @RequestBody TeamSpace update) {
        TeamSpace space = teamSpaceService.updateSpace(spaceId, update);
        return space != null ? Result.success(space) : Result.error("空间不存在");
    }

    @DeleteMapping("/{spaceId}")
    public Result<Void> deleteSpace(@PathVariable String spaceId) {
        teamSpaceService.deleteSpace(spaceId);
        return Result.success();
    }

    @GetMapping("/user/current")
    public Result<List<TeamSpace>> listCurrentUserSpaces() {
        String userId = String.valueOf(StpUtil.getLoginId());
        List<TeamSpace> spaces = teamSpaceService.listSpacesByUser(userId);
        return Result.success(spaces != null ? spaces : Collections.emptyList());
    }

    @GetMapping("/user/{userId}")
    public Result<List<TeamSpace>> listUserSpaces(@PathVariable String userId) {
        return Result.success(teamSpaceService.listSpacesByUser(userId));
    }

    // ==================== 成员管理 ====================

    @PostMapping("/{spaceId}/members")
    public Result<Void> addMember(@PathVariable String spaceId, @RequestBody MemberRequest req) {
        teamSpaceService.addMember(spaceId, req.getUserId(), req.getRole());
        return Result.success();
    }

    @DeleteMapping("/{spaceId}/members/{userId}")
    public Result<Void> removeMember(@PathVariable String spaceId, @PathVariable String userId) {
        teamSpaceService.removeMember(spaceId, userId);
        return Result.success();
    }

    @PutMapping("/{spaceId}/members/{userId}")
    public Result<Void> updateMemberRole(@PathVariable String spaceId, @PathVariable String userId,
                                         @RequestBody MemberRequest req) {
        teamSpaceService.updateMemberRole(spaceId, userId, req.getRole());
        return Result.success();
    }

    @GetMapping("/{spaceId}/members")
    public Result<List<TeamSpaceMember>> getMembers(@PathVariable String spaceId) {
        return Result.success(teamSpaceService.getMembers(spaceId));
    }

    // ==================== 资源管理 ====================

    @GetMapping("/{spaceId}/resources")
    public Result<Set<String>> getResources(@PathVariable String spaceId) {
        return Result.success(teamSpaceService.getResources(spaceId));
    }

    @DeleteMapping("/{spaceId}/resources/{resourceId}")
    public Result<Void> removeResource(@PathVariable String spaceId, @PathVariable String resourceId) {
        String userId = String.valueOf(StpUtil.getLoginId());
        teamSpaceService.removeResource(spaceId, resourceId, userId);
        return Result.success();
    }

    @GetMapping("/{spaceId}/activities")
    public Result<List<TeamSpaceService.SpaceActivity>> getActivities(
            @PathVariable String spaceId,
            @RequestParam(defaultValue = "50") int limit) {
        return Result.success(teamSpaceService.getActivities(spaceId, limit));
    }

    // ==================== 文件共享 ====================

    @GetMapping("/{spaceId}/files")
    public Result<List<TeamSpaceFile>> listFiles(@PathVariable String spaceId) {
        return Result.success(teamSpaceService.getFiles(spaceId));
    }

    @PostMapping("/{spaceId}/files")
    public Result<TeamSpaceFile> uploadFile(@PathVariable String spaceId,
                                            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return Result.error("上传文件不能为空");
        }
        try {
            String userId = String.valueOf(StpUtil.getLoginId());
            // 使用 getCanonicalFile 解析 ".." 得到真实绝对路径，避免Tomcat相对路径问题
            File uploadRoot = new File(uploadBasePath).getCanonicalFile();
            File dir = new File(uploadRoot, "team-files" + File.separator + spaceId).getCanonicalFile();
            if (!dir.exists()) {
                dir.mkdirs();
            }
            String fileId = UUID.randomUUID().toString().substring(0, 12);
            String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "unnamed";
            String safeFileName = fileId + "_" + originalName.replaceAll("[^a-zA-Z0-9.\\-_\\u4e00-\\u9fa5]", "_");
            File dest = new File(dir, safeFileName).getCanonicalFile();
            // 使用 Files.copy 从输入流写入，完全绕过 ApplicationPart.write 的路径解析
            Files.copy(file.getInputStream(), dest.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            TeamSpaceFile spaceFile = new TeamSpaceFile();
            spaceFile.setId(fileId);
            spaceFile.setName(originalName);
            spaceFile.setSize(file.getSize());
            spaceFile.setContentType(file.getContentType());
            spaceFile.setUploadedBy(userId);
            spaceFile.setStoragePath(dest.getAbsolutePath());
            log.info("团队文件上传成功: {} -> {}", originalName, dest.getAbsolutePath());
            return Result.success(teamSpaceService.addFile(spaceId, spaceFile));
        } catch (IOException e) {
            log.error("团队文件上传失败", e);
            return Result.error("文件上传失败: " + e.getMessage());
        }
    }

    @GetMapping("/{spaceId}/files/{fileId}/download")
    public ResponseEntity<Resource> downloadFile(@PathVariable String spaceId, @PathVariable String fileId) {
        TeamSpaceFile spaceFile = teamSpaceService.getFile(spaceId, fileId);
        if (spaceFile == null || spaceFile.getStoragePath() == null) {
            return ResponseEntity.notFound().build();
        }
        File file = new File(spaceFile.getStoragePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        String encodedName = URLEncoder.encode(spaceFile.getName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(file.length())
                .body(new FileSystemResource(file));
    }

    @DeleteMapping("/{spaceId}/files/{fileId}")
    public Result<Void> deleteFile(@PathVariable String spaceId, @PathVariable String fileId) {
        String userId = String.valueOf(StpUtil.getLoginId());
        TeamSpaceFile spaceFile = teamSpaceService.getFile(spaceId, fileId);
        if (spaceFile != null && spaceFile.getStoragePath() != null) {
            try {
                Files.deleteIfExists(Paths.get(spaceFile.getStoragePath()));
            } catch (IOException e) {
                log.warn("删除文件失败", e);
            }
        }
        teamSpaceService.deleteFile(spaceId, fileId, userId);
        return Result.success();
    }

    // ==================== 团队聊天 ====================

    @GetMapping("/{spaceId}/messages")
    public Result<List<TeamSpaceMessage>> listMessages(@PathVariable String spaceId,
                                                       @RequestParam(defaultValue = "100") int limit) {
        String currentUserId = String.valueOf(StpUtil.getLoginId());
        List<TeamSpaceMessage> messages = teamSpaceService.getMessages(spaceId, limit);
        messages.forEach(m -> m.setMine(currentUserId.equals(m.getSenderId())));
        return Result.success(messages);
    }

    @PostMapping("/{spaceId}/messages")
    public Result<TeamSpaceMessage> sendMessage(@PathVariable String spaceId,
                                                @RequestBody MessageRequest req) {
        String userId = String.valueOf(StpUtil.getLoginId());
        TeamSpaceMessage msg = teamSpaceService.addMessage(spaceId, userId, userId, req.getContent());
        msg.setMine(true);
        return Result.success(msg);
    }

    @Data
    public static class MemberRequest {
        private String userId;
        private String role;
    }

    @Data
    public static class MessageRequest {
        private String content;
    }
}
