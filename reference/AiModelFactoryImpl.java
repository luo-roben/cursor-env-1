package com.br.cheetah.module.ai.framework.ai.core.model;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.lang.Assert;
import cn.hutool.core.lang.Singleton;
import cn.hutool.core.lang.func.Func0;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.RuntimeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.br.cheetah.framework.common.util.spring.SpringUtils;
import com.br.cheetah.module.ai.controller.admin.model.vo.model.AiModelFactoryVO;
import com.br.cheetah.module.ai.enums.model.AiPlatformEnum;
import com.br.cheetah.module.ai.framework.ai.config.AiAutoConfiguration;
import com.br.cheetah.module.ai.framework.ai.config.CheetahAiProperties;
import com.br.cheetah.module.ai.framework.ai.core.model.baichuan.BaiChuanChatModel;
import com.br.cheetah.module.ai.framework.ai.core.model.coze.CozeChatModel;
import com.br.cheetah.module.ai.framework.ai.core.model.coze.CozeConf;
import com.br.cheetah.module.ai.framework.ai.core.model.cybertron.CybertronConf;
import com.br.cheetah.module.ai.framework.ai.core.model.cybertron.CybertronModel;
import com.br.cheetah.module.ai.framework.ai.core.model.doubao.DouBaoChatModel;
import com.br.cheetah.module.ai.framework.ai.core.model.doubao.DouBaoEmbeddingModel;
import com.br.cheetah.module.ai.framework.ai.core.model.doubao.DoubaoImageConf;
import com.br.cheetah.module.ai.framework.ai.core.model.doubao.DoubaoImageModel;
import com.br.cheetah.module.ai.framework.ai.core.model.gemini.GeminiChatModel;
import com.br.cheetah.module.ai.framework.ai.core.model.hunyuan.HunYuanChatModel;
import com.br.cheetah.module.ai.framework.ai.core.model.midjourney.api.MidjourneyApi;
import com.br.cheetah.module.ai.framework.ai.core.model.siliconflow.SiliconFlowApiConstants;
import com.br.cheetah.module.ai.framework.ai.core.model.siliconflow.SiliconFlowChatModel;
import com.br.cheetah.module.ai.framework.ai.core.model.siliconflow.SiliconFlowImageApi;
import com.br.cheetah.module.ai.framework.ai.core.model.siliconflow.SiliconFlowImageModel;
import com.br.cheetah.module.ai.framework.ai.core.model.suno.api.SunoApi;
import com.br.cheetah.module.ai.framework.ai.core.model.xinghuo.XingHuoChatModel;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeChatAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeEmbeddingAutoConfiguration;
import com.alibaba.cloud.ai.autoconfigure.dashscope.DashScopeImageAutoConfiguration;
import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.api.DashScopeImageApi;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatModel;
import com.alibaba.cloud.ai.dashscope.chat.DashScopeChatOptions;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import com.alibaba.cloud.ai.dashscope.image.DashScopeImageModel;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.KeyCredential;
import com.br.cheetah.module.ai.util.ProxyConfigUtil;
import io.micrometer.observation.ObservationRegistry;
import io.milvus.client.MilvusServiceClient;
import io.qdrant.client.QdrantClient;
import io.qdrant.client.QdrantGrpcClient;
import lombok.SneakyThrows;
import org.springaicommunity.moonshot.MoonshotChatModel;
import org.springaicommunity.moonshot.MoonshotChatOptions;
import org.springaicommunity.moonshot.api.MoonshotApi;
import org.springaicommunity.moonshot.autoconfigure.MoonshotChatAutoConfiguration;
import org.springaicommunity.qianfan.QianFanChatModel;
import org.springaicommunity.qianfan.QianFanEmbeddingModel;
import org.springaicommunity.qianfan.QianFanEmbeddingOptions;
import org.springaicommunity.qianfan.QianFanImageModel;
import org.springaicommunity.qianfan.api.QianFanApi;
import org.springaicommunity.qianfan.api.QianFanImageApi;
import org.springaicommunity.qianfan.autoconfigure.QianFanChatAutoConfiguration;
import org.springaicommunity.qianfan.autoconfigure.QianFanEmbeddingAutoConfiguration;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.azure.openai.AzureOpenAiChatModel;
import org.springframework.ai.azure.openai.AzureOpenAiEmbeddingModel;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.DeepSeekApi;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.BatchingStrategy;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.embedding.observation.EmbeddingModelObservationConvention;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.minimax.MiniMaxChatModel;
import org.springframework.ai.minimax.MiniMaxChatOptions;
import org.springframework.ai.minimax.MiniMaxEmbeddingModel;
import org.springframework.ai.minimax.MiniMaxEmbeddingOptions;
import org.springframework.ai.minimax.api.MiniMaxApi;
import org.springframework.ai.model.anthropic.autoconfigure.AnthropicChatAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiChatAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.azure.openai.autoconfigure.AzureOpenAiEmbeddingProperties;
import org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration;
import org.springframework.ai.model.minimax.autoconfigure.MiniMaxChatAutoConfiguration;
import org.springframework.ai.model.minimax.autoconfigure.MiniMaxEmbeddingAutoConfiguration;
import org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.openai.autoconfigure.OpenAiImageAutoConfiguration;
import org.springframework.ai.model.stabilityai.autoconfigure.StabilityAiImageAutoConfiguration;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiChatAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiEmbeddingAutoConfiguration;
import org.springframework.ai.model.zhipuai.autoconfigure.ZhiPuAiImageAutoConfiguration;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.OllamaEmbeddingModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaModel;
import org.springframework.ai.ollama.api.OllamaOptions;
import org.springframework.ai.openai.*;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.ai.openai.api.common.OpenAiApiConstants;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.anthropic.api.AnthropicApi;
import org.springframework.ai.stabilityai.StabilityAiImageModel;
import org.springframework.ai.stabilityai.api.StabilityAiApi;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusServiceClientConnectionDetails;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusServiceClientProperties;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.milvus.autoconfigure.MilvusVectorStoreProperties;
import org.springframework.ai.vectorstore.observation.DefaultVectorStoreObservationConvention;
import org.springframework.ai.vectorstore.observation.VectorStoreObservationConvention;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.qdrant.autoconfigure.QdrantVectorStoreProperties;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreAutoConfiguration;
import org.springframework.ai.vectorstore.redis.autoconfigure.RedisVectorStoreProperties;
import org.springframework.ai.zhipuai.*;
import org.springframework.ai.zhipuai.api.ZhiPuAiApi;
import org.springframework.ai.zhipuai.api.ZhiPuAiImageApi;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.web.client.RestClient;
import redis.clients.jedis.JedisPooled;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static com.br.cheetah.framework.common.util.collection.CollectionUtils.convertList;
import static org.springframework.ai.retry.RetryUtils.DEFAULT_RETRY_TEMPLATE;

/**
 * AI Model 模型工厂的实现类
 *
 */
public class AiModelFactoryImpl implements AiModelFactory {

    @Override
    public ChatModel getOrCreateChatModel(AiPlatformEnum platform, AiModelFactoryVO vo) {
        String url = vo.getUrl();
        String apiKey = vo.getApiKey();
        switch (platform) {
            case TONG_YI:
                return buildTongYiChatModel(vo,false);
            case TONG_YI_IMG:
                return buildTongYiChatModel(vo,true);
            case YI_YAN:
                return buildYiYanChatModel(apiKey);
            case DEEP_SEEK:
                return buildDeepSeekChatModel(vo);
            case DOU_BAO:
                return buildDouBaoChatModel(vo);
            case DOU_BAO_IMG:
                return buildDoubaoImageChatModel(vo);
            case HUN_YUAN:
                return buildHunYuanChatModel(vo);
            case SILICON_FLOW:
                return buildSiliconFlowChatModel(vo);
            case ZHI_PU:
                return buildZhiPuChatModel(vo);
            case MINI_MAX:
                return buildMiniMaxChatModel(vo);
            case MOONSHOT:
                return buildMoonshotChatModel(vo);
            case XING_HUO:
                return buildXingHuoChatModel(apiKey);
            case BAI_CHUAN:
                return buildBaiChuanChatModel(vo);
            case CYBERTRON:
                return buildCybertronChatModel(vo);
            case COZE:
                return buildCozeChatModel(vo);
            case OPENAI,OPENROUTER:
                return buildOpenAiChatModel(vo);
            case AZURE_OPENAI:
                return buildAzureOpenAiChatModel(apiKey, url);
            case ANTHROPIC:
                return buildAnthropicChatModel(vo);
            case GEMINI:
                return buildGeminiChatModel(vo);
            case OLLAMA:
                return buildOllamaChatModel(vo);
            default:
                throw new IllegalArgumentException(StrUtil.format("未知平台({})", platform));
        }
    }

    @Override
    public ChatModel getDefaultChatModel(AiPlatformEnum platform) {
        // noinspection EnhancedSwitchMigration
        switch (platform) {
            case TONG_YI:
                return SpringUtil.getBean(DashScopeChatModel.class);
            case YI_YAN:
                return SpringUtil.getBean(QianFanChatModel.class);
            case DEEP_SEEK:
                return SpringUtil.getBean(DeepSeekChatModel.class);
            case DOU_BAO:
                return SpringUtil.getBean(DouBaoChatModel.class);
            case HUN_YUAN:
                return SpringUtil.getBean(HunYuanChatModel.class);
            case SILICON_FLOW:
                return SpringUtil.getBean(SiliconFlowChatModel.class);
            case ZHI_PU:
                return SpringUtil.getBean(ZhiPuAiChatModel.class);
            case MINI_MAX:
                return SpringUtil.getBean(MiniMaxChatModel.class);
            case MOONSHOT:
                return SpringUtil.getBean(MoonshotChatModel.class);
            case XING_HUO:
                return SpringUtil.getBean(XingHuoChatModel.class);
            case BAI_CHUAN:
                return SpringUtil.getBean(BaiChuanChatModel.class);
            case OPENAI:
                return SpringUtil.getBean(OpenAiChatModel.class);
            case AZURE_OPENAI:
                return SpringUtil.getBean(AzureOpenAiChatModel.class);
            case ANTHROPIC:
                return SpringUtil.getBean(AnthropicChatModel.class);
            case GEMINI:
                return SpringUtil.getBean(GeminiChatModel.class);
            case OLLAMA:
                return SpringUtil.getBean(OllamaChatModel.class);
            default:
                throw new IllegalArgumentException(StrUtil.format("未知平台({})", platform));
        }
    }

    @Override
    public ImageModel getDefaultImageModel(AiPlatformEnum platform) {
        // noinspection EnhancedSwitchMigration
        switch (platform) {
            case TONG_YI:
                return SpringUtil.getBean(DashScopeImageModel.class);
            case YI_YAN:
                return SpringUtil.getBean(QianFanImageModel.class);
            case ZHI_PU:
                return SpringUtil.getBean(ZhiPuAiImageModel.class);
            case SILICON_FLOW:
                return SpringUtil.getBean(SiliconFlowImageModel.class);
            case OPENAI:
                return SpringUtil.getBean(OpenAiImageModel.class);
            case STABLE_DIFFUSION:
                return SpringUtil.getBean(StabilityAiImageModel.class);
            default:
                throw new IllegalArgumentException(StrUtil.format("未知平台({})", platform));
        }
    }

    @Override
    public ImageModel getOrCreateImageModel(AiPlatformEnum platform, String apiKey, String url) {
        // noinspection EnhancedSwitchMigration
        switch (platform) {
            case TONG_YI:
                return buildTongYiImagesModel(apiKey);
            case YI_YAN:
                return buildQianFanImageModel(apiKey);
            case ZHI_PU:
                return buildZhiPuAiImageModel(apiKey, url);
            case OPENAI:
                return buildOpenAiImageModel(apiKey, url);
            case SILICON_FLOW:
                return buildSiliconFlowImageModel(apiKey,url);
            case STABLE_DIFFUSION:
                return buildStabilityAiImageModel(apiKey, url);
            default:
                throw new IllegalArgumentException(StrUtil.format("未知平台({})", platform));
        }
    }

    @Override
    public MidjourneyApi getOrCreateMidjourneyApi(String apiKey, String url) {
        String cacheKey = buildClientCacheKey(MidjourneyApi.class, AiPlatformEnum.MIDJOURNEY.getPlatform(), apiKey,
                url);
        return Singleton.get(cacheKey, (Func0<MidjourneyApi>) () -> {
            CheetahAiProperties.Midjourney properties = SpringUtil.getBean(CheetahAiProperties.class)
                    .getMidjourney();
            return new MidjourneyApi(url, apiKey, properties.getNotifyUrl());
        });
    }

    @Override
    public SunoApi getOrCreateSunoApi(String apiKey, String url) {
        String cacheKey = buildClientCacheKey(SunoApi.class, AiPlatformEnum.SUNO.getPlatform(), apiKey, url);
        return Singleton.get(cacheKey, (Func0<SunoApi>) () -> new SunoApi(url));
    }

    @Override
    @SuppressWarnings("EnhancedSwitchMigration")
    public EmbeddingModel getOrCreateEmbeddingModel(AiPlatformEnum platform, String apiKey, String url, String model) {
        String cacheKey = buildClientCacheKey(EmbeddingModel.class, platform, apiKey, url, model);
        return Singleton.get(cacheKey, (Func0<EmbeddingModel>) () -> {
            switch (platform) {
                case TONG_YI:
                    return buildTongYiEmbeddingModel(apiKey, model);
                case YI_YAN:
                    return buildYiYanEmbeddingModel(apiKey, model);
                case DOU_BAO:
                    return buildDouBaoEmbeddingModel(apiKey, url, model);
                case ZHI_PU:
                    return buildZhiPuEmbeddingModel(apiKey, url, model);
                case MINI_MAX:
                    return buildMiniMaxEmbeddingModel(apiKey, url, model);
                case OPENAI:
                    return buildOpenAiEmbeddingModel(apiKey, url, model);
                case AZURE_OPENAI:
                    return buildAzureOpenAiEmbeddingModel(apiKey, url, model);
                case OLLAMA:
                    return buildOllamaEmbeddingModel(url, model);
                default:
                    throw new IllegalArgumentException(StrUtil.format("未知平台({})", platform));
            }
        });
    }

    @Override
    public VectorStore getOrCreateVectorStore(Class<? extends VectorStore> type,
                                              EmbeddingModel embeddingModel,
                                              Map<String, Class<?>> metadataFields) {
        String cacheKey = buildClientCacheKey(VectorStore.class, embeddingModel, type);
        return Singleton.get(cacheKey, (Func0<VectorStore>) () -> {
            if (type == SimpleVectorStore.class) {
                return buildSimpleVectorStore(embeddingModel);
            }
            if (type == QdrantVectorStore.class) {
                return buildQdrantVectorStore(embeddingModel);
            }
            if (type == RedisVectorStore.class) {
                return buildRedisVectorStore(embeddingModel, metadataFields);
            }
            if (type == MilvusVectorStore.class) {
                return buildMilvusVectorStore(embeddingModel);
            }
            throw new IllegalArgumentException(StrUtil.format("未知类型({})", type));
        });
    }

    private static String buildClientCacheKey(Class<?> clazz, Object... params) {
        if (ArrayUtil.isEmpty(params)) {
            return clazz.getName();
        }
        return StrUtil.format("{}#{}", clazz.getName(), ArrayUtil.join(params, "_"));
    }

    // ========== 各种创建 spring-ai 客户端的方法 ==========

    /**
     * 可参考 {@link DashScopeChatAutoConfiguration} 的 dashscopeChatModel 方法
     */
    private static DashScopeChatModel buildTongYiChatModel(AiModelFactoryVO vo,Boolean multiModel) {
        //设置baseurl、apikey
        DashScopeApi.Builder apiBuilder = DashScopeApi.builder()
                .apiKey(vo.getApiKey());
        if(StrUtil.isNotEmpty(vo.getUrl())){
            apiBuilder.baseUrl(vo.getUrl());
        }
        //是否开启代理
        if(vo.getEnableProxy() == 1){
            apiBuilder.webClientBuilder(ProxyConfigUtil.getWebClient())
                    .restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //设置模型、温度、最大上下文等参数
        DashScopeChatOptions.DashscopeChatOptionsBuilder optionsBuilder = DashScopeChatOptions.builder()
                .withModel(StrUtil.blankToDefault(vo.getModel(),DashScopeApi.DEFAULT_CHAT_MODEL))
                .withTemperature(vo.getTemperature()!=null?vo.getTemperature():0.7);
        if(vo.getMaxTokens()!=null){
            optionsBuilder.withMaxToken(vo.getMaxTokens());
        }
        //设置是否启用多模态
        optionsBuilder.withMultiModel(multiModel);
        //构建ChatModel
        return DashScopeChatModel.builder()
                .dashScopeApi(apiBuilder.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    /**
     * 可参考 {@link DashScopeImageAutoConfiguration} 的 dashScopeImageModel 方法
     */
    private static DashScopeImageModel buildTongYiImagesModel(String key) {
        DashScopeImageApi dashScopeImageApi = DashScopeImageApi.builder().apiKey(key).build();
        return DashScopeImageModel.builder()
                .dashScopeApi(dashScopeImageApi)
                .build();
    }

    /**
     * 可参考 {@link QianFanChatAutoConfiguration} 的 qianFanChatModel 方法
     */
    private static QianFanChatModel buildYiYanChatModel(String key) {
        // TODO spring ai qianfan 有 bug，无法使用 https://github.com/spring-ai-community/qianfan/issues/6
        List<String> keys = StrUtil.split(key, '|');
        Assert.equals(keys.size(), 2, "YiYanChatClient 的密钥需要 (appKey|secretKey) 格式");
        String appKey = keys.get(0);
        String secretKey = keys.get(1);
        QianFanApi qianFanApi = new QianFanApi(appKey, secretKey);
        return new QianFanChatModel(qianFanApi);
    }

    /**
     * 可参考 {@link QianFanEmbeddingAutoConfiguration} 的 qianFanImageModel 方法
     */
    private QianFanImageModel buildQianFanImageModel(String key) {
        // TODO spring ai qianfan 有 bug，无法使用 https://github.com/spring-ai-community/qianfan/issues/6
        List<String> keys = StrUtil.split(key, '|');
        Assert.equals(keys.size(), 2, "YiYanChatClient 的密钥需要 (appKey|secretKey) 格式");
        String appKey = keys.get(0);
        String secretKey = keys.get(1);
        QianFanImageApi qianFanApi = new QianFanImageApi(appKey, secretKey);
        return new QianFanImageModel(qianFanApi);
    }

    /**
     * 可参考 {@link DeepSeekChatAutoConfiguration} 的 deepSeekChatModel 方法
     */
    private static DeepSeekChatModel buildDeepSeekChatModel(AiModelFactoryVO vo) {
        //设置baseurl、apikey
        DeepSeekApi.Builder apiBuilder = DeepSeekApi.builder()
                .apiKey(vo.getApiKey());
        if(StrUtil.isNotEmpty(vo.getUrl())){
            apiBuilder.baseUrl(vo.getUrl());
        }
        //是否使用代理
        if(vo.getEnableProxy() == 1){
            apiBuilder.webClientBuilder(ProxyConfigUtil.getWebClient())
                    .restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //配置模型、温度、最大上下文等参数
        DeepSeekChatOptions.Builder optionsBuilder = DeepSeekChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(), DeepSeekApi.ChatModel.DEEPSEEK_CHAT.getValue()));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.maxTokens(vo.getMaxTokens());
        }
        //构建ChatModel
        return DeepSeekChatModel.builder()
                .deepSeekApi(apiBuilder.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    /**
     * 可参考 {@link AiAutoConfiguration#douBaoChatClient(CheetahAiProperties)}
     */
    private ChatModel buildDouBaoChatModel(AiModelFactoryVO vo) {
        //设置baseurl、apikey
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .baseUrl(StrUtil.blankToDefault(vo.getUrl(),DouBaoChatModel.BASE_URL))
                .apiKey(vo.getApiKey());
        //是否使用代理
        if(vo.getEnableProxy() == 1){
            apiBuilder.webClientBuilder(ProxyConfigUtil.getWebClient())
                    .restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //配置模型、温度、最大上下文等参数
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),DouBaoChatModel.MODEL_DEFAULT));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.maxTokens(vo.getMaxTokens());
        }
        //构建ChatModel
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(apiBuilder.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new DouBaoChatModel(openAiChatModel);
    }

    /**
     * 构建豆包图像理解ChatModel
     */
    private ChatModel buildDoubaoImageChatModel(AiModelFactoryVO vo) {
        // 构建豆包图像理解配置
        DoubaoImageConf conf = DoubaoImageConf.builder()
                .baseUrl(vo.getUrl())
                .apiKey(vo.getApiKey())
                .model(vo.getModel())
                .temperature(vo.getTemperature())
                .maxTokens(vo.getMaxTokens())
                .build();
        return new DoubaoImageModel(conf);
    }

    /**
     * 可参考 {@link AiAutoConfiguration#hunYuanChatClient(CheetahAiProperties)}
     */
    private ChatModel buildHunYuanChatModel(AiModelFactoryVO vo) {
        //设置baseurl、apikey
        DeepSeekApi.Builder deepSeekApi = DeepSeekApi.builder()
                .completionsPath(HunYuanChatModel.COMPLETE_PATH)
                .apiKey(vo.getApiKey());
        // 特殊：由于混元大模型不提供 deepseek，而是通过知识引擎，所以需要区分下 URL
        if (StrUtil.isEmpty(vo.getUrl())) {
            deepSeekApi.baseUrl(
                    StrUtil.startWithIgnoreCase(vo.getModel(), "deepseek") ? HunYuanChatModel.DEEP_SEEK_BASE_URL
                            : HunYuanChatModel.BASE_URL);
        }else{
            deepSeekApi.baseUrl(vo.getUrl());
        }
        //判断是否使用代理
        if (vo.getEnableProxy() == 1) {
            deepSeekApi.restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //设置模型、温度、最大上下文等参数
        DeepSeekChatOptions.Builder optionsBuilder = DeepSeekChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),HunYuanChatModel.MODEL_DEFAULT));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.maxTokens(vo.getMaxTokens());
        }
        // 创建 DeepSeekChatModel、HunYuanChatModel 对象
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new HunYuanChatModel(openAiChatModel);
    }

    /**
     * 可参考 {@link AiAutoConfiguration#siliconFlowChatClient(CheetahAiProperties)}
     */
    private ChatModel buildSiliconFlowChatModel(AiModelFactoryVO vo) {
        //设置baseurl、apikey
        DeepSeekApi.Builder deepSeekApi = DeepSeekApi.builder()
                .baseUrl(StrUtil.blankToDefault(vo.getUrl(),SiliconFlowApiConstants.DEFAULT_BASE_URL))
                .apiKey(vo.getApiKey());
        //判断是否使用代理
        if (vo.getEnableProxy() == 1) {
            deepSeekApi.restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //设置模型、温度、最大上下文等参数
        DeepSeekChatOptions.Builder optionsBuilder = DeepSeekChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),SiliconFlowApiConstants.MODEL_DEFAULT));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.maxTokens(vo.getMaxTokens());
        }
        //构建ChatModel
        DeepSeekChatModel openAiChatModel = DeepSeekChatModel.builder()
                .deepSeekApi(deepSeekApi.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new SiliconFlowChatModel(openAiChatModel);
    }

    private ChatModel buildCybertronChatModel(AiModelFactoryVO  vo) {
        //赛博坦、扣子这类智能体平台，具体的model、温度等参数是在智能体中设置的，这里只提供连接bot的基础构建
        CybertronConf cybertronConf = new CybertronConf();
        cybertronConf.setUrl(vo.getUrl());
        cybertronConf.setUsername(vo.getUsername());
        cybertronConf.setRobotKey(vo.getRobotId());
        cybertronConf.setRobotToken(vo.getApiKey());
        return new CybertronModel(cybertronConf);
    }

    //构造扣子ChatModel
    private ChatModel buildCozeChatModel(AiModelFactoryVO  vo) {
        //赛博坦、扣子这类智能体平台，具体的model、温度等参数是在智能体中设置的，这里只提供连接bot的基础构建
        //扣子这里的model实际上是Agent智能体的botId，具体使用的模型在Agent中配置
        CozeConf cozeConf = CozeConf.builder()
                .host(vo.getUrl())
                .botId(vo.getModel())
                .apiKey(vo.getApiKey())
                .build();
        return new CozeChatModel(cozeConf);
    }
    /**
     * 可参考 {@link ZhiPuAiChatAutoConfiguration} 的 zhiPuAiChatModel 方法
     */
    private ZhiPuAiChatModel buildZhiPuChatModel(AiModelFactoryVO vo) {
        //ZhiPu不提供Build方法，灵活度不够
        ZhiPuAiApi zhiPuAiApi;
        //判断是否使用代理，使用代理要求url必传
        if(vo.getEnableProxy() == 1){
            zhiPuAiApi = new ZhiPuAiApi(vo.getUrl(), vo.getApiKey(),ProxyConfigUtil.getRestClient());
        }else if(StrUtil.isNotEmpty(vo.getUrl())){
            zhiPuAiApi = new ZhiPuAiApi(vo.getUrl(), vo.getApiKey());
        }else{
            zhiPuAiApi = new ZhiPuAiApi(vo.getApiKey());
        }
        //设置model、温度、最大上下文
        ZhiPuAiChatOptions.Builder options = ZhiPuAiChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),ZhiPuAiApi.DEFAULT_CHAT_MODEL));
        if(vo.getTemperature()!=null){
            options.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            options.maxTokens(vo.getMaxTokens());
        }
        //构建ChatModel
        return new ZhiPuAiChatModel(zhiPuAiApi, options.build(), getToolCallingManager(), DEFAULT_RETRY_TEMPLATE,
                getObservationRegistry().getIfAvailable());
    }

    /**
     * 可参考 {@link ZhiPuAiImageAutoConfiguration} 的 zhiPuAiImageModel 方法
     */
    private ZhiPuAiImageModel buildZhiPuAiImageModel(String apiKey, String url) {
        ZhiPuAiImageApi zhiPuAiApi = StrUtil.isEmpty(url) ? new ZhiPuAiImageApi(apiKey)
                : new ZhiPuAiImageApi(url, apiKey, RestClient.builder());
        return new ZhiPuAiImageModel(zhiPuAiApi);
    }

    /**
     * 可参考 {@link MiniMaxChatAutoConfiguration} 的 miniMaxChatModel 方法
     */
    private MiniMaxChatModel buildMiniMaxChatModel(AiModelFactoryVO vo) {
        //MiniMaxApi不提供Build方法，灵活度不够
        MiniMaxApi miniMaxApi;
        //判断是否使用代理，使用代理要求url必传
        if(vo.getEnableProxy() == 1){
            miniMaxApi = new MiniMaxApi(vo.getUrl(), vo.getApiKey(),ProxyConfigUtil.getRestClient());
        }else if(StrUtil.isNotEmpty(vo.getUrl())){
            miniMaxApi = new MiniMaxApi(vo.getUrl(), vo.getApiKey());
        }else{
            miniMaxApi = new MiniMaxApi(vo.getApiKey());
        }
        //设置model、温度、最大上下文等参数
        MiniMaxChatOptions.Builder optionsBuilder = MiniMaxChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),MiniMaxApi.DEFAULT_CHAT_MODEL));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.maxTokens(vo.getMaxTokens());
        }
        return new MiniMaxChatModel(miniMaxApi, optionsBuilder.build(), getToolCallingManager(), DEFAULT_RETRY_TEMPLATE);
    }

    /**
     * 可参考 {@link MoonshotChatAutoConfiguration} 的 moonshotChatModel 方法
     */
    private MoonshotChatModel buildMoonshotChatModel(AiModelFactoryVO vo) {
        //设置baseurl、apikey
        MoonshotApi.Builder moonshotApiBuilder = MoonshotApi.builder()
                .apiKey(vo.getApiKey());
        if (StrUtil.isNotEmpty(vo.getUrl())) {
            moonshotApiBuilder.baseUrl(vo.getUrl());
        }
        //是否使用代理
        if(vo.getEnableProxy() == 1){
            moonshotApiBuilder.webClientBuilder(ProxyConfigUtil.getWebClient())
                    .restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //设置model、温度、最大上下文等参数
        MoonshotChatOptions.Builder optionsBuilder = MoonshotChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),MoonshotApi.DEFAULT_CHAT_MODEL));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.maxTokens(vo.getMaxTokens());
        }
        //构建ChatModel
        return MoonshotChatModel.builder()
                .moonshotApi(moonshotApiBuilder.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    /**
     * 可参考 {@link AiAutoConfiguration#xingHuoChatClient(CheetahAiProperties)}
     */
    private static XingHuoChatModel buildXingHuoChatModel(String key) {
        List<String> keys = StrUtil.split(key, '|');
        Assert.equals(keys.size(), 2, "XingHuoChatClient 的密钥需要 (appKey|secretKey) 格式");
        CheetahAiProperties.XingHuo properties = new CheetahAiProperties.XingHuo()
                .setAppKey(keys.get(0)).setSecretKey(keys.get(1));
        return new AiAutoConfiguration().buildXingHuoChatClient(properties);
    }

    /**
     * 可参考 {@link AiAutoConfiguration#baiChuanChatClient(CheetahAiProperties)}
     */
    private BaiChuanChatModel buildBaiChuanChatModel(AiModelFactoryVO vo) {
        //设置baseurl、apikey
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .baseUrl(StrUtil.blankToDefault(vo.getUrl(), BaiChuanChatModel.BASE_URL))
                .apiKey(vo.getApiKey());
        //是否使用代理
        if(vo.getEnableProxy() == 1){
            apiBuilder.webClientBuilder(ProxyConfigUtil.getWebClient())
                    .restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //设置model、温度、最大上下文等参数
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),BaiChuanChatModel.MODEL_DEFAULT));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.maxTokens(vo.getMaxTokens());
        }
        //构建ChatModel
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(apiBuilder.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new BaiChuanChatModel(openAiChatModel);
    }

    /**
     * 可参考 {@link OpenAiChatAutoConfiguration} 的 openAiChatModel 方法
     */
    private static OpenAiChatModel buildOpenAiChatModel(AiModelFactoryVO vo) {
        //设置baseurl、apikey
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .baseUrl(StrUtil.blankToDefault(vo.getUrl(), OpenAiApiConstants.DEFAULT_BASE_URL))
                .apiKey(vo.getApiKey());
        //是否使用代理
        if(vo.getEnableProxy() == 1){
            //使用海外代理
            apiBuilder.webClientBuilder(ProxyConfigUtil.getOutsideWebClient())
                    .restClientBuilder(ProxyConfigUtil.getOutsideRestClient());
        }
        //设置model、温度、最大上下文等参数
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),OpenAiApi.ChatModel.GPT_4_O.getValue()));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.maxTokens(vo.getMaxTokens());
        }
        //构建ChatModel
        return OpenAiChatModel.builder()
                .openAiApi(apiBuilder.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    /**
     * 可参考 {@link AzureOpenAiChatAutoConfiguration}
     */
    private static AzureOpenAiChatModel buildAzureOpenAiChatModel(String apiKey, String url) {
        // TODO @芋艿：使用前，请测试，暂时没密钥！！！
        OpenAIClientBuilder openAIClientBuilder = new OpenAIClientBuilder()
                .endpoint(url).credential(new KeyCredential(apiKey));
        return AzureOpenAiChatModel.builder()
                .openAIClientBuilder(openAIClientBuilder)
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    /**
     * 可参考 {@link AnthropicChatAutoConfiguration} 的 anthropicApi 方法
     */
    private static AnthropicChatModel buildAnthropicChatModel(AiModelFactoryVO vo) {
        //设置baseurl、apikey
        AnthropicApi.Builder apiBuilder = AnthropicApi.builder().apiKey(vo.getApiKey());
        if (StrUtil.isNotEmpty(vo.getUrl())) {
            apiBuilder.baseUrl(vo.getUrl());
        }
        //是否使用代理
        if(vo.getEnableProxy() == 1){
            apiBuilder.webClientBuilder(ProxyConfigUtil.getWebClient())
                    .restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //设置模型、温度、最大上下文等参数
        AnthropicChatOptions.Builder optionsBuilder = AnthropicChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),AnthropicChatModel.DEFAULT_MODEL_NAME))
                .maxTokens(vo.getMaxTokens()!=null?vo.getMaxTokens():AnthropicChatModel.DEFAULT_MAX_TOKENS)
                .temperature(vo.getTemperature()!=null?vo.getTemperature():AnthropicChatModel.DEFAULT_TEMPERATURE);
        //构建ChatModel
        return AnthropicChatModel.builder()
                .anthropicApi(apiBuilder.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    /**
     * 可参考 {@link AiAutoConfiguration#buildGeminiChatClient(CheetahAiProperties.Gemini)}
     */
    private static GeminiChatModel buildGeminiChatModel(AiModelFactoryVO vo) {
        //设置baseurl、apikey
        OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
                .baseUrl(StrUtil.blankToDefault(vo.getUrl(),GeminiChatModel.BASE_URL))
                .completionsPath(GeminiChatModel.COMPLETE_PATH)
                .apiKey(vo.getApiKey());
        //是否使用代理
        if(vo.getEnableProxy() == 1){
            apiBuilder.webClientBuilder(ProxyConfigUtil.getWebClient())
                    .restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //设置模型、温度、最大上下文等参数
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),GeminiChatModel.MODEL_DEFAULT));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.maxTokens(vo.getMaxTokens());
        }
        //构建ChatModel
        OpenAiChatModel openAiChatModel = OpenAiChatModel.builder()
                .openAiApi(apiBuilder.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
        return new GeminiChatModel(openAiChatModel);
    }

    /**
     * 可参考 {@link OpenAiImageAutoConfiguration} 的 openAiImageModel 方法
     */
    private OpenAiImageModel buildOpenAiImageModel(String openAiToken, String url) {
        url = StrUtil.blankToDefault(url, OpenAiApiConstants.DEFAULT_BASE_URL);
        OpenAiImageApi openAiApi = OpenAiImageApi.builder().baseUrl(url).apiKey(openAiToken).build();
        return new OpenAiImageModel(openAiApi);
    }

    /**
     * 创建 SiliconFlowImageModel 对象
     */
    private SiliconFlowImageModel buildSiliconFlowImageModel(String apiToken, String url) {
        url = StrUtil.blankToDefault(url, SiliconFlowApiConstants.DEFAULT_BASE_URL);
        SiliconFlowImageApi openAiApi = new SiliconFlowImageApi(url, apiToken);
        return new SiliconFlowImageModel(openAiApi);
    }

    /**
     * 可参考 {@link OllamaChatAutoConfiguration} 的 ollamaChatModel 方法
     */
    private static OllamaChatModel buildOllamaChatModel(AiModelFactoryVO vo) {
        //设置baseurl
        OllamaApi.Builder apiBuilder = OllamaApi.builder()
                .baseUrl(vo.getUrl());

        //是否使用代理
        if(vo.getEnableProxy() == 1){
            apiBuilder.webClientBuilder(ProxyConfigUtil.getWebClient())
                    .restClientBuilder(ProxyConfigUtil.getRestClient());
        }
        //设置模型、温度、最大上下文等参数
        OllamaOptions.Builder optionsBuilder = OllamaOptions.builder()
                .model(StrUtil.blankToDefault(vo.getModel(),OllamaModel.MISTRAL.id()));
        if(vo.getTemperature()!=null){
            optionsBuilder.temperature(vo.getTemperature());
        }
        if(vo.getMaxTokens()!=null){
            optionsBuilder.numPredict(vo.getMaxTokens());
        }
        //构建ChatModel
        return OllamaChatModel.builder()
                .ollamaApi(apiBuilder.build())
                .defaultOptions(optionsBuilder.build())
                .toolCallingManager(getToolCallingManager())
                .build();
    }

    /**
     * 可参考 {@link StabilityAiImageAutoConfiguration} 的 stabilityAiImageModel 方法
     */
    private StabilityAiImageModel buildStabilityAiImageModel(String apiKey, String url) {
        url = StrUtil.blankToDefault(url, StabilityAiApi.DEFAULT_BASE_URL);
        StabilityAiApi stabilityAiApi = new StabilityAiApi(apiKey, StabilityAiApi.DEFAULT_IMAGE_MODEL, url);
        return new StabilityAiImageModel(stabilityAiApi);
    }

    // ========== 各种创建 EmbeddingModel 的方法 ==========

    /**
     * 可参考 {@link DashScopeEmbeddingAutoConfiguration} 的 dashscopeEmbeddingModel 方法
     */
    private DashScopeEmbeddingModel buildTongYiEmbeddingModel(String apiKey, String model) {
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
        DashScopeEmbeddingOptions dashScopeEmbeddingOptions = DashScopeEmbeddingOptions.builder().withModel(model).build();
        return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED, dashScopeEmbeddingOptions);
    }

    /**
     * 可参考 {@link ZhiPuAiEmbeddingAutoConfiguration} 的 zhiPuAiEmbeddingModel 方法
     */
    private ZhiPuAiEmbeddingModel buildZhiPuEmbeddingModel(String apiKey, String url, String model) {
        ZhiPuAiApi zhiPuAiApi = StrUtil.isEmpty(url) ? new ZhiPuAiApi(apiKey)
                : new ZhiPuAiApi(url, apiKey);
        ZhiPuAiEmbeddingOptions zhiPuAiEmbeddingOptions = ZhiPuAiEmbeddingOptions.builder().model(model).build();
        return new ZhiPuAiEmbeddingModel(zhiPuAiApi, MetadataMode.EMBED, zhiPuAiEmbeddingOptions);
    }

    /**
     * 可参考 {@link MiniMaxEmbeddingAutoConfiguration} 的 miniMaxEmbeddingModel 方法
     */
    private EmbeddingModel buildMiniMaxEmbeddingModel(String apiKey, String url, String model) {
        MiniMaxApi miniMaxApi = StrUtil.isEmpty(url)? new MiniMaxApi(apiKey)
                : new MiniMaxApi(url, apiKey);
        MiniMaxEmbeddingOptions miniMaxEmbeddingOptions = MiniMaxEmbeddingOptions.builder().model(model).build();
        return new MiniMaxEmbeddingModel(miniMaxApi, MetadataMode.EMBED, miniMaxEmbeddingOptions);
    }

    /**
     * 可参考 {@link QianFanEmbeddingAutoConfiguration} 的 qianFanEmbeddingModel 方法
     */
    private QianFanEmbeddingModel buildYiYanEmbeddingModel(String key, String model) {
        List<String> keys = StrUtil.split(key, '|');
        Assert.equals(keys.size(), 2, "YiYanChatClient 的密钥需要 (appKey|secretKey) 格式");
        String appKey = keys.get(0);
        String secretKey = keys.get(1);
        QianFanApi qianFanApi = new QianFanApi(appKey, secretKey);
        QianFanEmbeddingOptions qianFanEmbeddingOptions = QianFanEmbeddingOptions.builder().model(model).build();
        return new QianFanEmbeddingModel(qianFanApi, MetadataMode.EMBED, qianFanEmbeddingOptions);
    }

    private OllamaEmbeddingModel buildOllamaEmbeddingModel(String url, String model) {
        OllamaApi ollamaApi = OllamaApi.builder().baseUrl(url).build();
        OllamaOptions ollamaOptions = OllamaOptions.builder().model(model).build();
        return OllamaEmbeddingModel.builder()
                .ollamaApi(ollamaApi)
                .defaultOptions(ollamaOptions)
                .build();
    }

    /**
     * 可参考 {@link OpenAiEmbeddingAutoConfiguration} 的 openAiEmbeddingModel 方法
     */
    private OpenAiEmbeddingModel buildOpenAiEmbeddingModel(String openAiToken, String url, String model) {
        url = StrUtil.blankToDefault(url, OpenAiApiConstants.DEFAULT_BASE_URL);
        OpenAiApi openAiApi = OpenAiApi.builder().baseUrl(url).apiKey(openAiToken).build();
        OpenAiEmbeddingOptions openAiEmbeddingProperties = OpenAiEmbeddingOptions.builder().model(model).build();
        return new OpenAiEmbeddingModel(openAiApi, MetadataMode.EMBED, openAiEmbeddingProperties);
    }
    /**
     * 构建豆包 EmbeddingModel
     * 使用火山引擎的 VikingDB Embedding API
     * 
     * @param apiKey API密钥
     * @param url API地址，如果为空则使用默认值
     * @param model 模型配置，格式：denseModel:denseVersion:dim 或 denseModel:denseVersion 或 denseModel
     *              例如：doubao-embedding-large:240915:1024
     * @return EmbeddingModel
     */
    private EmbeddingModel buildDouBaoEmbeddingModel(String apiKey, String url, String model) {
        // Embedding API 使用不同的 host，默认使用 api-vikingdb.volces.com
        // 如果 url 包含完整 URL，使用它；否则使用默认 host
        if (StrUtil.isEmpty(url)) {
            url = "https://" + DouBaoEmbeddingModel.DEFAULT_HOST;
        } else if (!url.startsWith("http")) {
            url = "https://" + url;
        }
        
        // 解析模型配置（可以从 model 参数中解析，或使用默认值）
        // 格式可以是：denseModel:denseVersion:dim 或 denseModel:denseVersion 或 denseModel
        String denseModel = DouBaoEmbeddingModel.DEFAULT_DENSE_MODEL;
        String denseVersion = DouBaoEmbeddingModel.DEFAULT_DENSE_VERSION;
        int dim = DouBaoEmbeddingModel.DEFAULT_DIM;
        
        // 如果 model 参数不为空，解析它（虽然当前使用 sparseModel，但保留解析逻辑）
        if (StrUtil.isNotEmpty(model)) {
            String[] parts = model.split(":");
            if (parts.length > 0 && StrUtil.isNotEmpty(parts[0])) {
                denseModel = parts[0];
            }
            if (parts.length > 1 && StrUtil.isNotEmpty(parts[1])) {
                denseVersion = parts[1];
            }
            if (parts.length > 2 && StrUtil.isNotEmpty(parts[2])) {
                try {
                    dim = Integer.parseInt(parts[2]);
                } catch (NumberFormatException e) {
                    // 维度参数解析失败，使用默认值
                }
            }
        }
        
        return new DouBaoEmbeddingModel(url, apiKey, denseModel, denseVersion, dim, 
                                       DouBaoEmbeddingModel.DEFAULT_SPARSE_MODEL, 
                                       DouBaoEmbeddingModel.DEFAULT_SPARSE_VERSION);
    }
    /**
     * 可参考 {@link AzureOpenAiEmbeddingAutoConfiguration} 的 azureOpenAiEmbeddingModel 方法
     */
    private AzureOpenAiEmbeddingModel buildAzureOpenAiEmbeddingModel(String apiKey, String url, String model) {
        // TODO @芋艿：手头暂时没密钥，使用建议再测试下
        AzureOpenAiEmbeddingAutoConfiguration azureOpenAiAutoConfiguration = new AzureOpenAiEmbeddingAutoConfiguration();
        // 创建 OpenAIClientBuilder 对象
        OpenAIClientBuilder openAIClientBuilder = new OpenAIClientBuilder()
                .endpoint(url).credential(new KeyCredential(apiKey));
        // 获取 AzureOpenAiChatProperties 对象
        AzureOpenAiEmbeddingProperties embeddingProperties = SpringUtil.getBean(AzureOpenAiEmbeddingProperties.class);
        return azureOpenAiAutoConfiguration.azureOpenAiEmbeddingModel(openAIClientBuilder, embeddingProperties,
                getObservationRegistry(), getEmbeddingModelObservationConvention());
    }

    // ========== 各种创建 VectorStore 的方法 ==========

    /**
     * 注意：仅适合本地测试使用，生产建议还是使用 Qdrant、Milvus 等
     */
    @SneakyThrows
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private SimpleVectorStore buildSimpleVectorStore(EmbeddingModel embeddingModel) {
        SimpleVectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();
        // 启动加载
        File file = new File(StrUtil.format("{}/vector_store/simple_{}.json",
                FileUtil.getUserHomePath(), embeddingModel.getClass().getSimpleName()));
        if (!file.exists()) {
            FileUtil.mkParentDirs(file);
            file.createNewFile();
        } else if (file.length() > 0) {
            vectorStore.load(file);
        }
        // 定时持久化，每分钟一次
        Timer timer = new Timer("SimpleVectorStoreTimer-" + file.getAbsolutePath());
        timer.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                vectorStore.save(file);
            }

        }, Duration.ofMinutes(1).toMillis(), Duration.ofMinutes(1).toMillis());
        // 关闭时，进行持久化
        RuntimeUtil.addShutdownHook(() -> vectorStore.save(file));
        return vectorStore;
    }

    /**
     * 参考 {@link QdrantVectorStoreAutoConfiguration} 的 vectorStore 方法
     */
    @SneakyThrows
    private QdrantVectorStore buildQdrantVectorStore(EmbeddingModel embeddingModel) {
        QdrantVectorStoreAutoConfiguration configuration = new QdrantVectorStoreAutoConfiguration();
        QdrantVectorStoreProperties properties = SpringUtil.getBean(QdrantVectorStoreProperties.class);
        // 参考 QdrantVectorStoreAutoConfiguration 实现，创建 QdrantClient 对象
        QdrantGrpcClient.Builder grpcClientBuilder = QdrantGrpcClient.newBuilder(
                properties.getHost(), properties.getPort(), properties.isUseTls());
        if (StrUtil.isNotEmpty(properties.getApiKey())) {
            grpcClientBuilder.withApiKey(properties.getApiKey());
        }
        QdrantClient qdrantClient = new QdrantClient(grpcClientBuilder.build());
        // 创建 QdrantVectorStore 对象
        QdrantVectorStore vectorStore = configuration.vectorStore(embeddingModel, properties, qdrantClient,
                getObservationRegistry(), getCustomObservationConvention(), getBatchingStrategy());
        // 初始化索引
        vectorStore.afterPropertiesSet();
        return vectorStore;
    }

    /**
     * 参考 {@link RedisVectorStoreAutoConfiguration} 的 vectorStore 方法
     */
    private RedisVectorStore buildRedisVectorStore(EmbeddingModel embeddingModel,
                                                   Map<String, Class<?>> metadataFields) {
        // 创建 JedisPooled 对象
        RedisProperties redisProperties = SpringUtils.getBean(RedisProperties.class);
        JedisPooled jedisPooled = new JedisPooled(redisProperties.getHost(), redisProperties.getPort(),
                redisProperties.getUsername(), redisProperties.getPassword());
        // 创建 RedisVectorStoreProperties 对象
        RedisVectorStoreProperties properties = SpringUtil.getBean(RedisVectorStoreProperties.class);
        RedisVectorStore redisVectorStore = RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(properties.getIndexName()).prefix(properties.getPrefix())
                .initializeSchema(properties.isInitializeSchema())
                .metadataFields(convertList(metadataFields.entrySet(), entry -> {
                    String fieldName = entry.getKey();
                    Class<?> fieldType = entry.getValue();
                    if (Number.class.isAssignableFrom(fieldType)) {
                        return RedisVectorStore.MetadataField.numeric(fieldName);
                    }
                    if (Boolean.class.isAssignableFrom(fieldType)) {
                        return RedisVectorStore.MetadataField.tag(fieldName);
                    }
                    return RedisVectorStore.MetadataField.text(fieldName);
                }))
                .observationRegistry(getObservationRegistry().getObject())
                .customObservationConvention(getCustomObservationConvention().getObject())
                .batchingStrategy(getBatchingStrategy())
                .build();
        // 初始化索引
        redisVectorStore.afterPropertiesSet();
        return redisVectorStore;
    }

    /**
     * 参考 {@link MilvusVectorStoreAutoConfiguration} 的 vectorStore 方法
     */
    @SneakyThrows
    private MilvusVectorStore buildMilvusVectorStore(EmbeddingModel embeddingModel) {
        MilvusVectorStoreAutoConfiguration configuration = new MilvusVectorStoreAutoConfiguration();
        // 获取配置属性
        MilvusVectorStoreProperties serverProperties = SpringUtil.getBean(MilvusVectorStoreProperties.class);
        MilvusServiceClientProperties clientProperties = SpringUtil.getBean(MilvusServiceClientProperties.class);

        // 创建 MilvusServiceClient 对象
        MilvusServiceClient milvusClient = configuration.milvusClient(serverProperties, clientProperties,
                new MilvusServiceClientConnectionDetails() {

                    @Override
                    public String getHost() {
                        return clientProperties.getHost();
                    }

                    @Override
                    public int getPort() {
                        return clientProperties.getPort();
                    }

                }
        );
        // 创建 MilvusVectorStore 对象
        MilvusVectorStore vectorStore = configuration.vectorStore(milvusClient, embeddingModel, serverProperties,
                getBatchingStrategy(), getObservationRegistry(), getCustomObservationConvention());

        // 初始化索引
        vectorStore.afterPropertiesSet();
        return vectorStore;
    }

    private static ObjectProvider<ObservationRegistry> getObservationRegistry() {
        return new ObjectProvider<>() {

            @Override
            public ObservationRegistry getObject() throws BeansException {
                return SpringUtil.getBean(ObservationRegistry.class);
            }

        };
    }

    private static ObjectProvider<VectorStoreObservationConvention> getCustomObservationConvention() {
        return new ObjectProvider<>() {

            @Override
            public VectorStoreObservationConvention getObject() throws BeansException {
                return new DefaultVectorStoreObservationConvention();
            }

        };
    }

    private static BatchingStrategy getBatchingStrategy() {
        return SpringUtil.getBean(BatchingStrategy.class);
    }

    private static ToolCallingManager getToolCallingManager() {
        return SpringUtil.getBean(ToolCallingManager.class);
    }

    private static ObjectProvider<EmbeddingModelObservationConvention> getEmbeddingModelObservationConvention() {
        return new ObjectProvider<>() {

            @Override
            public EmbeddingModelObservationConvention getObject() throws BeansException {
                return SpringUtil.getBean(EmbeddingModelObservationConvention.class);
            }

        };
    }

}
