package org.tongji.sse.service;

import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;

public interface UserPersonaService {

    void handleEvent(UserBehaviorSignalEvent event);
}
