package com.tencent.supersonic.chat.server.plugin.recognize.embedding;

import com.google.common.collect.Lists;
import com.tencent.supersonic.chat.server.plugin.ParseMode;
import com.tencent.supersonic.chat.server.plugin.Plugin;
import com.tencent.supersonic.chat.server.plugin.PluginManager;
import com.tencent.supersonic.chat.server.plugin.PluginRecallResult;
import com.tencent.supersonic.chat.server.plugin.recognize.PluginRecognizer;
import com.tencent.supersonic.chat.server.pojo.ChatParseContext;
import com.tencent.supersonic.common.config.EmbeddingConfig;
import com.tencent.supersonic.common.util.ContextUtils;
import com.tencent.supersonic.common.util.embedding.Retrieval;
import com.tencent.supersonic.common.util.embedding.RetrieveQueryResult;
import com.tencent.supersonic.headless.core.chat.parser.llm.PythonLLMProxy;
import com.tencent.supersonic.headless.core.utils.ComponentFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.CollectionUtils;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * EmbeddingRecallParser is an implementation of a recall plugin based on Embedding
 */
@Slf4j
public class EmbeddingRecallRecognizer extends PluginRecognizer {

    public boolean checkPreCondition(ChatParseContext chatParseContext) {
        EmbeddingConfig embeddingConfig = ContextUtils.getBean(EmbeddingConfig.class);
        if (StringUtils.isBlank(embeddingConfig.getUrl()) && ComponentFactory.getLLMProxy() instanceof PythonLLMProxy) {
            return false;
        }
        List<Plugin> plugins = getPluginList(chatParseContext);
        return !CollectionUtils.isEmpty(plugins);
    }

    public PluginRecallResult recallPlugin(ChatParseContext chatParseContext) {
        String text = chatParseContext.getQueryText();
        List<Retrieval> embeddingRetrievals = embeddingRecall(text);
        if (CollectionUtils.isEmpty(embeddingRetrievals)) {
            return null;
        }
        List<Plugin> plugins = getPluginList(chatParseContext);
        Map<Long, Plugin> pluginMap = plugins.stream().collect(Collectors.toMap(Plugin::getId, p -> p));
        for (Retrieval embeddingRetrieval : embeddingRetrievals) {
            Plugin plugin = pluginMap.get(Long.parseLong(embeddingRetrieval.getId()));
            if (plugin == null) {
                continue;
            }
            Pair<Boolean, Set<Long>> pair = PluginManager.resolve(plugin, chatParseContext);
            log.info("embedding plugin resolve: {}", pair);
            if (pair.getLeft()) {
                Set<Long> dataSetList = pair.getRight();
                if (CollectionUtils.isEmpty(dataSetList)) {
                    continue;
                }
                plugin.setParseMode(ParseMode.EMBEDDING_RECALL);
                double distance = embeddingRetrieval.getDistance();
                double score = chatParseContext.getQueryText().length() * (1 - distance);
                return PluginRecallResult.builder()
                        .plugin(plugin).dataSetIds(dataSetList).score(score).distance(distance).build();
            }
        }
        return null;
    }

    public List<Retrieval> embeddingRecall(String embeddingText) {
        try {
            PluginManager pluginManager = ContextUtils.getBean(PluginManager.class);
            RetrieveQueryResult embeddingResp = pluginManager.recognize(embeddingText);

            List<Retrieval> embeddingRetrievals = embeddingResp.getRetrieval();
            if (!CollectionUtils.isEmpty(embeddingRetrievals)) {
                embeddingRetrievals = embeddingRetrievals.stream().sorted(Comparator.comparingDouble(o ->
                        Math.abs(o.getDistance()))).collect(Collectors.toList());
                embeddingResp.setRetrieval(embeddingRetrievals);
            }
            return embeddingRetrievals;
        } catch (Exception e) {
            log.warn("get embedding result error ", e);
        }
        return Lists.newArrayList();
    }

}
