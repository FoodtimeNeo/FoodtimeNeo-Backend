package com.FoodtimeNeo.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.FoodtimeNeo.ai.dto.AiVerdict;
import com.FoodtimeNeo.config.properties.AiTriageProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Calls any OpenAI-compatible chat-completions endpoint with vision support.
 * Only the review text or a downscaled thumbnail ever leaves the server; secrets stay server-side.
 */
@Service
public class OpenAiCompatibleProvider implements AiProvider {
    static final String REVIEW_SYSTEM = """
            你是校园食堂点评的内容审核助手，只做风险分诊，不做最终判定。
            只返回 JSON 对象，不要输出其他文字，字段如下：
            {"risk":"LOW|MEDIUM|HIGH","confidence":0.0-1.0之间的数字,"labels":["标签"],"reason":"一句话中文说明"}
            HIGH：色情、暴力、政治敏感、广告引流、人身攻击、违法信息。
            MEDIUM：疑似广告、阴阳怪气、与菜品无关但无害。
            LOW：正常点评。labels 可用：色情、暴力、政治敏感、广告、人身攻击、违法、无关、低俗，其他情况用空数组。""";
    static final String IMAGE_SYSTEM = """
            你是校园食堂菜品图片的内容审核助手，只做风险分诊，不做最终判定。
            只返回 JSON 对象，不要输出其他文字，字段如下：
            {"risk":"LOW|MEDIUM|HIGH","confidence":0.0-1.0之间的数字,"labels":["标签"],"reason":"一句话中文说明"}
            HIGH：色情、暴力、政治敏感、明显广告水印或二维码。
            MEDIUM：疑似非食物、模糊难辨、与菜品弱相关。
            LOW：正常菜品实拍。labels 可用：色情、暴力、政治敏感、广告、非食物、模糊，其他情况用空数组。""";

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);
    private final AiTriageProperties properties;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http;

    public OpenAiCompatibleProvider(AiTriageProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public AiVerdict checkReview(int rating, String content) throws AiUnavailableException {
        ObjectNode user = json.createObjectNode();
        user.put("role", "user");
        user.put("content", "评分：" + rating + " / 5，正文：" + content);
        return complete(REVIEW_SYSTEM, user);
    }

    @Override
    public AiVerdict checkImage(byte[] image, String contentType) throws AiUnavailableException {
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
        return complete(IMAGE_SYSTEM, user);
    }

    private AiVerdict complete(String system, ObjectNode userMessage) throws AiUnavailableException {
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
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                    .build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) throw new AiUnavailableException("AI服务返回" + response.statusCode());
            String text = json.readTree(response.body()).path("choices").path(0).path("message").path("content").asText("");
            return parseVerdict(text, properties.model());
        } catch (AiUnavailableException exception) {
            throw exception;
        } catch (Exception failure) {
            LOG.error("AI triage call failed: {}", failure.getClass().getSimpleName());
            throw new AiUnavailableException("AI服务暂时不可用");
        }
    }

    /** Parses the model answer leniently; anything unusable is a caller-side error, not a verdict. */
    static AiVerdict parseVerdict(String answer, String model) throws AiUnavailableException {
        try {
            int begin = answer.indexOf('{');
            int end = answer.lastIndexOf('}');
            if (begin < 0 || end <= begin) throw new IllegalArgumentException("no JSON object");
            JsonNode root = new ObjectMapper().readTree(answer.substring(begin, end + 1));
            String risk = root.path("risk").asText("").strip().toUpperCase(java.util.Locale.ROOT);
            double confidence = root.path("confidence").asDouble(Double.NaN);
            List<String> labels = new ArrayList<>();
            JsonNode rawLabels = root.path("labels");
            if (rawLabels.isArray()) {
                for (JsonNode label : rawLabels) {
                    String text = label.asText("").strip();
                    if (!text.isEmpty()) labels.add(text);
                }
            }
            String reason = root.path("reason").asText("").strip();
            if ((!risk.equals("LOW") && !risk.equals("MEDIUM") && !risk.equals("HIGH"))
                    || Double.isNaN(confidence) || reason.isEmpty()) {
                throw new IllegalArgumentException("incomplete verdict");
            }
            return new AiVerdict(risk, confidence, labels, reason, model);
        } catch (AiUnavailableException exception) {
            throw exception;
        } catch (Exception failure) {
            throw new AiUnavailableException("AI返回无法解析");
        }
    }
}
