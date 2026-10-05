package com.recipereels.service.impl;

import com.recipereels.exception.StorageException;
import com.recipereels.service.StorageService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
public class FileSystemStorageService implements StorageService {

    @Value("${app.upload.dir:uploads}")
    private String uploadDir;

    private Path rootLocation;
    private Path imageLocation;
    private Path videoLocation;

    private static final List<String> ALLOWED_IMAGE_EXTENSIONS = Arrays.asList("jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_VIDEO_EXTENSIONS = Arrays.asList("mp4", "webm", "mov");

    @PostConstruct
    @Override
    public void init() {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.imageLocation = this.rootLocation.resolve("images");
        this.videoLocation = this.rootLocation.resolve("videos");

        try {
            Files.createDirectories(this.imageLocation);
            Files.createDirectories(this.videoLocation);
        } catch (IOException e) {
            throw new StorageException("Could not initialize storage directories", e);
        }
    }

    @Override
    public String storeImage(MultipartFile file) {
        return storeFile(file, imageLocation, ALLOWED_IMAGE_EXTENSIONS, "images");
    }

    @Override
    public String storeVideo(MultipartFile file) {
        return storeFile(file, videoLocation, ALLOWED_VIDEO_EXTENSIONS, "videos");
    }

    private String storeFile(MultipartFile file, Path targetDir, List<String> allowedExtensions, String subDir) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Failed to store empty file");
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "");
        String extension = getFileExtension(originalFilename).toLowerCase();

        if (!allowedExtensions.contains(extension)) {
            throw new StorageException("Invalid file type. Allowed extensions are: " + String.join(", ", allowedExtensions));
        }

        String uniqueFilename = UUID.randomUUID().toString() + "." + extension;

        try {
            Path destinationFile = targetDir.resolve(uniqueFilename).normalize().toAbsolutePath();
            if (!destinationFile.getParent().equals(targetDir.toAbsolutePath())) {
                throw new StorageException("Cannot store file outside current directory");
            }
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new StorageException("Failed to store file " + originalFilename, e);
        }

        return "/uploads/" + subDir + "/" + uniqueFilename;
    }

    @Override
    public Resource loadAsResource(String filename, String subDir) {
        try {
            Path file = loadPath(filename, subDir);
            Resource resource = new UrlResource(file.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new StorageException("Could not read file: " + filename);
            }
        } catch (MalformedURLException e) {
            throw new StorageException("Could not read file: " + filename, e);
        }
    }

    @Override
    public Path loadPath(String filename, String subDir) {
        Path targetDir = "videos".equalsIgnoreCase(subDir) ? videoLocation : imageLocation;
        return targetDir.resolve(filename).normalize();
    }

    @Override
    public void deleteFile(String filename, String subDir) {
        try {
            Path file = loadPath(filename, subDir);
            Files.deleteIfExists(file);
        } catch (IOException e) {
            // Log and ignore delete failure
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }
}
