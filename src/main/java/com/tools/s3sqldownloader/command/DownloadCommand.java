package com.tools.s3sqldownloader.command;

import com.tools.s3sqldownloader.service.DownloadService;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

@ShellComponent
public class DownloadCommand {

    private final DownloadService downloadService;

    public DownloadCommand(DownloadService downloadService) {
        this.downloadService = downloadService;
    }

    @ShellMethod(key = "download-sql", value = "Executes a SQL query to fetch and download files from S3.")
    public void downloadBySql(
            @ShellOption(help = "The SQL SELECT query. Must return 'id' and 'filename' columns.")
            String query) {

        if (query == null || query.isBlank()) {
            System.err.println("Error: Query cannot be empty.");
            return;
        }

        if (!query.trim().toLowerCase().startsWith("select")) {
            System.err.println("Error: Only SELECT queries are allowed for safety reasons.");
            return;
        }

        downloadService.processDownload(query);
    }
}
