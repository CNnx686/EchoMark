package org.tongji.sse.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DeepSeekRequest {
    private List<Message> messages;
    private String model;
    
    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;
    
    @JsonProperty("max_tokens")
    private Integer maxTokens;
    
    @JsonProperty("presence_penalty")
    private Double presencePenalty;
    
    @JsonProperty("response_format")
    private ResponseFormat responseFormat;
    
    private Boolean stream;
    private Double temperature;
    
    @JsonProperty("top_p")
    private Double topP;
    
    private Boolean logprobs;
    
    @JsonProperty("top_logprobs")
    private Integer topLogprobs;
    
    private Thinking thinking;

    @JsonProperty("stream_options")
    private StreamOptions streamOptions;

    @Data
    @Builder
    public static class StreamOptions {
        @JsonProperty("include_usage")
        private Boolean includeUsage;
    }

    @Data
    @Builder
    public static class Thinking {
        private String type;
    }

    @Data
    @Builder
    public static class Message {
        private String content;
        private String role;
    }

    @Data
    @Builder
    public static class ResponseFormat {
        private String type;
    }
}
