package com.example.showyourvoice.domain.image;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@SpringBootTest
@AutoConfigureMockMvc
class ImageUploadIntegrationTests {

    private static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n', 0, 0, 0, 13, 'I', 'H', 'D', 'R'};
    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 36, 0, 0, 0, 'W', 'E', 'B', 'P', 'V', 'P', '8', ' '};

    @TempDir
    static Path uploadDir;

    @Autowired
    private MockMvc mvc;

    @DynamicPropertySource
    static void useTemporaryUploadDir(DynamicPropertyRegistry registry) {
        registry.add("image.upload-dir", () -> uploadDir.toString());
    }

    @Test
    void uploadsPostImageAndServesItOnlyToLoggedInUsers() throws Exception {
        String imageUrl = uploadAndGetUrl("/api/images/posts", file("photo.png", PNG), student());

        assertThat(imageUrl).matches("/images/posts/[0-9a-f-]{36}\\.png");
        assertThat(uploadDir.resolve(imageUrl.substring("/images/".length()))).hasBinaryContent(PNG);

        mvc.perform(get(imageUrl).with(student()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(PNG));
        mvc.perform(get(imageUrl))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void decidesExtensionFromFileContentNotFileName() throws Exception {
        assertThat(uploadAndGetUrl("/api/images/posts", file("photo.png", JPEG), student())).endsWith(".jpg");
        assertThat(uploadAndGetUrl("/api/images/posts", file("photo.jpg", WEBP), student())).endsWith(".webp");
    }

    @Test
    void rejectsFileThatIsNotAllowedImage() throws Exception {
        long before = countSavedFiles();

        mvc.perform(multipart("/api/images/posts").file(file("fake.png", "not an image".getBytes())).with(student()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("I001"))
                .andExpect(jsonPath("$.errors[0].field").value("file"));
        mvc.perform(multipart("/api/images/posts").file(file("empty.png", new byte[0])).with(student()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("I001"));

        assertThat(countSavedFiles()).isEqualTo(before);
    }

    @Test
    void rejectsImageLargerThanFiveMegabytes() throws Exception {
        byte[] large = new byte[5 * 1024 * 1024 + 1];
        System.arraycopy(PNG, 0, large, 0, PNG.length);

        mvc.perform(multipart("/api/images/posts").file(file("large.png", large)).with(student()))
                .andExpect(status().isContentTooLarge())
                .andExpect(jsonPath("$.code").value("I002"))
                .andExpect(jsonPath("$.errors[0].field").value("file"));
    }

    @Test
    void rejectsRequestWithoutFilePart() throws Exception {
        mvc.perform(multipart("/api/images/posts").with(student()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("file"));
    }

    @Test
    void rejectsUploadWhenSessionIsAbsent() throws Exception {
        mvc.perform(multipart("/api/images/posts").file(file("photo.png", PNG)))
                .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.code").value("A001"));
    }

    @Test
    void allowsPlaceImageUploadOnlyForTeachers() throws Exception {
        mvc.perform(multipart("/api/images/places").file(file("map.png", PNG)).with(student()))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("A002"));

        String imageUrl = uploadAndGetUrl("/api/images/places", file("map.png", PNG), teacher());
        assertThat(imageUrl).matches("/images/places/[0-9a-f-]{36}\\.png");
    }

    private String uploadAndGetUrl(String path, MockMultipartFile file, RequestPostProcessor user) throws Exception {
        String body = mvc.perform(multipart(path).file(file).with(user))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.imageUrl");
    }

    private static MockMultipartFile file(String name, byte[] bytes) {
        return new MockMultipartFile("file", name, "image/png", bytes);
    }

    private static RequestPostProcessor student() {
        return user("student").roles("USER");
    }

    private static RequestPostProcessor teacher() {
        return user("teacher").roles("TEACHER");
    }

    private static long countSavedFiles() throws Exception {
        try (Stream<Path> files = Files.walk(uploadDir)) {
            return files.filter(Files::isRegularFile).count();
        }
    }
}
