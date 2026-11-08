package com.example.showyourvoice.domain.image.service;

import com.example.showyourvoice.domain.image.controller.dto.ImageUploadResponse;
import com.example.showyourvoice.domain.image.domain.ImagePurpose;
import com.example.showyourvoice.domain.image.domain.ImageType;
import com.example.showyourvoice.domain.image.exception.ImageNotFoundException;
import com.example.showyourvoice.domain.image.exception.ImageTooLargeException;
import com.example.showyourvoice.domain.image.exception.ImageUploadFailedException;
import com.example.showyourvoice.domain.image.exception.InvalidImageException;
import com.example.showyourvoice.global.config.ImageProperties;
import com.example.showyourvoice.global.config.WebConfig;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImageService {

    public static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    // upload()가 만드는 파일 이름 형식 (UUID.확장자)
    private static final Pattern SAVED_FILE_NAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(jpg|png|webp)");

    private final ImageProperties imageProperties;

    public ImageUploadResponse upload(MultipartFile file, ImagePurpose purpose) {
        if (file.isEmpty()) {
            throw new InvalidImageException();
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new ImageTooLargeException();
        }

        ImageType type = ImageType.detect(readHeader(file)).orElseThrow(InvalidImageException::new);
        // 원본 파일 이름은 쓰지 않고, 추측할 수 없는 이름으로 저장한다.
        String fileName = UUID.randomUUID() + "." + type.getExtension();
        save(file, purpose, fileName);

        return new ImageUploadResponse(WebConfig.IMAGE_URL_PREFIX + purpose.getDirectory() + "/" + fileName);
    }

    private byte[] readHeader(MultipartFile file) {
        try (InputStream input = file.getInputStream()) {
            return input.readNBytes(ImageType.HEADER_LENGTH);
        } catch (IOException e) {
            throw new InvalidImageException();
        }
    }

    // 우리 서버가 돌려준 주소이고, 실제로 저장된 파일이 있는지 확인한다.
    public void verifyUploaded(String imageUrl, ImagePurpose purpose) {
        String prefix = WebConfig.IMAGE_URL_PREFIX + purpose.getDirectory() + "/";
        String fileName = imageUrl.startsWith(prefix) ? imageUrl.substring(prefix.length()) : "";
        if (!SAVED_FILE_NAME.matcher(fileName).matches() || !Files.isRegularFile(directoryOf(purpose).resolve(fileName))) {
            throw new ImageNotFoundException();
        }
    }

    private Path directoryOf(ImagePurpose purpose) {
        return imageProperties.resolvedUploadDir().resolve(purpose.getDirectory());
    }

    private void save(MultipartFile file, ImagePurpose purpose, String fileName) {
        Path directory = directoryOf(purpose);
        try (InputStream input = file.getInputStream()) {
            Files.createDirectories(directory);
            Files.copy(input, directory.resolve(fileName));
        } catch (IOException e) {
            throw new ImageUploadFailedException();
        }
    }
}
