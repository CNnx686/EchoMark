package org.tongji.sse.util;

import java.util.List;
import java.util.stream.Collectors;

import com.hankcs.hanlp.HanLP;
import com.hankcs.hanlp.seg.common.Term;

public final class KeywordExtractor {

    // 这一版不再需要手动维护 STOP_WORDS，因为 HanLP 内部有更完善的停用词库
    // 同时也无需手动 split，HanLP 会处理中文分词

    /**
     * 提取关键词
     * 使用 HanLP 的 TextRank 算法提取核心关键词
     *
     * @param text 输入的自然语言文本
     * @return 提取出的关键词列表
     */
    public static List<String> extract(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        // 1. 进行分词并带上词性标注
        List<Term> termList = HanLP.segment(text);

        // 2. 过滤出名词并取前 3 个
        return termList.stream()
                // 判断词性是否以 "n" 开头 (名词, 地名, 机构名等)
                .filter(term -> term.nature.startsWith("n"))
                // 过滤掉单字名词（通常关键词至少 2 个字更有意义）
                .filter(term -> term.word.length() > 1)
                // 获取词语字符串
                .map(term -> term.word)
                // 去重
                .distinct()
                // 截取前 3 个
                .limit(3)
                .collect(Collectors.toList());
    }
}

