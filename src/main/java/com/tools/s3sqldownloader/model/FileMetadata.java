package com.tools.s3sqldownloader.model;

/**
 * Represents the minimal metadata required to download a file.
 * @param id The S3 key (usually a UUID).
 * @param filename The desired output filename.
 */
public record FileMetadata(String id, String filename) {
    public FileMetadata {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("File ID cannot be null or empty");
        }
        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException("Filename cannot be null or empty");
        }
    }
}
