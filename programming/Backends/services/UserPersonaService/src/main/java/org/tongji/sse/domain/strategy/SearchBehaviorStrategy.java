package org.tongji.sse.domain.strategy;

import org.tongji.sse.domain.model.UserPersona;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;
import org.tongji.sse.util.KeywordExtractor;

public class SearchBehaviorStrategy extends AbstractWeightStrategy
        implements BehaviorUpdateStrategy {

    @Override
    public boolean supports(String behaviorType) {
        return "SEARCH".equals(behaviorType);
    }

    @Override
    public void apply(UserBehaviorSignalEvent event, UserPersona persona) {
        increaseAdaptive(persona.getTagWeights(), event.getKeyword(), 1.0, 10.0);

        // 新增：关键词
        KeywordExtractor.extract(event.getKeyword())
                .forEach(k -> increaseAdaptive(persona.getKeywordWeights(), k, 1.2, 10.0));
    }

}
