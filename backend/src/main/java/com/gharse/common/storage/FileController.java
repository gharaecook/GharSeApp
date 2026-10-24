package com.gharse.common.storage;

import com.gharse.common.ApiException;
import com.gharse.common.AppProperties;
import java.io.IOException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    public record UploadResponse(String key, String url) {}

    private static final Map<String, String> TYPES = Map.of(
            "image/jpeg", "jpg", "image/png", "png", "image/webp", "webp", "application/pdf", "pdf");
    private static final Set<String> FOLDERS = Set.of("profiles", "dishes", "kitchens", "documents");

    private final StorageService storage;
    private final long maxBytes;

    public FileController(StorageService storage, AppProperties props) {
        this.storage = storage;
        this.maxBytes = props.storage().maxFileBytes();
    }

    /** Upload media and return a public image URL or a private document key. */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public UploadResponse upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(defaultValue = "dishes") String folder) throws IOException {
        if (file.isEmpty()) throw ApiException.badRequest("FILE_EMPTY", "File is empty");
        if (file.getSize() > maxBytes) throw ApiException.badRequest("FILE_TOO_LARGE", "File exceeds the maximum size");
        String type = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String ext = TYPES.get(type);
        if (ext == null) throw ApiException.badRequest("FILE_TYPE_NOT_ALLOWED", "Only JPEG, PNG, WebP or PDF files are allowed");
        if (!FOLDERS.contains(folder)) throw ApiException.badRequest("FOLDER_NOT_ALLOWED", "Unknown folder");
        byte[] head;
        try (var in = file.getInputStream()) {
            head = in.readNBytes(8);
        }
        if (!matchesMagic(ext, head)) throw ApiException.badRequest("FILE_TYPE_NOT_ALLOWED", "File content does not match its type");
        String key;
        try (var in = file.getInputStream()) {
            key = storage.upload(folder, ext, type, in, file.getSize());
        }
        return new UploadResponse(key, folder.equals("documents") ? null : storage.getUrl(key));
    }

    private static boolean matchesMagic(String ext, byte[] h) {
        if (h.length < 4) return false;
        return switch (ext) {
            case "jpg" -> (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8;
            case "png" -> (h[0] & 0xFF) == 0x89 && h[1] == 'P' && h[2] == 'N' && h[3] == 'G';
            case "webp" -> h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F';
            case "pdf" -> h[0] == '%' && h[1] == 'P' && h[2] == 'D' && h[3] == 'F';
            default -> false;
        };
    }
}
