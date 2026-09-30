package com.example.mypindownloader.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class PinterestService {

    private final ObjectMapper objectMapper =
            new ObjectMapper();
    private final HttpClient httpClient =
            HttpClient.newBuilder()
                    .followRedirects(
                            HttpClient.Redirect.NORMAL
                    )
                    .connectTimeout(
                            java.time.Duration.ofSeconds(10)
                    )
                    .build();


    public String processUrl(String pinterestUrl) {

        if (pinterestUrl == null
                || pinterestUrl.isBlank()) {

            return "Invalid Pinterest URL";
        }

        pinterestUrl =
                pinterestUrl.trim();

        if (pinterestUrl.contains("/pin/")) {

            String pinId =
                    pinterestUrl.substring(
                            pinterestUrl.indexOf("/pin/") + 5
                    );

            pinId =
                    pinId.split("[/?]")[0];

            return "Pin ID: " + pinId;
        }


        if (pinterestUrl.contains("pin.it/")) {

            return "Pinterest short URL";
        }


        return "Invalid Pinterest URL";
    }

    private String resolvePinterestUrl(
            String pinterestUrl)
            throws Exception {

        pinterestUrl =
                pinterestUrl.trim();

        if (pinterestUrl.contains("pin.it/")) {

            HttpRequest redirectRequest =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(
                                            pinterestUrl
                                    )
                            )
                            .timeout(
                                    java.time.Duration.ofSeconds(10)
                            )
                            .GET()
                            .build();

            HttpResponse<String> redirectResponse;

            try {

                redirectResponse =
                        httpClient.send(
                                redirectRequest,
                                HttpResponse.BodyHandlers
                                        .ofString()
                        );

            } catch (java.net.http.HttpTimeoutException e) {

                System.out.println(
                        "Pinterest short URL resolution timed out."
                );

                return "";

            } catch (java.net.ConnectException e) {

                System.out.println(
                        "Could not connect to Pinterest."
                );

                return "";
            }

            if (redirectResponse.statusCode() >= 400) {

                System.out.println(
                        "Pinterest URL resolution failed. HTTP Status: "
                                + redirectResponse.statusCode()
                );

                return "";
            }

            pinterestUrl =
                    redirectResponse
                            .uri()
                            .toString();

            System.out.println(
                    "Redirected Pinterest URL:"
            );

            System.out.println(
                    pinterestUrl
            );
        }

        return pinterestUrl;
    }


    private String extractPinId(
            String pinterestUrl) {

        if (pinterestUrl == null
                || pinterestUrl.isBlank()) {

            return "";
        }

        if (!pinterestUrl.contains("/pin/")) {

            return "";
        }

        String pinId =
                pinterestUrl.substring(
                        pinterestUrl.indexOf("/pin/") + 5
                );

        pinId =
                pinId.split("[/?]")[0];

        if (!pinId.matches("\\d+")) {

            return "";
        }

        return pinId;
    }


    private JsonNode getPinData(
            String pinId) {
        if (pinId == null
                || pinId.isBlank()
                || !pinId.matches("\\d+")) {

            System.out.println(
                    "Invalid Pin ID: " + pinId
            );

            return null;
        }

        try {

            String url =
                    "https://www.pinterest.com/resource/PinResource/get/"
                            + "?source_url=%2Fpin%2F"
                            + pinId
                            + "%2F"
                            + "&data=%7B%22options%22%3A%7B%22id%22%3A%22"
                            + pinId
                            + "%22%2C%22field_set_key%22%3A%22detailed%22%7D"
                            + "%2C%22context%22%3A%7B%7D%7D"
                            + "&_="
                            + System.currentTimeMillis();


            HttpRequest request =
                    HttpRequest.newBuilder()
                            .uri(
                                    URI.create(url)
                            )
                            .timeout(
                                    java.time.Duration.ofSeconds(10)
                            )
                            .header(
                                    "X-Pinterest-PWS-Handler",
                                    "www/pin/[id].js"
                            )
                            .header(
                                    "Accept",
                                    "application/json, text/javascript, */*; q=0.01"
                            )
                            .GET()
                            .build();


            HttpResponse<String> response =
                    httpClient.send(
                            request,
                            HttpResponse.BodyHandlers
                                    .ofString()
                    );


            System.out.println(
                    "Pinterest API Status:"
            );

            System.out.println(
                    response.statusCode()
            );


            // -------------------------------------------------
            // Pin not found
            // -------------------------------------------------

            if (response.statusCode() == 404) {

                System.out.println(
                        "Pin not found: " + pinId
                );

                return null;
            }


            // -------------------------------------------------
            // Other HTTP errors
            // -------------------------------------------------

            if (response.statusCode() != 200) {

                System.out.println(
                        "Pinterest request failed."
                );

                System.out.println(
                        "HTTP Status: "
                                + response.statusCode()
                );

                if (response.statusCode() >= 500) {

                    System.out.println(
                            "Pinterest server error."
                    );

                } else if (response.statusCode() == 429) {

                    System.out.println(
                            "Pinterest rate limit reached."
                    );

                } else {

                    System.out.println(
                            "Pinterest returned an unexpected response."
                    );
                }

                return null;
            }


            // -------------------------------------------------
            // Parse JSON response
            // -------------------------------------------------
            String contentType =
                    response.headers()
                            .firstValue("Content-Type")
                            .orElse("");

            if (!contentType.toLowerCase().contains("application/json")) {

                System.out.println(
                        "Pinterest returned a non-JSON response."
                );

                System.out.println(
                        "Content-Type: " + contentType
                );

                return null;
            }
            if (response.body() == null
                    || response.body().isBlank()) {

                System.out.println(
                        "Pinterest returned an empty response."
                );

                return null;
            }
            JsonNode root;

            try {

                root =
                        objectMapper.readTree(
                                response.body()
                        );

            } catch (com.fasterxml.jackson.core.JsonProcessingException e) {

                System.out.println(
                        "Pinterest returned invalid JSON."
                );

                return null;
            }

            JsonNode resourceResponse =
                    root.path("resource_response");

            if (resourceResponse.isMissingNode()
                    || resourceResponse.isNull()) {

                System.out.println(
                        "Pinterest response does not contain resource_response."
                );

                return null;
            }

            JsonNode data =
                    resourceResponse.path("data");
            if (data.isMissingNode()
                    || data.isNull()) {

                System.out.println(
                        "Pinterest response does not contain Pin data."
                );

                return null;
            }

            if (!data.isObject()) {

                System.out.println(
                        "Pinterest Pin data has an unexpected format."
                );

                return null;
            }

            return data;


        } catch (java.net.http.HttpTimeoutException e) {

            System.out.println(
                    "Pinterest request timed out after 10 seconds."
            );

            return null;

        } catch (java.net.ConnectException e) {

            System.out.println(
                    "Could not connect to Pinterest."
            );

            return null;

        } catch (Exception e) {

            System.out.println(
                    "Unexpected error while retrieving Pin data."
            );

            e.printStackTrace();

            return null;
        }
    }


    private String convertQualityName(
            String qualityKey,
            String videoUrl) {

        if (qualityKey == null) {
            qualityKey = "";
        }

        if (videoUrl == null) {
            videoUrl = "";
        }

        if (qualityKey.startsWith("V_")) {

            String quality =
                    qualityKey.substring(2);

            if (quality.equalsIgnoreCase("HLSV4")
                    || quality.equalsIgnoreCase(
                    "HLSV3_MOBILE")) {

                return "HLS";
            }

            return quality.toLowerCase();
        }

        if (videoUrl.contains("/720p/")) {
            return "720p";
        }

        if (videoUrl.contains("/480p/")) {
            return "480p";
        }

        if (videoUrl.contains("/360p/")) {
            return "360p";
        }

        if (videoUrl.contains("/240p/")) {
            return "240p";
        }

        if (videoUrl.contains("_720w")) {
            return "720p";
        }

        if (videoUrl.contains("_480w")) {
            return "480p";
        }

        if (videoUrl.contains("_360w")) {
            return "360p";
        }

        if (videoUrl.contains("_240w")) {
            return "240p";
        }

        return qualityKey;
    }

    private int getQualityNumber(
            String quality) {

        if (quality == null
                || quality.isBlank()) {

            return 0;
        }

        try {

            return Integer.parseInt(
                    quality
                            .toLowerCase()
                            .replace("p", "")
                            .trim()
            );

        } catch (NumberFormatException e) {

            return 0;
        }
    }

    public String getPinId(
            String pinterestUrl) {

        try {

            if (pinterestUrl == null
                    || pinterestUrl.isBlank()) {

                return "";
            }

            pinterestUrl =
                    resolvePinterestUrl(
                            pinterestUrl
                    );

            if (pinterestUrl == null
                    || pinterestUrl.isBlank()) {

                System.out.println(
                        "Unable to resolve Pinterest URL."
                );

                return "";
            }

            String pinId =
                    extractPinId(
                            pinterestUrl
                    );

            if (pinId == null
                    || pinId.isBlank()
                    || !pinId.matches("\\d+")) {

                System.out.println(
                        "Invalid Pin ID: "
                                + pinId
                );

                return "";
            }

            return pinId;

        } catch (Exception e) {

            e.printStackTrace();

            return "";
        }
    }


    public Map<String, Object> getMediaData(
            String pinterestUrl) {

        Map<String, Object> result =
                new java.util.HashMap<>();

        try {

            // -------------------------------------------------
            // Resolve Pinterest URL
            // -------------------------------------------------

            pinterestUrl =
                    resolvePinterestUrl(
                            pinterestUrl
                    );

            if (pinterestUrl == null
                    || pinterestUrl.isBlank()) {

                System.out.println(
                        "Unable to resolve Pinterest URL."
                );

                return result;
            }


            // -------------------------------------------------
            // Extract Pin ID
            // -------------------------------------------------

            String pinId =
                    extractPinId(
                            pinterestUrl
                    );

            if (pinId == null
                    || pinId.isBlank()
                    || !pinId.matches("\\d+")) {

                System.out.println(
                        "Invalid Pinterest Pin ID: "
                                + pinId
                );

                return result;
            }


            // -------------------------------------------------
            // Get Pin data
            // -------------------------------------------------

            JsonNode data =
                    getPinData(
                            pinId
                    );

            if (data == null
                    || data.isMissingNode()
                    || data.isNull()) {

                System.out.println(
                        "No Pin data found for Pin ID: "
                                + pinId
                );

                return Map.of(
                        "mediaUrl", "",
                        "videoQualities", List.of(),
                        "pinId", pinId
                );
            }


            String mediaUrl = "";

            List<Map<String, String>>
                    qualities =
                    new ArrayList<>();


            // -------------------------------------------------
            // Check videos
            // -------------------------------------------------

            JsonNode videos =
                    data.path("videos");

            System.out.println(
                    "Checking video formats..."
            );

            if (!videos.isMissingNode()
                    && !videos.isNull()
                    && videos.isObject()) {

                JsonNode videoList =
                        videos.path(
                                "video_list"
                        );

                System.out.println(
                        "Video list: "
                                + videoList
                );


                // -------------------------------------------------
                // Validate video_list
                // -------------------------------------------------

                if (videoList.isMissingNode()
                        || videoList.isNull()
                        || !videoList.isObject()) {

                    System.out.println(
                            "Pin does not contain a valid video list."
                    );

                } else {


                    // -------------------------------------------------
                    // Find all available MP4 videos
                    // -------------------------------------------------

                    videoList
                            .fields()
                            .forEachRemaining(
                                    entry -> {

                                        String qualityKey =
                                                entry.getKey();

                                        JsonNode video =
                                                entry.getValue();

                                        String videoUrl =
                                                video.path(
                                                        "url"
                                                ).asText("");

                                        System.out.println(
                                                "Found video: "
                                                        + qualityKey
                                                        + " -> "
                                                        + videoUrl
                                        );


                                        // -------------------------------------------------
                                        // Check MP4
                                        // -------------------------------------------------

                                        if (!videoUrl.isBlank()
                                                && (videoUrl.startsWith("https://")
                                                || videoUrl.startsWith("http://"))
                                                && videoUrl
                                                .toLowerCase()
                                                .contains(".mp4")) {

                                            String quality =
                                                    convertQualityName(
                                                            qualityKey,
                                                            videoUrl
                                                    );


                                            // -------------------------------------------------
                                            // Ignore HLS
                                            // -------------------------------------------------

                                            if (!quality
                                                    .equalsIgnoreCase(
                                                            "HLS"
                                                    )) {

                                                qualities.add(
                                                        Map.of(
                                                                "quality",
                                                                quality,

                                                                "key",
                                                                qualityKey,

                                                                "url",
                                                                videoUrl,

                                                                "width",
                                                                String.valueOf(
                                                                        video.path(
                                                                                "width"
                                                                        ).asInt()
                                                                ),

                                                                "height",
                                                                String.valueOf(
                                                                        video.path(
                                                                                "height"
                                                                        ).asInt()
                                                                )
                                                        )
                                                );
                                            }
                                        }
                                    }
                            );


                    // -------------------------------------------------
                    // Sort highest quality first
                    // -------------------------------------------------

                    qualities.sort(
                            (a, b) -> {

                                int qualityA =
                                        getQualityNumber(
                                                a.get("quality")
                                        );

                                int qualityB =
                                        getQualityNumber(
                                                b.get("quality")
                                        );

                                return Integer.compare(
                                        qualityB,
                                        qualityA
                                );
                            }
                    );


                    // -------------------------------------------------
                    // Select highest available MP4
                    // -------------------------------------------------

                    if (!qualities.isEmpty()) {

                        String selectedVideoUrl =
                                qualities
                                        .get(0)
                                        .get("url");

                        if (selectedVideoUrl != null
                                && !selectedVideoUrl.isBlank()
                                && (selectedVideoUrl.startsWith("https://")
                                || selectedVideoUrl.startsWith("http://"))) {

                            mediaUrl = selectedVideoUrl;

                            System.out.println(
                                    "Selected video: "
                                            + qualities
                                            .get(0)
                                            .get("quality")
                            );

                        } else {

                            System.out.println(
                                    "Selected video URL is invalid."
                            );
                        }
                    }


                    // -------------------------------------------------
                    // HLS-only video
                    // -------------------------------------------------

                    if (qualities.isEmpty()
                            && videoList.size() > 0) {

                        System.out.println(
                                "HLS-ONLY VIDEO FOUND"
                        );
                    }
                }

            } else {

                System.out.println(
                        "Pin does not contain valid video data."
                );
            }


            // -------------------------------------------------
            // If no video found, check original image
            // -------------------------------------------------

            if (mediaUrl.isEmpty()) {

                JsonNode images =
                        data.path("images");

                if (images.isMissingNode()
                        || images.isNull()
                        || !images.isObject()) {

                    System.out.println(
                            "Pin does not contain valid image data."
                    );

                } else {

                    JsonNode original =
                            images.path("orig");

                    if (original.isMissingNode()
                            || original.isNull()
                            || !original.isObject()) {

                        System.out.println(
                                "Original image data not found."
                        );

                    } else {

                        String imageUrl =
                                original.path("url")
                                        .asText("");

                        if (!imageUrl.isBlank()
                                && (imageUrl.startsWith("https://")
                                || imageUrl.startsWith("http://"))) {

                            mediaUrl = imageUrl;

                            System.out.println(
                                    "Original image found: "
                                            + imageUrl
                            );
                        }
                    }
                }
            }


            // -------------------------------------------------
            // Prepare result
            // -------------------------------------------------
            if (!mediaUrl.isBlank()
                    && !(mediaUrl.startsWith("https://")
                    || mediaUrl.startsWith("http://"))) {

                System.out.println(
                        "Final media URL is invalid."
                );

                mediaUrl = "";
            }
            result.put(
                    "mediaUrl",
                    mediaUrl
            );

            result.put(
                    "videoQualities",
                    qualities
            );

            result.put(
                    "pinId",
                    pinId
            );


            return result;


        } catch (Exception e) {

            System.out.println(
                    "Unexpected error while processing media data."
            );

            e.printStackTrace();

            return result;
        }
    }
}