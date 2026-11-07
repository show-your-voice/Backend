package com.example.showyourvoice.domain.image.domain;

import java.util.Arrays;
import java.util.Optional;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ImageType {
    JPEG("jpg"),
    PNG("png"),
    WEBP("webp");

    // 판별에 필요한 파일 앞부분 길이 (WEBP: "RIFF" + 크기 4바이트 + "WEBP")
    public static final int HEADER_LENGTH = 12;

    private static final byte[] JPEG_SIGNATURE = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final byte[] RIFF_SIGNATURE = {'R', 'I', 'F', 'F'};
    private static final byte[] WEBP_SIGNATURE = {'W', 'E', 'B', 'P'};

    private final String extension;

    // 확장자나 Content-Type은 바꿔 보낼 수 있으므로 파일 앞부분의 시그니처로 형식을 판별한다.
    public static Optional<ImageType> detect(byte[] header) {
        if (startsWith(header, 0, JPEG_SIGNATURE)) {
            return Optional.of(JPEG);
        }
        if (startsWith(header, 0, PNG_SIGNATURE)) {
            return Optional.of(PNG);
        }
        if (startsWith(header, 0, RIFF_SIGNATURE) && startsWith(header, 8, WEBP_SIGNATURE)) {
            return Optional.of(WEBP);
        }
        return Optional.empty();
    }

    private static boolean startsWith(byte[] header, int offset, byte[] signature) {
        return header.length >= offset + signature.length
                && Arrays.equals(header, offset, offset + signature.length, signature, 0, signature.length);
    }
}
