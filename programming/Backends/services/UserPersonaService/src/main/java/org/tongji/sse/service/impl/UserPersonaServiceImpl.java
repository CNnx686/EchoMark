package org.tongji.sse.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.tongji.sse.domain.model.UserPersona;
import org.tongji.sse.entity.UserPersonaEntity;
import org.tongji.sse.eventUtil.event.UserBehaviorSignalEvent;
import org.tongji.sse.repository.UserPersonaRepository;
import org.tongji.sse.service.UserPersonaDecayService;
import org.tongji.sse.service.UserPersonaService;
import org.tongji.sse.service.UserPersonaUpdateService;

import java.time.Instant;

@Service
public class UserPersonaServiceImpl implements UserPersonaService {

    private final UserPersonaRepository repository;
    private final UserPersonaUpdateService updateService;
    private final UserPersonaDecayService decayService;
    private final ObjectMapper mapper = new ObjectMapper();

    public UserPersonaServiceImpl(
            UserPersonaRepository repository,
            UserPersonaUpdateService updateService,
            UserPersonaDecayService decayService
    ) {
        this.repository = repository;
        this.updateService = updateService;
        this.decayService = decayService;
    }

    @Override
    public void handleEvent(UserBehaviorSignalEvent event) {
        UserPersonaEntity entity = repository
                .findById(event.getUserId())
                .orElseGet(() -> {
                    UserPersonaEntity e = new UserPersonaEntity();
                    e.setUserId(event.getUserId());
                    e.setVersion(1);
                    e.setUpdatedAt(Instant.now());
                    return e;
                });

        UserPersona persona;
        try {
            persona = entity.getPersonaJson() == null
                    ? new UserPersona()
                    : mapper.readValue(entity.getPersonaJson(), UserPersona.class);
        } catch (Exception e) {
            persona = new UserPersona();
        }

        decayService.decay(persona);
        updateService.update(event, persona);

        try {
            entity.setPersonaJson(mapper.writeValueAsString(persona));
        } catch (Exception ignored) {}

        entity.setUpdatedAt(Instant.now());
        repository.save(entity);
    }
}
