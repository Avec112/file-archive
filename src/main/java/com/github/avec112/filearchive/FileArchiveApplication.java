package com.github.avec112.filearchive;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.theme.lumo.Lumo;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@Theme(value = "pdfsearch", variant = Lumo.DARK)
public class FileArchiveApplication implements AppShellConfigurator {

    public static void main(String[] args) {
        SpringApplication.run(FileArchiveApplication.class, args);
    }

}

