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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Calls any OpenAI-compatible chat-completions endpoint with vision support.
 * Only the review text or a downscaled thumbnail ever leaves the server; secrets stay server-side.
 */
@Service
public class OpenAiCompatibleProvider implements AiProvider {
    static final String REVIEW_SYSTEM = """
            你是校园内容（食堂点评、社区动态、回复）的合规审核助手，只做风险分诊，不做最终判定。
            只返回 JSON 对象，不要输出其他文字，字段如下：
            {"risk":"LOW|MEDIUM|HIGH","confidence":0.0-1.0之间的数字,"labels":["标签"],"reason":"一句话中文说明"}
            只判断是否合规，不要求内容必须与食堂或菜品相关；与食堂无关但无害的测试、闲聊等内容可以判 LOW。
            HIGH：色情、暴力、政治敏感、广告引流、人身攻击、违法信息。
            MEDIUM：疑似软广/引流、阴阳怪气、低俗但不违法、其他需要人工确认的合规风险。
            LOW：合规内容（包括与食堂无关但无害的内容）。
            labels 可用：色情、暴力、政治敏感、广告、人身攻击、违法、低俗，其他情况用空数组。""";
    static final String IMAGE_SYSTEM = """
            你是校园内容图片的合规审核助手，只做风险分诊，不做最终判定。
            只返回 JSON 对象，不要输出其他文字，字段如下：
            {"risk":"LOW|MEDIUM|HIGH","confidence":0.0-1.0之间的数字,"labels":["标签"],"reason":"一句话中文说明"}
            只判断是否合规，不要求图片是实拍菜品照片：插画、图标、表情包、卡通图、截图等，只要不含违规内容都可以判 LOW。
            HIGH：色情、暴力、政治敏感、明显广告水印或二维码、违法信息。
            MEDIUM：疑似软广/引流、令人不适但不违法、其他需要人工确认的合规风险。
            LOW：合规图片（实拍、插画、图标、表情包、卡通图等均可）。
            labels 可用：色情、暴力、政治敏感、广告、违法、不适，其他情况用空数组。""";
    static final String IMAGE_USER = "判断这张图片是否含有违规内容，只看是否合规；不要求是实拍菜品照片。";

    private static final Logger LOG = LoggerFactory.getLogger(OpenAiCompatibleProvider.class);
    private final AiTriageProperties properties;
    private final ObjectMapper json = new ObjectMapper();
    private final HttpClient http;
    private final AtomicInteger keyCursor = new AtomicInteger();

    /** A key the server refused: dead, wrong scope or per-key throttled. The next key is worth one try. */
    static final class KeyRejectedException extends AiUnavailableException {
        KeyRejectedException(int status) {
            super("AI服务返回" + status);
        }
    }

    public OpenAiCompatibleProvider(AiTriageProperties properties) {
        this.properties = properties;
        this.http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    }

    @Override
    public AiVerdict checkReview(String model, int rating, String content) throws AiUnavailableException {
        ObjectNode user = json.createObjectNode();
        user.put("role", "user");
        user.put("content", "评分：" + rating + " / 5，正文：" + content);
        return complete(model, REVIEW_SYSTEM, user);
    }

    @Override
    public AiVerdict checkText(String model, String content) throws AiUnavailableException {
        ObjectNode user = json.createObjectNode();
        user.put("role", "user");
        user.put("content", "社区动态正文：" + content);
        return complete(model, REVIEW_SYSTEM, user);
    }

    @Override
    public AiVerdict checkImage(String model, byte[] image, String contentType) throws AiUnavailableException {
        ObjectNode user = json.createObjectNode();
        user.put("role", "user");
        ArrayNode parts = user.putArray("content");
        ObjectNode text = parts.addObject();
        text.put("type", "text");
        text.put("text", IMAGE_USER);
        ObjectNode picture = parts.addObject();
        picture.put("type", "image_url");
        ObjectNode url = picture.putObject("image_url");
        String mime = contentType != null && contentType.equals("image/png") ? "image/png" : "image/jpeg";
        url.put("url", "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(image));
        return complete(model, IMAGE_SYSTEM, user);
    }

    private AiVerdict complete(String model, String system, ObjectNode userMessage) throws AiUnavailableException {
        List<String> keys = splitKeys(properties.apiKey());
        if (keys.isEmpty()) throw new AiUnavailableException("AI服务暂时不可用");
        AiUnavailableException lastRejection = null;
        for (int attempt = 0; attempt < keys.size(); attempt++) {
            String key = keys.get(Math.floorMod(keyCursor.getAndIncrement(), keys.size()));
            try {
                return callOnce(model, system, userMessage, key);
            } catch (KeyRejectedException rejected) {
                LOG.warn("AI key rejected, rotating to next key ({}/{})", attempt + 1, keys.size());
                lastRejection = rejected;
            }
        }
        throw lastRejection;
    }

    /** Splits the comma-separated key list; blanks and surrounding spaces are ignored. */
    static List<String> splitKeys(String apiKey) {
        if (apiKey == null) return List.of();
        return Arrays.stream(apiKey.split(",")).map(String::strip).filter(s -> !s.isEmpty()).toList();
    }

    private AiVerdict callOnce(String model, String system, ObjectNode userMessage, String key) throws AiUnavailableException {
        try {
            ObjectNode body = json.createObjectNode();
            body.put("model", model);
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
            int status = response.statusCode();
            if (status == 401 || status == 403 || status == 429) throw new KeyRejectedException(status);
            if (status != 200) throw new AiUnavailableException("AI服务返回" + status);
            String text = json.readTree(response.body()).path("choices").path(0).path("message").path("content").asText("");
            return parseVerdict(text, model);
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
