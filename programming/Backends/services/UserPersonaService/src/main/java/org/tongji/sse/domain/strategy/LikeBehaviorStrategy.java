package org.tongji.sse.domain.strategy;

import org.tongji.sse.domain.model.UserPersona;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;

public class LikeBehaviorStrategy extends AbstractWeightStrategy
        implements BehaviorUpdateStrategy {

    @Override
    public boolean supports(String behaviorType) {
        return "LIKE".equals(behaviorType);
    }

    @Override
    public void apply(UserBehaviorSignalEvent event, UserPersona persona) {
        if (event.getTags() != null) {
            event.getTags().forEach(tag ->
                    increaseAdaptive(persona.getTagWeights(), tag, 2.0, 10.0)
            );
        }
        increaseAdaptive(persona.getCategoryWeights(), event.getCategory(), 2.0, 10.0);
        increaseLongAdaptive(persona.getAuthorWeights(), event.getAuthorId(), 1.5, 10.0);
    }
}
