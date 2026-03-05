package com.blog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
public class SearchServiceApplication {

    public static void main(String[] args) {
        ensureLogDir();
        SpringApplication.run(SearchServiceApplication.class, args);
    }

    private static void ensureLogDir() {
        String logPath = System.getenv("LOG_PATH");
        if (logPath == null || logPath.isEmpty()) {
            logPath = System.getProperty("user.dir") + File.separator + "logs";
        }
        new File(logPath).mkdirs();
    }
}
