package com.wltogether.controller;

import com.wltogether.model.dto.*;
import com.wltogether.service.GroupService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
@RequiredArgsConstructor
public class GroupController {

    private final GroupService groupService;

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

    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<ApiResponse<Void>> removeMember(Authentication auth,
                                                           @PathVariable Long id,
                                                           @PathVariable Long userId) {
        Long operatorId = (Long) auth.getPrincipal();
        groupService.removeMember(id, operatorId, userId);
        return ResponseEntity.ok(ApiResponse.ok("已移出群组"));
    }
}
