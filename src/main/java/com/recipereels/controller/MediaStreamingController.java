package com.recipereels.controller;

import com.recipereels.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
@RequestMapping("/api/media")
public class MediaStreamingController {

    private final StorageService storageService;

    public MediaStreamingController(StorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/videos/{filename:.+}")
    public ResponseEntity<ResourceRegion> streamVideo(
            @PathVariable String filename,
            @RequestHeader(value = "Range", required = false) HttpHeaders headers) {

        try {
            Resource video = storageService.loadAsResource(filename, "videos");
            long contentLength = video.contentLength();
            HttpRange range = headers != null && !headers.getRange().isEmpty() ? headers.getRange().get(0) : null;

            if (range != null) {
                long start = range.getRangeStart(contentLength);
                long end = range.getRangeEnd(contentLength);
                long rangeLength = Math.min(1024 * 1024, end - start + 1); // 1MB chunks for snappy buffering
                ResourceRegion region = new ResourceRegion(video, start, rangeLength);

                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .header("Accept-Ranges", "bytes")
                        .body(region);
            } else {
                long rangeLength = Math.min(1024 * 1024, contentLength);
                ResourceRegion region = new ResourceRegion(video, 0, rangeLength);
                return ResponseEntity.status(HttpStatus.PARTIAL_CONTENT)
                        .contentType(MediaTypeFactory.getMediaType(video).orElse(MediaType.APPLICATION_OCTET_STREAM))
                        .header("Accept-Ranges", "bytes")
                        .body(region);
            }
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
