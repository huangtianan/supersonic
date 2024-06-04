package com.tencent.supersonic.headless.core.config;

import com.tencent.supersonic.headless.core.chat.knowledge.helper.HanlpHelper;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.FileNotFoundException;


@Data
@Configuration
@Slf4j
public class ChatLocalFileConfig {


    @Value("${dict.directory.latest:/data/dictionary/custom}")
    private String dictDirectoryLatest;

    @Value("${dict.directory.backup:./dict/backup}")
    private String dictDirectoryBackup;

    public String getDictDirectoryLatest() {
        return getResourceDir() + dictDirectoryLatest;
    }

    public String getDictDirectoryBackup() {
        return dictDirectoryBackup;
    }

    private String getResourceDir() {
        String hanlpPropertiesPath = "";
        try {
            hanlpPropertiesPath = HanlpHelper.getHanlpPropertiesPath();
        } catch (FileNotFoundException e) {
            log.warn("getResourceDir, e:", e);
        }
        return hanlpPropertiesPath;
    }
}