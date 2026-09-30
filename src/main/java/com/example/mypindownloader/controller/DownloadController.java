 package com.example.mypindownloader.controller;

import com.example.mypindownloader.service.PinterestService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class DownloadController {

    private final PinterestService pinterestService;

    public DownloadController(
            PinterestService pinterestService) {

        this.pinterestService =
                pinterestService;
    }


    // =========================================================
    // GET PINTEREST MEDIA URL
    // =========================================================

    @PostMapping("/download")
    public Map<String, Object> download(
            @RequestBody Map<String, String> request) {

        System.out.println(
                "========== /api/download CALLED =========="
        );


        // -----------------------------------------------------
        // Get Pinterest URL
        // -----------------------------------------------------

        String pinterestUrl =
                request.get("pinterestUrl");

        System.out.println(
                "Pinterest URL received: "
                        + pinterestUrl
        );


        // -----------------------------------------------------
        // Check empty URL
        // -----------------------------------------------------

        if (pinterestUrl == null
                || pinterestUrl.isBlank()) {

            return Map.of(
                    "success",
                    false,

                    "message",
                    "Please enter a Pinterest URL.",

                    "mediaUrl",
                    ""
            );
        }


        pinterestUrl =
                pinterestUrl.trim();


        // -----------------------------------------------------
        // Validate Pinterest host
        // -----------------------------------------------------

        try {

            URI uri =
                    URI.create(
                            pinterestUrl
                    );

            String host =
                    uri.getHost() == null
                            ? null
                            : uri.getHost().toLowerCase();


            if (host == null
                    || !(host.equals("pinterest.com")
                    || host.endsWith(".pinterest.com")
                    || host.equals("pin.it"))) {

                return Map.of(
                        "success",
                        false,

                        "message",
                        "Please enter a valid Pinterest URL.",

                        "mediaUrl",
                        ""
                );
            }

        } catch (IllegalArgumentException e) {

            return Map.of(
                    "success",
                    false,

                    "message",
                    "Please enter a valid Pinterest URL.",

                    "mediaUrl",
                    ""
            );
        }


        // -----------------------------------------------------
        // Get Pin ID
        // -----------------------------------------------------

        System.out.println(
                "Getting Pin ID..."
        );

        String pinId =
                pinterestService.getPinId(
                        pinterestUrl
                );


        System.out.println(
                "Pin ID: "
                        + pinId
        );


        // -----------------------------------------------------
        // Validate Pin ID
        // -----------------------------------------------------

        if (pinId == null
                || pinId.isBlank()
                || !pinId.matches("\\d+")) {

            return Map.of(
                    "success",
                    false,

                    "message",
                    "Could not determine the Pinterest Pin ID.",

                    "mediaUrl",
                    ""
            );
        }


        // -----------------------------------------------------
        // Get media data
        // -----------------------------------------------------

        System.out.println(
                "Getting Pin media data..."
        );

        Map<String, Object> mediaData =
                pinterestService.getMediaData(
                        pinterestUrl
                );


        System.out.println(
                "Media data received."
        );


        // -----------------------------------------------------
        // Check media data
        // -----------------------------------------------------

        if (mediaData == null) {

            return Map.of(
                    "success",
                    false,

                    "message",
                    "Unable to retrieve Pin media data.",

                    "mediaUrl",
                    ""
            );
        }


        // -----------------------------------------------------
        // Get media URL safely
        // -----------------------------------------------------

        String mediaUrl =
                mediaData.get("mediaUrl")
                        instanceof String
                        ? (String)
                        mediaData.get("mediaUrl")
                        : "";


        // -----------------------------------------------------
        // Get video qualities safely
        // -----------------------------------------------------

        List<Map<String, String>>
                videoQualities =
                mediaData.get("videoQualities")
                        instanceof List
                        ? (List<Map<String, String>>)
                        mediaData.get("videoQualities")
                        : List.of();


        // -----------------------------------------------------
        // Check media URL
        // -----------------------------------------------------

        if (mediaUrl == null
                || mediaUrl.isBlank()) {

            return Map.of(
                    "success",
                    false,

                    "message",
                    "This Pin does not contain a downloadable image or video.",

                    "mediaUrl",
                    "",

                    "pinId",
                    pinId,

                    "videoQualities",
                    videoQualities
            );
        }


        // -----------------------------------------------------
        // Determine media type
        // -----------------------------------------------------

        String mediaType;

        String lowerMediaUrl =
                mediaUrl.toLowerCase();


        if (lowerMediaUrl.contains(".m3u8")
                || lowerMediaUrl.contains(".mp4")) {

            mediaType =
                    "video";

        } else {

            mediaType =
                    "image";
        }


        // -----------------------------------------------------
        // Print final result
        // -----------------------------------------------------

        System.out.println(
                "Media type: "
                        + mediaType
        );

        System.out.println(
                "Media URL: "
                        + mediaUrl
        );


        // -----------------------------------------------------
        // Return result
        // -----------------------------------------------------

        return Map.of(
                "success",
                true,

                "message",
                "Pinterest media found successfully.",

                "mediaUrl",
                mediaUrl,

                "mediaType",
                mediaType,

                "pinId",
                pinId,

                "videoQualities",
                videoQualities
        );
    }


    // =========================================================
    // DOWNLOAD IMAGE
    // =========================================================

    @GetMapping("/download-image")
    public ResponseEntity<byte[]> downloadImage(
            @RequestParam String url,
            @RequestParam String pinId) {


        // -----------------------------------------------------
        // Check Pin ID
        // -----------------------------------------------------

        if (pinId == null
                || pinId.isBlank()
                || !pinId.matches("\\d+")) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid Pin ID"
                                    .getBytes()
                    );
        }


        // -----------------------------------------------------
        // Check image URL
        // -----------------------------------------------------

        if (url == null
                || url.isBlank()) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid image URL"
                                    .getBytes()
                    );
        }


        // -----------------------------------------------------
        // Validate image URL
        // -----------------------------------------------------

        try {

            URI imageUri =
                    URI.create(url);

            String scheme =
                    imageUri.getScheme();

            String host =
                    imageUri.getHost();


            if (scheme == null
                    || !scheme.equalsIgnoreCase("https")
                    || host == null
                    || !host.equalsIgnoreCase(
                    "i.pinimg.com")) {

                return ResponseEntity
                        .badRequest()
                        .body(
                                "Invalid image URL"
                                        .getBytes()
                        );
            }

        } catch (IllegalArgumentException e) {

            return ResponseEntity
                    .badRequest()
                    .body(
                            "Invalid image URL"
                                    .getBytes()
                    );
        }


        try {

            System.out.println(
                    "Downloading image for Pin ID: "
                            + pinId
            );


            // -------------------------------------------------
            // HTTP client
            // -------------------------------------------------

            HttpClient client =
                    HttpClient.newBuilder()
                            .followRedirects(
                                    HttpClient.Redirect.NORMAL
                            )
                            .build();


            // -------------------------------------------------
            // HTTP request
            // -------------------------------------------------

            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(url)
                            )
                            .timeout(
                                    java.time.Duration
                                            .ofSeconds(10)
                            )
                            .GET()
                            .build();


            // -------------------------------------------------
            // Send request
            // -------------------------------------------------

            HttpResponse<byte[]> response =
                    client.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofByteArray()
                    );


            System.out.println(
                    "Image HTTP status: "
                            + response.statusCode()
            );


            // -------------------------------------------------
            // Check HTTP status
            // -------------------------------------------------

            if (response.statusCode() != 200) {

                return ResponseEntity
                        .status(
                                response.statusCode()
                        )
                        .body(
                                "Invalid or unavailable image URL"
                                        .getBytes()
                        );
            }


            // -------------------------------------------------
            // Check body
            // -------------------------------------------------

            if (response.body() == null
                    || response.body().length == 0) {

                return ResponseEntity
                        .internalServerError()
                        .body(
                                "Downloaded image is empty"
                                        .getBytes()
                        );
            }


            // -------------------------------------------------
            // Content type
            // -------------------------------------------------

            String contentType =
                    response.headers()
                            .firstValue(
                                    "Content-Type"
                            )
                            .orElse("");


            if (!contentType
                    .toLowerCase()
                    .startsWith("image/")) {

                System.out.println(
                        "Invalid image content type: "
                                + contentType
                );

                return ResponseEntity
                        .internalServerError()
                        .body(
                                "Downloaded content is not an image"
                                        .getBytes()
                        );
            }


            // -------------------------------------------------
            // Determine extension
            // -------------------------------------------------

            String extension =
                    ".jpg";

            String lowerContentType =
                    contentType.toLowerCase();


            if (lowerContentType.contains("png")) {

                extension =
                        ".png";

            } else if (
                    lowerContentType.contains("webp")) {

                extension =
                        ".webp";
            }


            // -------------------------------------------------
            // Return image
            // -------------------------------------------------

            return ResponseEntity.ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"pinterest-image-"
                                    + pinId
                                    + extension
                                    + "\""
                    )
                    .contentType(
                            MediaType.parseMediaType(
                                    contentType
                            )
                    )
                    .body(
                            response.body()
                    );

        } catch (Exception e) {

            e.printStackTrace();

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Unable to download image"
                                    .getBytes()
                    );
        }
    }


    // =========================================================
    // DOWNLOAD VIDEO
    // =========================================================
    @GetMapping("/download-video")
    public ResponseEntity<byte[]> downloadVideo(
            @RequestParam String url,
            @RequestParam String pinId,
            @RequestParam(required = false, defaultValue = "video") String quality) {

        // Validate Pin ID
        if (pinId == null || !pinId.matches("\\d+")) {
            return ResponseEntity.badRequest()
                    .body("Invalid Pin ID".getBytes());
        }

        // Validate video URL
        try {
            URI videoUri = URI.create(url);

            if (!"https".equalsIgnoreCase(videoUri.getScheme())) {
                return ResponseEntity.badRequest()
                        .body("Only HTTPS video URLs are allowed".getBytes());
            }

            String host = videoUri.getHost();

            if (host == null ||
                    !"v1.pinimg.com".equalsIgnoreCase(host)) {

                return ResponseEntity.badRequest()
                        .body("Invalid Pinterest video URL".getBytes());
            }

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Invalid video URL".getBytes());
        }

        Path outputFile = null;

        try {

            System.out.println(
                    "Downloading video for Pin ID: " + pinId
            );

            System.out.println(
                    "Selected quality: " + quality
            );

            outputFile =
                    Files.createTempFile(
                            "pinterest-video-",
                            ".mp4"
                    );

            ProcessBuilder processBuilder;

            // HLS video
            if (url.toLowerCase().contains(".m3u8")) {

                processBuilder =
                        new ProcessBuilder(
                                "ffmpeg",
                                "-y",
                                "-i",
                                url,
                                "-c:v",
                                "copy",
                                "-c:a",
                                "aac",
                                "-movflags",
                                "+faststart",
                                outputFile.toString()
                        );

            } else {

                // Normal MP4 video
                processBuilder =
                        new ProcessBuilder(
                                "ffmpeg",
                                "-y",
                                "-i",
                                url,
                                "-c",
                                "copy",
                                outputFile.toString()
                        );
            }

            processBuilder
                    .redirectErrorStream(true)
                    .redirectOutput(
                            ProcessBuilder.Redirect.INHERIT
                    );

            Process process =
                    processBuilder.start();

            boolean finished =
                    process.waitFor(
                            60,
                            TimeUnit.SECONDS
                    );

            if (!finished) {

                process.destroyForcibly();

                Files.deleteIfExists(outputFile);

                return ResponseEntity
                        .internalServerError()
                        .body(
                                "Video download timed out"
                                        .getBytes()
                        );
            }

            int exitCode =
                    process.exitValue();

            if (exitCode != 0) {

                Files.deleteIfExists(outputFile);

                return ResponseEntity
                        .internalServerError()
                        .body(
                                "FFmpeg could not process the video"
                                        .getBytes()
                        );
            }

            if (!Files.exists(outputFile)) {

                return ResponseEntity
                        .internalServerError()
                        .body(
                                "Video file was not created"
                                        .getBytes()
                        );
            }

            long fileSize =
                    Files.size(outputFile);

            if (fileSize == 0) {

                Files.deleteIfExists(outputFile);

                return ResponseEntity
                        .internalServerError()
                        .body(
                                "Downloaded video is empty"
                                        .getBytes()
                        );
            }

            System.out.println(
                    "Video file created successfully."
            );

            System.out.println(
                    "Video size: "
                            + fileSize
                            + " bytes"
            );

            // Make quality safe for filename
            String safeQuality =
                    quality.replaceAll(
                            "[^a-zA-Z0-9_-]",
                            ""
                    );

            if (safeQuality.isBlank()) {
                safeQuality = "video";
            }

            String filename =
                    "pinterest-"
                            + pinId
                            + "-"
                            + safeQuality
                            + "-video.mp4";

            byte[] videoBytes;

            try {

                videoBytes =
                        Files.readAllBytes(
                                outputFile
                        );

            } finally {

                Files.deleteIfExists(
                        outputFile
                );
            }

            return ResponseEntity
                    .ok()
                    .header(
                            HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\""
                                    + filename
                                    + "\""
                    )
                    .contentType(
                            MediaType.valueOf(
                                    "video/mp4"
                            )
                    )
                    .body(videoBytes);

        } catch (Exception e) {

            e.printStackTrace();

            try {

                if (outputFile != null) {
                    Files.deleteIfExists(
                            outputFile
                    );
                }

            } catch (Exception ignored) {
            }

            return ResponseEntity
                    .internalServerError()
                    .body(
                            "Unable to download video"
                                    .getBytes()
                    );
        }
    }
}
