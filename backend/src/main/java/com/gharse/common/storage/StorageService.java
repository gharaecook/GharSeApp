package com.gharse.common.storage;

import java.io.InputStream;
import java.time.Duration;

/** Object storage abstraction. Local disk today; an S3-compatible implementation can be added without touching callers. */
public interface StorageService {

    /** Stores the content under a generated key and returns that key. */
    String upload(String folder, String extension, String contentType, InputStream content, long length) throws java.io.IOException;

    void delete(String key);

    /** Public URL for a stored key. */
    String getUrl(String key);

    /** Temporary access URL for a private key. Local development may serve it from local disk. */
    String getPrivateUrl(String key, Duration expiry);
}
