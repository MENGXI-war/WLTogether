package com.wltogether.service;

import com.wltogether.model.entity.ServerConfig;
import com.wltogether.repository.ServerConfigRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class ServerConfigService {

    private final ServerConfigRepository configRepository;
    private final Map<String, String> cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadConfigs() {
        configRepository.findAll().forEach(c -> cache.put(c.getConfigKey(), c.getConfigValue()));
        log.info("Loaded {} server config entries", cache.size());
    }

    public String getValue(String key, String defaultValue) {
        return cache.getOrDefault(key, defaultValue);
    }

    public boolean getBool(String key, boolean defaultValue) {
        return Optional.ofNullable(cache.get(key))
                .map(Boolean::parseBoolean)
                .orElse(defaultValue);
    }

    public int getInt(String key, int defaultValue) {
        try {
            return Optional.ofNullable(cache.get(key))
                    .map(Integer::parseInt)
                    .orElse(defaultValue);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setValue(String key, String value, Long updatedBy) {
        ServerConfig config = configRepository.findByConfigKey(key)
                .orElseGet(() -> ServerConfig.builder().configKey(key).configValue(value).valueType("STRING").build());
        config.setConfigValue(value);
        config.setUpdatedBy(updatedBy);
        configRepository.save(config);
        cache.put(key, value);
    }

    public void reload() {
        cache.clear();
        loadConfigs();
    }
}
