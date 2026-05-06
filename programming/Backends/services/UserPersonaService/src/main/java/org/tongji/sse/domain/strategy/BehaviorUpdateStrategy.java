package org.tongji.sse.domain.strategy;

import org.tongji.sse.domain.model.UserPersona;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;

public interface BehaviorUpdateStrategy {

    boolean supports(String behaviorType);

    void apply(UserBehaviorSignalEvent event, UserPersona persona);
}
