package com.gharse.common.storage;

import java.io.InputStream;

/** Object storage abstraction. Local disk today; an S3-compatible implementation can be added without touching callers. */
public interface StorageService {

    /** Stores the content under a generated key and returns that key. */
    String upload(String folder, String extension, String contentType, InputStream content, long length);

    void delete(String key);

    /** Public URL for a stored key. */
    String getUrl(String key);
}
