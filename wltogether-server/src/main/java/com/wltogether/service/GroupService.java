package com.wltogether.service;

import com.wltogether.model.dto.*;
import com.wltogether.model.entity.Group;
import com.wltogether.model.entity.GroupMember;
import com.wltogether.model.entity.User;
import com.wltogether.repository.GroupMemberRepository;
import com.wltogether.repository.GroupRepository;
import com.wltogether.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final GroupMemberRepository groupMemberRepository;
    private final UserRepository userRepository;

    @Value("${storage.avatar-dir:./data}/avatars")
    private String avatarDir;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/png", "image/jpeg", "image/webp"
    );

    // ===================== Basic CRUD =====================

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
        if (request.getAnnouncement() != null) group.setAnnouncement(request.getAnnouncement());
        if (request.getTags() != null) group.setTags(request.getTags());
        if (request.getJoinMode() != null) group.setJoinMode(request.getJoinMode());
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

    // ===================== Members =====================

    @Transactional
    public MemberResponse inviteMember(Long groupId, Long inviterId, InviteMemberRequest request) {
        Group group = ensureOwnerOrAdmin(groupId, inviterId);

        // Check join mode - if not INVITE_ONLY, user can also join via application
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
                .uid(user.getUid())
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
            throw new IllegalArgumentException("请使用 /members/me 端点退出群组");
        }
        ensureOwnerOrAdmin(groupId, operatorId);
        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不在此群组中"));
        if ("OWNER".equals(target.getRole())) {
            throw new IllegalArgumentException("不能移除群主");
        }
        groupMemberRepository.deleteByGroupIdAndUserId(groupId, targetUserId);
    }

    public List<MemberResponse> listMembers(Long groupId, Long userId) {
        ensureMember(groupId, userId);
        List<GroupMember> members = groupMemberRepository.findByGroupId(groupId);
        return members.stream().map(m -> {
            User user = userRepository.findById(m.getUserId()).orElse(null);
            return MemberResponse.builder()
                    .userId(m.getUserId())
                    .uid(user != null ? user.getUid() : null)
                    .username(user != null ? user.getUsername() : null)
                    .nickname(user != null ? user.getNickname() : null)
                    .nicknameInGroup(m.getNicknameInGroup())
                    .avatarUrl(user != null ? user.getAvatarUrl() : null)
                    .role(m.getRole())
                    .joinedAt(m.getJoinedAt())
                    .build();
        }).collect(Collectors.toList());
    }

    // ===================== Self-leave =====================

    @Transactional
    public void leaveGroup(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("你不在此群组中"));
        if ("OWNER".equals(member.getRole())) {
            throw new IllegalArgumentException("群主需先转让群组才能退出");
        }
        groupMemberRepository.deleteByGroupIdAndUserId(groupId, userId);
        log.info("User {} left group {}", userId, groupId);
    }

    // ===================== Role Management =====================

    @Transactional
    public void changeMemberRole(Long groupId, Long operatorId, Long targetUserId, String newRole) {
        GroupMember operator = ensureOwnerOrAdminMember(groupId, operatorId);

        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不在此群组中"));

        // Only OWNER can promote to ADMIN
        if ("ADMIN".equals(newRole) && !"OWNER".equals(operator.getRole())) {
            throw new IllegalArgumentException("仅群主可设置管理员");
        }
        // Cannot change OWNER role
        if ("OWNER".equals(target.getRole())) {
            throw new IllegalArgumentException("不能修改群主的角色");
        }
        // Only OWNER can demote ADMIN
        if ("ADMIN".equals(target.getRole()) && "MEMBER".equals(newRole) && !"OWNER".equals(operator.getRole())) {
            throw new IllegalArgumentException("仅群主可取消管理员");
        }

        target.setRole(newRole);
        groupMemberRepository.save(target);
        log.info("User {} changed role of {} in group {} to {}", operatorId, targetUserId, groupId, newRole);
    }

    @Transactional
    public void transferOwnership(Long groupId, Long currentOwnerId, Long newOwnerId) {
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
        if (!group.getOwnerId().equals(currentOwnerId)) {
            throw new IllegalArgumentException("仅群主可转让群组");
        }
        if (currentOwnerId.equals(newOwnerId)) {
            throw new IllegalArgumentException("不能转让给自己");
        }

        GroupMember targetMember = groupMemberRepository.findByGroupIdAndUserId(groupId, newOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不在此群组中"));

        // Update group owner
        group.setOwnerId(newOwnerId);
        groupRepository.save(group);

        // Switch roles
        GroupMember currentOwnerMember = groupMemberRepository.findByGroupIdAndUserId(groupId, currentOwnerId)
                .orElseThrow(() -> new IllegalArgumentException("当前群主不在群组中"));
        currentOwnerMember.setRole("ADMIN");
        targetMember.setRole("OWNER");
        groupMemberRepository.save(currentOwnerMember);
        groupMemberRepository.save(targetMember);

        log.info("Ownership of group {} transferred from {} to {}", groupId, currentOwnerId, newOwnerId);
    }

    // ===================== Mute =====================

    @Transactional
    public void muteMember(Long groupId, Long operatorId, Long targetUserId, String duration) {
        ensureOwnerOrAdmin(groupId, operatorId);

        GroupMember target = groupMemberRepository.findByGroupIdAndUserId(groupId, targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("目标用户不在此群组中"));
        if ("OWNER".equals(target.getRole())) {
            throw new IllegalArgumentException("不能禁言群主");
        }

        Instant mutedUntil;
        switch (duration) {
            case "1h":
                mutedUntil = Instant.now().plus(1, ChronoUnit.HOURS);
                break;
            case "8h":
                mutedUntil = Instant.now().plus(8, ChronoUnit.HOURS);
                break;
            case "forever":
                mutedUntil = Instant.now().plus(36500, ChronoUnit.DAYS); // ~100 years
                break;
            case "none":
                mutedUntil = null;
                break;
            default:
                throw new IllegalArgumentException("无效的禁言时长: " + duration);
        }

        target.setMutedUntil(mutedUntil);
        groupMemberRepository.save(target);
        log.info("User {} muted user {} in group {} until {}", operatorId, targetUserId, groupId, mutedUntil);
    }

    // ===================== Nickname in Group =====================

    @Transactional
    public void updateNicknameInGroup(Long groupId, Long userId, String nicknameInGroup) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("你不在此群组中"));

        member.setNicknameInGroup(nicknameInGroup);
        groupMemberRepository.save(member);
        log.info("User {} set nickname in group {} to '{}'", userId, groupId, nicknameInGroup);
    }

    // ===================== Avatar =====================

    @Transactional
    public String uploadGroupAvatar(Long groupId, Long userId, MultipartFile file) throws IOException {
        ensureOwnerOrAdmin(groupId, userId);

        if (file.isEmpty()) {
            throw new IllegalArgumentException("文件为空");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new IllegalArgumentException("头像文件不能超过 2MB");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("仅支持 PNG / JPEG / WebP 格式");
        }

        // Read, resize, convert to PNG
        BufferedImage original;
        try (InputStream in = file.getInputStream()) {
            original = ImageIO.read(in);
        }
        if (original == null) {
            throw new IllegalArgumentException("无法解析图片文件，请确认文件未损坏");
        }
        BufferedImage resized = resizeImage(original, 256, 256);
        byte[] pngBytes;
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ImageIO.write(resized, "PNG", baos);
            pngBytes = baos.toByteArray();
        }

        // Ensure directory
        Path dir = Paths.get(avatarDir);
        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        }

        // Save: {avatarDir}/group_{groupId}.png
        Path avatarPath = dir.resolve("group_" + groupId + ".png");
        Files.write(avatarPath, pngBytes);

        // Update DB
        String avatarUrl = "/api/groups/" + groupId + "/avatar?t=" + System.currentTimeMillis();
        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
        group.setAvatarUrl(avatarUrl);
        groupRepository.save(group);

        log.info("Group avatar uploaded for group {} ({} bytes)", groupId, pngBytes.length);
        return avatarUrl;
    }

    @Transactional
    public void deleteGroupAvatar(Long groupId, Long userId) {
        ensureOwnerOrAdmin(groupId, userId);

        Group group = groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
        group.setAvatarUrl(null);
        groupRepository.save(group);

        Path avatarPath = Paths.get(avatarDir, "group_" + groupId + ".png");
        try {
            Files.deleteIfExists(avatarPath);
        } catch (IOException e) {
            log.warn("Failed to delete group avatar file: {}", avatarPath, e);
        }
        log.info("Group avatar deleted for group {}", groupId);
    }

    // ===================== Helper Methods =====================

    private Group ensureOwnerOrAdmin(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("你不在此群组中"));
        if (!"OWNER".equals(member.getRole()) && !"ADMIN".equals(member.getRole())) {
            throw new IllegalArgumentException("仅群主/管理员可执行此操作");
        }
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
    }

    private GroupMember ensureOwnerOrAdminMember(Long groupId, Long userId) {
        GroupMember member = groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                .orElseThrow(() -> new IllegalArgumentException("你不在此群组中"));
        if (!"OWNER".equals(member.getRole()) && !"ADMIN".equals(member.getRole())) {
            throw new IllegalArgumentException("仅群主/管理员可执行此操作");
        }
        return member;
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
                .joinMode(group.getJoinMode())
                .tags(group.getTags())
                .lastMessageAt(group.getLastMessageAt())
                .memberCount(count)
                .createdAt(group.getCreatedAt())
                .build();
    }

    private BufferedImage resizeImage(BufferedImage original, int width, int height) {
        int srcW = original.getWidth();
        int srcH = original.getHeight();
        int cropSize = Math.min(srcW, srcH);
        int cropX = (srcW - cropSize) / 2;
        int cropY = (srcH - cropSize) / 2;

        BufferedImage cropped = original.getSubimage(cropX, cropY, cropSize, cropSize);
        BufferedImage resized = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = resized.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(cropped, 0, 0, width, height, null);
        g.dispose();

        return resized;
    }
}
