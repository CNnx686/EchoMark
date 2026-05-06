package org.tongji.sse.service.impl;

import org.springframework.stereotype.Service;
import org.tongji.sse.domain.model.UserPersona;
import org.tongji.sse.service.UserPersonaDecayService;

import java.util.Map;

@Service
public class UserPersonaDecayServiceImpl implements UserPersonaDecayService {

    private static final double DECAY = 0.98;

    @Override
    public void decay(UserPersona persona) {
        // tag：中等衰减
        decayAdaptive(persona.getTagWeights(), 10.0, 0.97, 0.92, 0.85);

        // keyword：比 tag 更快，避免短期词污染长期画像
        decayAdaptive(persona.getKeywordWeights(), 10.0, 0.95, 0.88, 0.80);

        // author：较快衰减，避免长期绑定创作者
        decayAdaptive(persona.getAuthorWeights(), 10.0, 0.93, 0.85, 0.75);

        // category：慢衰减（偏长期兴趣）
        decayAdaptive(persona.getCategoryWeights(), 10.0, 0.98, 0.95, 0.90);

        // 活跃时间：非常慢衰减（用户作息变化很慢）
        persona.getPreferredTimeSlots()
                .replaceAll((k, v) -> v * 0.995);

        // 行为占比：归一化衰减
        decayInteractionRatio(persona.getInteractionRatio());
    }

    private <K> void decayAdaptive(
            Map<K, Double> map,
            double cap,
            double lowDecay,
            double midDecay,
            double highDecay
    ) {
        map.replaceAll((k, v) -> {
            if (v > cap * 0.7) return v * highDecay;
            if (v > cap * 0.3) return v * midDecay;
            return v * lowDecay;
        });
    }

    private void decayInteractionRatio(Map<String, Double> ratio) {
        // 轻微衰减
        ratio.replaceAll((k, v) -> v * 0.98);

        // 归一化
        double sum = ratio.values().stream().mapToDouble(Double::doubleValue).sum();
        if (sum <= 0) return;

        ratio.replaceAll((k, v) -> v / sum);
    }
}
