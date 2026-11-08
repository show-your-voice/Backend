package com.example.showyourvoice.domain.post;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.showyourvoice.global.security.CustomUserDetails;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

class PostImageLinkIntegrationTests extends PostApiTestSupport {

    @TempDir
    static Path uploadDir;

    @DynamicPropertySource
    static void useTemporaryUploadDir(DynamicPropertyRegistry registry) {
        registry.add("image.upload-dir", () -> uploadDir.toString());
    }

    @Test
    void linksUploadedImagesInRequestOrder() throws Exception {
        String first = uploadedImage("posts", "png");
        String second = uploadedImage("posts", "jpg");

        long id = responseId(mvc.perform(createRequest(Map.of("imageUrls", List.of(first, second))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrls[0]").value(first))
                .andExpect(jsonPath("$.imageUrls[1]").value(second))
                .andReturn());

        mvc.perform(get("/api/posts/{id}", id).with(user(CustomUserDetails.from(otherUser))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imageUrls[0]").value(first))
                .andExpect(jsonPath("$.imageUrls[1]").value(second));
        assertThat(jdbc.queryForList("SELECT image_url FROM tbl_post_image WHERE post_id = ? ORDER BY sort_order",
                String.class, id)).containsExactly(first, second);
    }

    @Test
    void createsPostWithoutImagesWhenImageUrlsAreAbsent() throws Exception {
        mvc.perform(createRequest(Collections.singletonMap("imageUrls", null)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrls").isEmpty());
        mvc.perform(createRequest(Map.of("imageUrls", List.of())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.imageUrls").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "/images/posts/3f1c2a9e-7b4d-4e8a-9c21-7d4e5f6a8b90.png",
            "https://example.com/photo.png",
            "/images/posts/../places/map.png",
            "/images/posts/photo.png"
    })
    void rejectsImageUrlThatWasNotUploaded(String imageUrl) throws Exception {
        long before = postRepository.count();

        mvc.perform(createRequest(Map.of("imageUrls", List.of(imageUrl))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("I004"))
                .andExpect(jsonPath("$.errors[0].field").value("imageUrls"));

        assertThat(postRepository.count()).isEqualTo(before);
    }

    @Test
    void rejectsPlaceImageAsPostImage() throws Exception {
        String placeImage = uploadedImage("places", "png");

        mvc.perform(createRequest(Map.of("imageUrls", List.of(placeImage))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("I004"));
    }

    @Test
    void rejectsMoreThanFiveImages() throws Exception {
        List<String> imageUrls = IntStream.range(0, 6).mapToObj(i -> uploadedImage("posts", "png")).toList();

        mvc.perform(createRequest(Map.of("imageUrls", imageUrls)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("C001"))
                .andExpect(jsonPath("$.errors[0].field").value("imageUrls"));
    }

    private static String uploadedImage(String directory, String extension) {
        String fileName = UUID.randomUUID() + "." + extension;
        try {
            Files.createDirectories(uploadDir.resolve(directory));
            Files.write(uploadDir.resolve(directory).resolve(fileName), new byte[]{1});
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        return "/images/" + directory + "/" + fileName;
    }
}
