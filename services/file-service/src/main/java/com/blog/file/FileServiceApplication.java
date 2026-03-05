package com.blog.file;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;

@SpringBootApplication
@MapperScan("com.blog.file.mapper")
public class FileServiceApplication {

    public static void main(String[] args) {
        ensureLogDir();
        SpringApplication.run(FileServiceApplication.class, args);
    }

    private static void ensureLogDir() {
        String logPath = System.getenv("LOG_PATH");
        if (logPath == null || logPath.isEmpty()) {
            logPath = System.getProperty("user.dir") + File.separator + "logs";
        }
        new File(logPath).mkdirs();
    }
}
