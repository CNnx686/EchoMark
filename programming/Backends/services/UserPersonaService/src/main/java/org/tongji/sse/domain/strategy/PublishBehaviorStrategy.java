package org.tongji.sse.domain.strategy;

import org.tongji.sse.domain.model.UserPersona;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;
import org.tongji.sse.util.KeywordExtractor;

public class PublishBehaviorStrategy extends AbstractWeightStrategy
        implements BehaviorUpdateStrategy {

    @Override
    public boolean supports(String behaviorType) {
        return "PUBLISH".equals(behaviorType);
    }

    @Override
    public void apply(UserBehaviorSignalEvent event, UserPersona persona) {
        if (event.getTags() != null) {
            event.getTags().forEach(tag ->
                    increaseAdaptive(persona.getTagWeights(), tag, 1.5, 10.0)
            );
        }

        // 新增：简介关键词
        KeywordExtractor.extract(event.getTextContent())
                .forEach(k -> increaseAdaptive(persona.getKeywordWeights(), k, 2.0, 10.0));
    }
}