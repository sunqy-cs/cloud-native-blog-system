package com.blog.content;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

import java.io.File;

@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
public class ContentServiceApplication {

    public static void main(String[] args) {
        ensureLogDir();
        SpringApplication.run(ContentServiceApplication.class, args);
    }

    private static void ensureLogDir() {
        String logPath = System.getenv("LOG_PATH");
        if (logPath == null || logPath.isEmpty()) {
            logPath = System.getProperty("user.dir") + File.separator + "logs";
        }
        new File(logPath).mkdirs();
    }
}
