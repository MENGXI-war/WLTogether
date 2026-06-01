package com.wltogether.controller.admin;

import com.wltogether.model.dto.ApiResponse;
import com.wltogether.model.entity.Group;
import com.wltogether.repository.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/groups")
@RequiredArgsConstructor
public class AdminGroupController {

    private final GroupRepository groupRepository;

    @GetMapping
    public ResponseEntity<Page<Group>> list(Pageable pageable) {
        return ResponseEntity.ok(groupRepository.findAll(pageable));
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        Group group = groupRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("群组不存在"));
        groupRepository.delete(group);
        return ResponseEntity.ok(ApiResponse.ok("群组已删除"));
    }
}
