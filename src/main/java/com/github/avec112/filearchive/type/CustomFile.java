package com.github.avec112.filearchive.type;

import lombok.Data;
import lombok.Getter;
import lombok.Value;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class CustomFile {
    private String fileName;
    private String content;
}
