package com.wltogether.service;

import com.wltogether.model.dto.*;
import com.wltogether.model.entity.Group;
import com.wltogether.model.entity.GroupMember;
import com.wltogether.model.entity.User;
import com.wltogether.repository.GroupMemberRepository;
import com.wltogether.repository.GroupRepository;
import com.wltogether.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Transactional
    public GroupResponse createGroup(Long userId, CreateGroupRequest request) {
        Group group = Group.builder()
                .name(request.getName())
                .description(request.getDescription())
                .ownerId(userId)
                .build();
        group = groupRepository.save(group);

        GroupMember member = GroupMember.builder()
                .groupId(group.getId())
                .userId(userId)
                .role("OWNER")
                .build();
        groupMemberRepository.save(member);

        return toResponse(group);
    }

    public List<GroupResponse> listGroups(Long userId) {
        return groupRepository.findByMemberUserId(userId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public GroupResponse getGroup(Long groupId, Long userId) {
        ensureMember(groupId, userId);
        return groupRepository.findById(groupId)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
    }

    @Transactional
    public GroupResponse updateGroup(Long groupId, Long userId, UpdateGroupRequest request) {
        Group group = ensureOwnerOrAdmin(groupId, userId);
        if (request.getName() != null) group.setName(request.getName());
        if (request.getDescription() != null) group.setDescription(request.getDescription());
        group = groupRepository.save(group);
        return toResponse(group);
    }

    @Transactional
    public void deleteGroup(Long groupId, Long userId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
        if (!group.getOwnerId().equals(userId)) {
            throw new IllegalArgumentException("仅群主可解散群组");
        }
        groupRepository.delete(group);
    }

    @Transactional
    public MemberResponse inviteMember(Long groupId, Long inviterId, InviteMemberRequest request) {
        ensureOwnerOrAdmin(groupId, inviterId);

        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        if (groupMemberRepository.existsByGroupIdAndUserId(groupId, user.getId())) {
            throw new IllegalArgumentException("该用户已在群组中");
        }

        GroupMember member = GroupMember.builder()
                .groupId(groupId)
                .userId(user.getId())
                .role("MEMBER")
                .build();
        groupMemberRepository.save(member);

        return MemberResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role("MEMBER")
                .joinedAt(member.getJoinedAt())
                .build();
    }

    @Transactional
    public void removeMember(Long groupId, Long operatorId, Long targetUserId) {
        if (operatorId.equals(targetUserId)) {
            // Self-leave
            GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, operatorId)
                    .orElseThrow(() -> new IllegalArgumentException("你不在此群组中"));
            if ("OWNER".equals(member.getRole())) {
                throw new IllegalArgumentException("群主需先转让群组才能退出");
            }
            groupMemberRepository.deleteByGroupIdAndUserId(groupId, operatorId);
        } else {
            ensureOwnerOrAdmin(groupId, operatorId);
            GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                    .orElseThrow(() -> new IllegalArgumentException("目标用户不在此群组中"));
            if ("OWNER".equals(target.getRole())) {
                throw new IllegalArgumentException("不能移除群主");
            }
            groupMemberRepository.deleteByGroupIdAndUserId(groupId, targetUserId);
        }
    }

    public List<MemberResponse> listMembers(Long groupId, Long userId) {
        ensureMember(groupId, userId);
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        return members.stream().map(m -> {
            User user = userRepository.findById(m.getUserId()).orElse(null);
            return MemberResponse.builder()
                    .userId(m.getUserId())
                    .username(user != null ? user.getUsername() : null)
                    .nickname(user != null ? user.getNickname() : null)
                    .nicknameInGroup(m.getNicknameInGroup())
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .role(m.getRole())
                    .joinedAt(m.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    private Group ensureOwnerOrAdmin(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("你不在此群组中"));
        if (!"OWNER".equals(member.getRole()) && !"ADMIN".equals(member.getRole())) {
            throw new IllegalArgumentException("仅群主/管理员可执行此操作");
        }
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
    }

    private void ensureMember(Long groupId, Long userId) {
        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw new IllegalArgumentException("你不在此群组中");
        }
    }

    private GroupResponse toResponse(Group group) {
        long count = groupMemberRepository.countByGroupId(group.getId());
        return GroupResponse.builder()
                .id(group.getId())
                .name(group.getName())
                .ownerId(group.getOwnerId())
                .description(group.getDescription())
                .avatarUrl(group.getAvatarUrl())
                .announcement(group.getAnnouncement())
                .lastMessageAt(group.getLastMessageAt())
                .memberCount(count)
                .createdAt(group.getCreatedAt())
                .build();
    }
}
