package com.mindvault.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mindvault.agent.config.AgentConfig;
import com.mindvault.agent.tool.Tool;
import com.mindvault.model.ModelConfigService;
import com.mindvault.model.entity.ModelConfig;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentService {

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final ModelConfigService modelConfigService;
    private final AgentConfig agentConfig;
    private final List<Tool> tools;
    private final ObjectMapper objectMapper;

    private RestClient restClient;
    private AgentConfig.LlmEndpoint endpoint;
    private String systemPrompt;

    public AgentService(ModelConfigService modelConfigService,
                        AgentConfig agentConfig,
                        List<Tool> tools) {
        this.modelConfigService = modelConfigService;
        this.agentConfig = agentConfig;
        this.tools = tools;
        this.objectMapper = new ObjectMapper();
    }

    @PostConstruct
    public void init() {
        try {
            ModelConfig primaryConfig = modelConfigService.getPrimaryChatModel();
            endpoint = agentConfig.buildEndpoint(primaryConfig);

            restClient = RestClient.builder()
                    .baseUrl(endpoint.getFullUrl())
                    .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .defaultHeader("Authorization", "Bearer " + endpoint.getApiKey())
                    .build();

            systemPrompt = buildSystemPrompt();

            log.info("AgentService 初始化完成，主模型: {}/{}",
                    primaryConfig.getProvider(), primaryConfig.getModelName());
        } catch (Exception e) {
            log.warn("AgentService 初始化失败（未配置主模型）: {}", e.getMessage());
        }
    }

    private String buildSystemPrompt() {
        StringBuilder sb = new StringBuilder();
        sb.append("你是 MindVault（知忆）AI 助手，一个个人知识库 Agent。\n\n");
        sb.append("你可以使用以下工具来帮助用户管理知识库：\n");

        for (Tool tool : tools) {
            sb.append("- ").append(tool.getName()).append(": ").append(tool.getDescription()).append("\n");
        }

        sb.append("\n当你需要使用工具时，请按以下格式返回：\n");
        sb.append("[TOOL_CALL]\n");
        sb.append("name: 工具名称\n");
        sb.append("args: {\"key\": \"value\"}\n");
        sb.append("[END_TOOL_CALL]\n\n");
        sb.append("请用中文回复用户。");

        return sb.toString();
    }

    public String processMessage(String userMessage) {
        if (restClient == null) {
            return "系统未配置主模型，请先在设置中添加并设置主模型。";
        }

        try {
            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> systemMsg = new LinkedHashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            Map<String, String> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);

            String response = callLlm(messages);
            String toolResult = executeToolCall(response);

            if (toolResult != null) {
                Map<String, String> assistantMsg = new LinkedHashMap<>();
                assistantMsg.put("role", "assistant");
                assistantMsg.put("content", response);
                messages.add(assistantMsg);

                Map<String, String> toolMsg = new LinkedHashMap<>();
                toolMsg.put("role", "user");
                toolMsg.put("content", "工具执行结果: " + toolResult);
                messages.add(toolMsg);

                return callLlm(messages);
            }

            return response;
        } catch (Exception e) {
            log.error("Agent 处理消息失败: {}", e.getMessage(), e);
            return "抱歉，处理您的消息时遇到了问题，请稍后重试。";
        }
    }

    private String callLlm(List<Map<String, String>> messages) throws Exception {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", endpoint.getModelName());
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        String responseJson = restClient.post()
                .body(jsonBody)
                .retrieve()
                .body(String.class);

        Map<?, ?> responseMap = objectMapper.readValue(responseJson, Map.class);

        if (responseMap.containsKey("choices")) {
            List<?> choices = (List<?>) responseMap.get("choices");
            if (!choices.isEmpty()) {
                Map<?, ?> choice = (Map<?, ?>) choices.get(0);
                Map<?, ?> message = (Map<?, ?>) choice.get("message");
                return (String) message.get("content");
            }
        }

        if (responseMap.containsKey("message")) {
            Map<?, ?> message = (Map<?, ?>) responseMap.get("message");
            return (String) message.get("content");
        }

        return responseJson;
    }

    private String executeToolCall(String response) {
        Pattern pattern = Pattern.compile(
                "\\[TOOL_CALL\\]\\s*name:\\s*(\\S+)\\s*args:\\s*(\\{.*?\\})\\s*\\[END_TOOL_CALL\\]",
                Pattern.DOTALL);
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String toolName = matcher.group(1).trim();
            String argsJson = matcher.group(2).trim();

            for (Tool tool : tools) {
                if (tool.getName().equals(toolName)) {
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> args = objectMapper.readValue(argsJson, Map.class);
                        return tool.execute(args);
                    } catch (Exception e) {
                        log.error("执行工具 {} 失败: {}", toolName, e.getMessage());
                        return "工具执行失败: " + e.getMessage();
                    }
                }
            }
            return "未知工具: " + toolName;
        }

        return null;
    }

    public List<Tool> getTools() {
        return tools;
    }
}