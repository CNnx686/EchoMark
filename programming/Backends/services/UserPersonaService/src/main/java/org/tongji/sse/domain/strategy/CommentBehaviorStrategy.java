package org.tongji.sse.domain.strategy;

import org.tongji.sse.domain.model.UserPersona;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;
import org.tongji.sse.util.KeywordExtractor;

public class CommentBehaviorStrategy extends AbstractWeightStrategy
        implements BehaviorUpdateStrategy {

    @Override
    public boolean supports(String behaviorType) {
        return "COMMENT".equals(behaviorType);
    }

    @Override
    public void apply(UserBehaviorSignalEvent event, UserPersona persona) {
        if (event.getTags() != null) {
            event.getTags().forEach(tag ->
                    increaseAdaptive(persona.getTagWeights(), tag, 2.0, 10.0)
            );
        }

        increaseLongAdaptive(persona.getAuthorWeights(), event.getAuthorId(), 2.0, 10.0);

        // ③ keyword 权重 + 弱绑定
        KeywordExtractor.extract(event.getTextContent())
                .forEach(keyword -> {

                    // keyword 本身
                    increaseAdaptive(
                            persona.getKeywordWeights(), keyword, 1.5, 10.0
                    );

                    // ⭐ 弱绑定：keyword -> tag
                    if (persona.getTagWeights().containsKey(keyword)) {
                        increaseAdaptive(
                                persona.getTagWeights(), keyword, 0.3, 10.0
                        );
                    }
                });
    }
}
