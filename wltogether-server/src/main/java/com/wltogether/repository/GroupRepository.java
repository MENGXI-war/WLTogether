package com.wltogether.repository;

import com.wltogether.model.entity.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface GroupRepository extends JpaRepository<Group, Long> {
    @Query("SELECT g FROM Group g JOIN GroupMember gm ON g.id = gm.groupId WHERE gm.userId = :userId ORDER BY g.lastMessageAt DESC NULLS LAST")
    List<Group> findByMemberUserId(Long userId);
}
