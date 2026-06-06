package com.actionth.membership.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.actionth.membership.model.AppConfig;
import com.actionth.membership.repository.AppConfigRepository;

@Service
public class AppConfigService {

    @Autowired
    private AppConfigRepository appConfigRepository;

    public String findFirstByName(String name) {
        return appConfigRepository.findFirstByName(name)
                .map(AppConfig::getValue)
                .orElse(null);
    }

    public int getIntConfig(String key, int defaultValue) {
        String val = findFirstByName(key);
        if (val == null) return defaultValue;
        try {
            return Integer.parseInt(val.trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public void setConfig(String key, String value) {
        AppConfig config = appConfigRepository.findFirstByName(key)
                .orElseGet(() -> {
                    AppConfig c = new AppConfig();
                    c.setName(key);
                    return c;
                });
        config.setValue(value);
        appConfigRepository.save(config);
    }
}
