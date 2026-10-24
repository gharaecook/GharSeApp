package com.gharse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class FileUploadTest extends IntegrationTestBase {

    private int upload(String token, String name, String type, byte[] bytes) throws Exception {
        var req = multipart("/api/v1/files").file(new MockMultipartFile("file", name, type, bytes));
        if (token != null) req.header("Authorization", "Bearer " + token);
        return mvc.perform(req).andReturn().getResponse().getStatus();
    }

    @Test
    void validatesAuthTypeAndContent() throws Exception {
        Acct c = registerCustomer();
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0};
        assertThat(upload(null, "a.png", "image/png", png)).isEqualTo(401);
        assertThat(upload(c.token(), "a.png", "image/png", png)).isEqualTo(200);
        assertThat(upload(c.token(), "a.exe", "application/octet-stream", png)).isEqualTo(400);
        assertThat(upload(c.token(), "fake.png", "image/png", "not an image at all".getBytes())).isEqualTo(400);
        assertThat(upload(c.token(), "empty.png", "image/png", new byte[0])).isEqualTo(400);
    }

    @Test
    void documentsReturnPrivateKeysInsteadOfPublicUrls() throws Exception {
        Acct c = registerCustomer();
        byte[] pdf = "%PDF-1.7 test".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        var req = multipart("/api/v1/files").file(new MockMultipartFile("file", "certificate.pdf", "application/pdf", pdf))
                .param("folder", "documents").header("Authorization", "Bearer " + c.token());
        var response = mvc.perform(req).andExpect(status().isOk()).andReturn().getResponse();
        var body = json.readTree(response.getContentAsString());

        assertThat(body.path("key").asText()).startsWith("documents/");
        assertThat(body.has("url")).isFalse();
    }
}
