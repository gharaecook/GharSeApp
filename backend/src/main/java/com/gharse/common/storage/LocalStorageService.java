package com.gharse.common.storage;

import com.gharse.common.AppProperties;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(prefix = "gharse.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalStorageService implements StorageService {

    private final Path root;
    private final String baseUrl;

    public LocalStorageService(AppProperties props) {
        this.root = Path.of(props.storage().localDir()).toAbsolutePath().normalize();
        this.baseUrl = props.storage().publicBaseUrl().replaceAll("/+$", "");
    }

    public Path root() {
        return root;
    }

    @Override
    public String upload(String folder, String extension, String contentType, InputStream content, long length) throws IOException {
        String key = folder + "/" + UUID.randomUUID() + "." + extension;
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) throw new IllegalArgumentException("Invalid storage key");
        try {
            Files.createDirectories(target.getParent());
            Files.copy(content, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return key;
    }

    @Override
    public void delete(String key) {
        Path target = root.resolve(key).normalize();
        if (!target.startsWith(root)) return;
        try {
            Files.deleteIfExists(target);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String getUrl(String key) {
        return baseUrl + "/" + key;
    }

    @Override
    public String getPrivateUrl(String key, Duration expiry) {
        return getUrl(key);
    }
}
