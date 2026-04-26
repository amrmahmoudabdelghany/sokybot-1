package org.sokybot.behaviors.social.webhook;

import java.util.LinkedHashMap;
import java.util.Map;

import org.sokybot.social.api.SocialAlert;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Pure JSON payload builders for outbound webhooks (Jackson).
 */
public final class WebhookPayloadBuilder {

    private static final int DISCORD_COLOR_ALERT = 15158332;
    private static final int DISCORD_COLOR_HIVE_DISPATCHED = 0xD4AF37;
    private static final int DISCORD_COLOR_HIVE_COMPLETED = 0x2ECC71;
    private static final int DISCORD_COLOR_HIVE_ABORTED = 0xE74C3C;

    private WebhookPayloadBuilder() {
    }

    public static String discord(ObjectMapper mapper, SocialAlert alert) throws JsonProcessingException {
        ObjectNode root = mapper.createObjectNode();
        root.put("username", "Sokybot");
        ArrayNode embeds = root.putArray("embeds");
        ObjectNode embed = embeds.addObject();
        embed.put("title", titleFor(alert));
        embed.put("description", bodyDescription(alert));
        embed.put("color", colorFor(alert));
        return mapper.writeValueAsString(root);
    }

    public static String telegram(ObjectMapper mapper, SocialAlert alert, String chatId) throws JsonProcessingException {
        ObjectNode root = mapper.createObjectNode();
        root.put("chat_id", chatId == null ? "" : chatId);
        root.put("parse_mode", "Markdown");
        root.put("text", telegramMarkdown(alert));
        return mapper.writeValueAsString(root);
    }

    public static String genericJson(ObjectMapper mapper, SocialAlert alert) throws JsonProcessingException {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("machineId", alert.getMachineId());
        map.put("timestampEpochMs", alert.getTimestampEpochMs());
        map.put("kind", alert.getKind() != null ? alert.getKind().name() : null);
        map.put("subject", alert.getSubject());
        map.put("attributes", alert.getAttributes());
        return mapper.writeValueAsString(map);
    }

    private static String titleFor(SocialAlert alert) {
        if (alert.getKind() == null) {
            return "Alert";
        }
        switch (alert.getKind()) {
            case HIVE_DISPATCHED:
                return ":dart: HIVE_DISPATCHED";
            case HIVE_COMPLETED:
                return ":crossed_swords: HIVE_COMPLETED";
            case HIVE_ABORTED:
                return ":x: HIVE_ABORTED";
            default:
                return alert.getKind().name();
        }
    }

    private static int colorFor(SocialAlert alert) {
        if (alert.getKind() == null) {
            return DISCORD_COLOR_ALERT;
        }
        switch (alert.getKind()) {
            case HIVE_DISPATCHED:
                return DISCORD_COLOR_HIVE_DISPATCHED;
            case HIVE_COMPLETED:
                return DISCORD_COLOR_HIVE_COMPLETED;
            case HIVE_ABORTED:
                return DISCORD_COLOR_HIVE_ABORTED;
            default:
                return DISCORD_COLOR_ALERT;
        }
    }

    private static String bodyDescription(SocialAlert alert) {
        StringBuilder sb = new StringBuilder();
        sb.append(alert.getSubject());
        if (alert.getAttributes() != null && !alert.getAttributes().isEmpty()) {
            sb.append("\n\n");
            alert.getAttributes().forEach((k, v) -> sb.append('`').append(k).append("`: ").append(v).append('\n'));
        }
        return sb.toString();
    }

    private static String telegramMarkdown(SocialAlert alert) {
        String title = "*" + escapeMd(titleFor(alert)) + "*";
        String subj = escapeMd(alert.getSubject());
        String machine = escapeMd(alert.getMachineId());
        return title + "\n" + subj + "\n_" + machine + "_";
    }

    private static String escapeMd(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("\\", "\\\\")
                .replace("_", "\\_")
                .replace("*", "\\*")
                .replace("[", "\\[");
    }
}
