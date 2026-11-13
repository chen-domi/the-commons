package com.thecommons.backend.nlp;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import java.util.function.Consumer;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.services.bedrockruntime.BedrockRuntimeClient;
import software.amazon.awssdk.services.bedrockruntime.model.*;

@SuppressWarnings("unchecked")
class BedrockItemDescriptionModelTest {
    private final BedrockRuntimeClient client = mock(BedrockRuntimeClient.class);

    private BedrockItemDescriptionModel model(boolean enabled) {
        var model = new BedrockItemDescriptionModel(enabled, "us-east-1", "test-model");
        ReflectionTestUtils.setField(model, "client", client);
        return model;
    }

    @Test void disabledModeNeverCallsBedrock() {
        assertThrows(NlpException.class, () -> model(false).extract("chairs"));
        verifyNoInteractions(client);
        assertFalse(new BedrockItemDescriptionModel(true, "us-east-1", "").isAvailable());
    }

    @Test void buildsBoundedConverseRequestWithUntrustedTextInUserMessage() {
        when(client.converse(any(Consumer.class))).thenAnswer(invocation -> {
            var builder = ConverseRequest.builder();
            ((Consumer<ConverseRequest.Builder>) invocation.getArgument(0)).accept(builder);
            var request = builder.build();
            assertEquals("test-model", request.modelId());
            assertEquals(512, request.inferenceConfig().maxTokens());
            assertEquals(0.0f, request.inferenceConfig().temperature());
            assertEquals(ConversationRole.USER, request.messages().getFirst().role());
            assertEquals("12 chairs", request.messages().getFirst().content().getFirst().text());
            assertTrue(request.system().getFirst().text().contains("untrusted data"));
            return ConverseResponse.builder().stopReason(StopReason.END_TURN)
                    .output(output -> output.message(Message.builder().role(ConversationRole.ASSISTANT)
                            .content(ContentBlock.fromText("{\"name\":\"Chairs\"}")).build())).build();
        });
        assertEquals("{\"name\":\"Chairs\"}", model(true).extract("12 chairs"));
    }

    @Test void truncatedResponseIsNotUsed() {
        when(client.converse(any(Consumer.class))).thenReturn(ConverseResponse.builder()
                .stopReason(StopReason.MAX_TOKENS).build());
        assertEquals(502, assertThrows(NlpException.class, () -> model(true).extract("chairs")).status().value());
    }

    @Test void providerAndCredentialErrorsAreRedacted() {
        when(client.converse(any(Consumer.class))).thenThrow(SdkClientException.create("SECRET_INTERNAL_PROVIDER_DETAILS"));
        var error = assertThrows(NlpException.class, () -> model(true).extract("chairs"));
        assertEquals(503, error.status().value());
        assertFalse(error.getMessage().contains("SECRET"));
    }
}
