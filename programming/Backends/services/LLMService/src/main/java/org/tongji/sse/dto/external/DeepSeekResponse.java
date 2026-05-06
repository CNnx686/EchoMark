package org.tongji.sse.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
public class DeepSeekResponse {
    private String id;
    private List<Choice> choices;
    private Long created;
    private String model;
    private Usage usage;

    @Data
    @NoArgsConstructor
    public static class Choice {
        @JsonProperty("finish_reason")
        private String finishReason;
        
        private int index;
        private Message message;
        private Message delta; // For streaming
    }

    @Data
    @NoArgsConstructor
    public static class Message {
        private String content;
        
        @JsonProperty("reasoning_content")
        private String reasoningContent;
        
        private String role;
    }

    @Data
    @NoArgsConstructor
    public static class Usage {
        @JsonProperty("completion_tokens")
        private int completionTokens;
        
        @JsonProperty("prompt_tokens")
        private int promptTokens;
        
        @JsonProperty("total_tokens")
        private int totalTokens;
    }
}
