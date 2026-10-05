package com.recipereels.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface StorageService {
    void init();
    String storeImage(MultipartFile file);
    String storeVideo(MultipartFile file);
    Resource loadAsResource(String filename, String subDir);
    Path loadPath(String filename, String subDir);
    void deleteFile(String filename, String subDir);
}
