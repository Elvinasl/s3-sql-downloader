package com.tools.s3sqldownloader.service;

import com.tools.s3sqldownloader.model.FileMetadata;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DownloadService {

    private static final String DEFAULT_DOWNLOAD_DIR = "downloads";

    private final JdbcTemplate jdbcTemplate;
    private final S3Client s3Client;
    private final String bucketName;

    public DownloadService(JdbcTemplate jdbcTemplate,
                           S3Client s3Client,
                           @Value("${aws.s3.bucket}") String bucketName) {
        this.jdbcTemplate = jdbcTemplate;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
    }

    /**
     * Executes the provided SQL query and downloads the corresponding files from S3.
     *
     * @param sqlQuery The SQL query to execute. Must return 'id' and 'filename' columns.
     */
    public void processDownload(String sqlQuery) {
        log.info("Executing query: {}", sqlQuery);

        List<FileMetadata> files;
        try {
            files = fetchMetadata(sqlQuery);
        } catch (Exception e) {
            log.error("Failed to execute database query. Ensure columns 'id' and 'filename' exist.", e);
            return;
        }

        if (files.isEmpty()) {
            log.warn("Query returned 0 results.");
            return;
        }

        log.info("Found {} files. Starting download...", files.size());

        try {
            Path downloadDir = prepareDownloadDirectory();
            downloadFiles(files, downloadDir);
        } catch (IOException e) {
            log.error("Failed to initialize download directory.", e);
        }
    }

    private List<FileMetadata> fetchMetadata(String query) {
        return jdbcTemplate.query(query, (rs, rowNum) -> {
            // Provide a clear error message if columns are missing
            String id;
            String filename;
            try {
                id = rs.getString("id");
                filename = rs.getString("filename");
            } catch (Exception e) {
                throw new IllegalArgumentException("ResultSet missing required columns 'id' or 'filename'");
            }
            return new FileMetadata(id, filename);
        });
    }

    private Path prepareDownloadDirectory() throws IOException {
        Path path = Paths.get(DEFAULT_DOWNLOAD_DIR);
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }
        return path;
    }

    private void downloadFiles(List<FileMetadata> files, Path directory) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (FileMetadata file : files) {
            try {
                String safeFilename = sanitizeFilename(file.filename());
                Path targetPath = resolveUniquePath(directory, safeFilename);

                log.debug("Downloading ID: {} to {}", file.id(), targetPath);

                GetObjectRequest request = GetObjectRequest.builder()
                        .bucket(bucketName)
                        .key(file.id())
                        .build();

                s3Client.getObject(request, ResponseTransformer.toFile(targetPath));
                successCount.incrementAndGet();

            } catch (S3Exception e) {
                log.error("S3 Error for file ID {}: {}", file.id(), e.awsErrorDetails().errorMessage());
                failCount.incrementAndGet();
            } catch (Exception e) {
                log.error("General Error for file ID {}: {}", file.id(), e.getMessage());
                failCount.incrementAndGet();
            }
        }

        log.info("Download completed. Success: {}, Failed: {}", successCount.get(), failCount.get());
    }

    private Path resolveUniquePath(Path dir, String filename) {
        Path target = dir.resolve(filename);
        int counter = 1;
        String name = filename;
        String ext = "";

        int dotIndex = filename.lastIndexOf(".");
        if (dotIndex > 0) {
            name = filename.substring(0, dotIndex);
            ext = filename.substring(dotIndex);
        }

        while (Files.exists(target)) {
            target = dir.resolve(String.format("%s (%d)%s", name, counter++, ext));
        }
        return target;
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
