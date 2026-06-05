package com.wltogether.controller;

import com.wltogether.model.dto.*;
import com.wltogether.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

    // ========== Basic CRUD ==========

    @PostMapping
    public ResponseEntity<GroupResponse> create(Authentication auth,
                                                 @Valid @RequestBody CreateGroupRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.status(HttpStatus.CREATED).body(groupService.createGroup(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<GroupResponse>> list(Authentication auth) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(groupService.listGroups(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GroupResponse> get(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(groupService.getGroup(id, userId));
    }

    @PutMapping("/{id}")
    public ResponseEntity<GroupResponse> update(Authentication auth,
                                                 @PathVariable Long id,
                                                 @RequestBody UpdateGroupRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(groupService.updateGroup(id, userId, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        groupService.deleteGroup(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("群组已解散"));
    }

    // ========== Members ==========

    @GetMapping("/{id}/members")
    public ResponseEntity<List<MemberResponse>> listMembers(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(groupService.listMembers(id, userId));
    }

    @PostMapping("/{id}/members")
    public ResponseEntity<MemberResponse> invite(Authentication auth,
                                                  @PathVariable Long id,
                                                  @Valid @RequestBody InviteMemberRequest request) {
        Long userId = (Long) auth.getPrincipal();
        return ResponseEntity.ok(groupService.inviteMember(id, userId, request));
    }

    @DeleteMapping("/{id}/members/{targetUserId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(Authentication auth,
                                                           @PathVariable Long id,
                                                           @PathVariable Long targetUserId) {
        Long operatorId = (Long) auth.getPrincipal();
        groupService.removeMember(id, operatorId, targetUserId);
        return ResponseEntity.ok(ApiResponse.ok("已移出群组"));
    }

    // ---- Self-leave ----

    @DeleteMapping("/{id}/members/me")
    public ResponseEntity<ApiResponse<Void>> leave(Authentication auth, @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        groupService.leaveGroup(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("已退出群组"));
    }

    // ========== Role Management ==========

    @PutMapping("/{id}/members/{targetUserId}/role")
    public ResponseEntity<ApiResponse<Void>> changeRole(Authentication auth,
                                                         @PathVariable Long id,
                                                         @PathVariable Long targetUserId,
                                                         @Valid @RequestBody ChangeRoleRequest request) {
        Long operatorId = (Long) auth.getPrincipal();
        groupService.changeMemberRole(id, operatorId, targetUserId, request.getRole());
        return ResponseEntity.ok(ApiResponse.ok("角色已更新"));
    }

    @PutMapping("/{id}/owner")
    public ResponseEntity<ApiResponse<Void>> transferOwner(Authentication auth,
                                                            @PathVariable Long id,
                                                            @Valid @RequestBody TransferOwnerRequest request) {
        Long userId = (Long) auth.getPrincipal();
        groupService.transferOwnership(id, userId, request.getNewOwnerId());
        return ResponseEntity.ok(ApiResponse.ok("群主已转让"));
    }

    // ========== Mute ==========

    @PutMapping("/{id}/members/{targetUserId}/mute")
    public ResponseEntity<ApiResponse<Void>> muteMember(Authentication auth,
                                                         @PathVariable Long id,
                                                         @PathVariable Long targetUserId,
                                                         @Valid @RequestBody MuteMemberRequest request) {
        Long operatorId = (Long) auth.getPrincipal();
        groupService.muteMember(id, operatorId, targetUserId, request.getDuration());
        return ResponseEntity.ok(ApiResponse.ok("已更新禁言状态"));
    }

    // ========== Nickname in Group ==========

    @PutMapping("/{id}/members/me/nickname")
    public ResponseEntity<ApiResponse<Void>> updateNickname(Authentication auth,
                                                             @PathVariable Long id,
                                                             @Valid @RequestBody UpdateNicknameRequest request) {
        Long userId = (Long) auth.getPrincipal();
        groupService.updateNicknameInGroup(id, userId, request.getNicknameInGroup());
        return ResponseEntity.ok(ApiResponse.ok("群内昵称已更新"));
    }

    // ========== Avatar ==========

    @GetMapping("/{id}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long id) {
        try {
            byte[] bytes = groupService.getGroupAvatar(id);
            return ResponseEntity.ok()
                    .contentType(org.springframework.http.MediaType.IMAGE_PNG)
                    .body(bytes);
        } catch (IOException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @PostMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<String>> uploadAvatar(Authentication auth,
                                                             @PathVariable Long id,
                                                             @RequestParam("file") MultipartFile file) {
        Long userId = (Long) auth.getPrincipal();
        try {
            String avatarUrl = groupService.uploadGroupAvatar(id, userId, file);
            return ResponseEntity.ok(ApiResponse.ok("群头像上传成功", avatarUrl));
        } catch (IOException e) {
            throw new IllegalArgumentException("群头像处理失败: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<Void>> deleteAvatar(Authentication auth,
                                                           @PathVariable Long id) {
        Long userId = (Long) auth.getPrincipal();
        groupService.deleteGroupAvatar(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("群头像已删除"));
    }
}
