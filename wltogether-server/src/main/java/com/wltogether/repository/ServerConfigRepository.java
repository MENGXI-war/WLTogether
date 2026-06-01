package com.wltogether.repository;

import com.wltogether.model.entity.ServerConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ServerConfigRepository extends JpaRepository<ServerConfig, Long> {
    Optional<ServerConfig> findByConfigKey(String configKey);
}
