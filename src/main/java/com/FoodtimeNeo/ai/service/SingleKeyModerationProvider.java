package com.FoodtimeNeo.ai.service;

import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.config.properties.SingleModerationProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

/**
 * 单 key 直判：同一个 key 同一个模型处理全部审核，一次判完。
 * 无 key 轮询、无升级复核、无兜底、无 Redis 模型映射；调用失败直接报不可用，
 * 由扫批稍后重试或转人工。与 OpenAiCompatibleProvider 共用提示词与 verdict 解析，
 * 旧链路代码保留，切回时只需关闭 app.ai.single.enabled。
 */
@Service
public class SingleKeyModerationProvider implements AiProvider {
    private static final Logger LOG = LoggerFactory.getLogger(SingleKeyModerationProvider.class);
    private final SingleModerationProperties properties;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http;

    public SingleKeyModerationProvider(SingleModerationProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public AiVerdict checkReview(String model, int rating, String content) throws AiUnavailableException {
        ObjectNode user = json.createObjectNode();
        user.put("role", "user");
        user.put("content", "评分：" + rating + " / 5，正文：" + content);
        return complete(OpenAiCompatibleProvider.REVIEW_SYSTEM, user);
    }

    @Override
    public AiVerdict checkImage(String model, byte[] image, String contentType) throws AiUnavailableException {
        ObjectNode user = json.createObjectNode();
        user.put("role", "user");
        ArrayNode parts = user.putArray("content");
        ObjectNode text = parts.addObject();
        text.put("type", "text");
        text.put("text", "判断这张食堂菜品图片是否有风险，只看图片内容。");
        ObjectNode picture = parts.addObject();
        picture.put("type", "image_url");
        ObjectNode url = picture.putObject("image_url");
        String mime = contentType != null && contentType.equals("image/png") ? "image/png" : "image/jpeg";
        url.put("url", "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(image));
        return complete(OpenAiCompatibleProvider.IMAGE_SYSTEM, user);
    }

    /** 唯一 key 唯一模型；入参 model 被忽略，调用方传什么都不影响实际使用的模型。 */
    private AiVerdict complete(String system, ObjectNode userMessage) throws AiUnavailableException {
        String key = properties.apiKey() == null ? "" : properties.apiKey().strip();
        if (key.isEmpty() || key.contains(",")) throw new AiUnavailableException("AI服务暂时不可用");
        try {
            ObjectNode body = json.createObjectNode();
            body.put("model", properties.model());
            body.put("temperature", 0);
            body.put("max_tokens", 400);
            ArrayNode messages = body.putArray("messages");
            ObjectNode systemMessage = messages.addObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", system);
            messages.add(userMessage);
            HttpRequest request = HttpRequest.newBuilder(URI.create(properties.baseUrl() + "/chat/completions"))
                    .timeout(properties.timeout())
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + key)
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new AiUnavailableException("AI服务返回" + response.statusCode());
            String text = json.readTree(response.body()).path("choices").path(0).path("message").path("content").asText("");
            return OpenAiCompatibleProvider.parseVerdict(text, properties.model());
        } catch (AiUnavailableException exception) {
            throw exception;
        } catch (Exception failure) {
            LOG.error("AI single-key call failed: {}", failure.getClass().getSimpleName());
            throw new AiUnavailableException("AI服务暂时不可用");
        }
    }
}
