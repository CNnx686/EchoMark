package org.tongji.sse.service;

import org.tongji.sse.domain.model.UserPersona;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;

public interface UserPersonaUpdateService {

    void update(UserBehaviorSignalEvent event, UserPersona persona);
}
