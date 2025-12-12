package org.hmmk.sms.dto.apikey;

import lombok.Builder;
import lombok.Data;
import org.hmmk.sms.entity.ApiKey;

import java.time.Instant;

@Data
@Builder
public class ApiKeyResponse {
    private String id;
    private String senderId;
    private String senderName;
    private String apiKey;
    private String name;
    private Instant createdAt;

    public static ApiKeyResponse fromEntity(ApiKey entity) {
        return ApiKeyResponse.builder()
                .id(entity.id)
                .senderId(entity.sender.id)
                .senderName(entity.sender.name)
                .apiKey(entity.apiKey)
                .name(entity.name)
                .createdAt(entity.createdAt)
                .build();
    }
}
