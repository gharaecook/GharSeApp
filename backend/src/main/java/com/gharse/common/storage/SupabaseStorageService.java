package com.gharse.common.storage;

import com.gharse.common.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

@Service
@ConditionalOnProperty(prefix = "gharse.storage", name = "type", havingValue = "supabase")
public class SupabaseStorageService implements StorageService {

    private static final String KEY_PATTERN = "^(profiles|dishes|kitchens|documents)/[0-9a-fA-F-]{36}\\.(jpg|png|webp|pdf)$";
    private final RestClient client;
    private final String baseUrl;
    private final String serviceKey;
    private final String publicBucket;
    private final String privateBucket;

    public SupabaseStorageService(AppProperties props) {
        AppProperties.Storage storage = props.storage();
        if (storage.supabaseUrl() == null || storage.supabaseUrl().isBlank()
                || storage.supabaseServiceKey() == null || storage.supabaseServiceKey().isBlank()) {
            throw new IllegalStateException("SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are required when STORAGE_TYPE=supabase");
        }
        this.baseUrl = storage.supabaseUrl().replaceAll("/+$", "");
        this.serviceKey = storage.supabaseServiceKey();
        this.publicBucket = storage.publicBucket();
        this.privateBucket = storage.privateBucket();
        this.client = RestClient.builder().baseUrl(baseUrl).build();
    }

    @Override
    public String upload(String folder, String extension, String contentType, InputStream content, long length) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        String bucket = folder.equals("documents") ? privateBucket : publicBucket;
        client.post()
                .uri(objectPath(bucket, key))
                .header("apikey", serviceKey)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header("x-upsert", "false")
                .body(content.readAllBytes())
                .retrieve()
                .toBodilessEntity();
        return key;
    }

    @Override
    public void delete(String key) {
        validateKey(key);
        client.delete()
                .uri(objectPath(bucketFor(key), key))
                .header("apikey", serviceKey)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public String getUrl(String key) {
        validateKey(key);
        if (key.startsWith("documents/")) {
            throw new IllegalArgumentException("Private documents do not have public URLs");
        }
        return baseUrl + "/storage/v1/object/public/" + encode(publicBucket) + "/" + encodePath(key);
    }

    @Override
    public String getPrivateUrl(String key, Duration expiry) {
        validateKey(key);
        if (!key.startsWith("documents/")) {
            throw new IllegalArgumentException("Only documents use private URLs");
        }
        SignedUrl response = client.post()
                .uri("/storage/v1/object/sign/" + encode(privateBucket) + "/" + encodePath(key))
                .header("apikey", serviceKey)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + serviceKey)
                .body(Map.of("expiresIn", Math.toIntExact(expiry.toSeconds())))
                .retrieve()
                .body(SignedUrl.class);
        if (response == null || response.signedURL() == null || response.signedURL().isBlank()) {
            throw new IllegalStateException("Supabase did not return a signed document URL");
        }
        String path = response.signedURL();
        if (path.startsWith("http://") || path.startsWith("https://")) return path;
        return baseUrl + (path.startsWith("/storage/v1/") ? path : "/storage/v1" + (path.startsWith("/") ? path : "/" + path));
    }

    private String bucketFor(String key) {
        return key.startsWith("documents/") ? privateBucket : publicBucket;
    }

    private static String objectPath(String bucket, String key) {
        return "/storage/v1/object/" + encode(bucket) + "/" + encodePath(key);
    }

    private static String encode(String value) {
        return UriUtils.encodePathSegment(value, StandardCharsets.UTF_8);
    }

    private static String encodePath(String value) {
        validateKey(value);
        return UriUtils.encodePath(value, StandardCharsets.UTF_8);
    }

    private static void validateKey(String key) {
        if (key == null || !key.matches(KEY_PATTERN)) {
            throw new IllegalArgumentException("Invalid storage key");
        }
    }

    private record SignedUrl(String signedURL) {}
}
