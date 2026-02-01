package org.sokybot.webview.handler;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.sokybot.runtime.template.ITemplateManager;
import org.sokybot.runtime.template.MachineTemplate;
import org.sokybot.webview.api.IRSocketHandler;
import org.sokybot.webview.api.RSocketRequest;
import org.sokybot.webview.api.RSocketResponse;
import org.sokybot.webview.api.doc.RSocketMethod;
import org.sokybot.webview.api.doc.RSocketParam;

import reactor.core.publisher.Mono;

/**
 * RSocket handler for machine template operations.
 */
@Component(service = IRSocketHandler.class, property = {
        IRSocketHandler.METHOD_PROPERTY + "=template.list",
        IRSocketHandler.METHOD_PROPERTY + "=template.get",
        IRSocketHandler.METHOD_PROPERTY + "=template.create",
        IRSocketHandler.METHOD_PROPERTY + "=template.save",
        IRSocketHandler.METHOD_PROPERTY + "=template.delete",
        IRSocketHandler.METHOD_PROPERTY + "=template.apply",
        IRSocketHandler.METHOD_PROPERTY + "=template.export",
        IRSocketHandler.METHOD_PROPERTY + "=template.import"
})
@RSocketMethod(name = "template.list", description = "List all templates", returnType = "object")
@RSocketMethod(name = "template.get", description = "Get a specific template", params = {
        @RSocketParam(name = "id", description = "Template ID")
})
@RSocketMethod(name = "template.create", description = "Create template from machine", params = {
        @RSocketParam(name = "machineId", description = "Source machine ID"),
        @RSocketParam(name = "name", description = "Template name"),
        @RSocketParam(name = "description", required = false),
        @RSocketParam(name = "author", required = false)
})
@RSocketMethod(name = "template.save", description = "Save/update template", params = {
        @RSocketParam(name = "id", description = "Template ID"),
        @RSocketParam(name = "name", required = false),
        @RSocketParam(name = "description", required = false),
        @RSocketParam(name = "author", required = false)
})
@RSocketMethod(name = "template.delete", description = "Delete a template", params = {
        @RSocketParam(name = "id", description = "Template ID")
})
@RSocketMethod(name = "template.apply", description = "Apply template to machine", params = {
        @RSocketParam(name = "templateId", description = "Template ID"),
        @RSocketParam(name = "machineId", description = "Target machine ID")
})
public class TemplateHandler implements IRSocketHandler {

    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private volatile ITemplateManager templateManager;

    @Reference(cardinality = ReferenceCardinality.OPTIONAL, policy = ReferencePolicy.DYNAMIC)
    protected void setTemplateManager(ITemplateManager manager) {
        this.templateManager = manager;
    }

    protected void unsetTemplateManager(ITemplateManager manager) {
        this.templateManager = null;
    }

    @Override
    public String[] getMethods() {
        return new String[] {
                "template.list", "template.get", "template.create",
                "template.save", "template.delete", "template.apply",
                "template.export", "template.import"
        };
    }

    @Override
    public String getDescription() {
        return "Machine template management";
    }

    @Override
    public Mono<RSocketResponse> handle(RSocketRequest request) {
        String method = request.getMethod();

        switch (method) {
            case "template.list":
                return handleList(request);
            case "template.get":
                return handleGet(request);
            case "template.create":
                return handleCreate(request);
            case "template.save":
                return handleSave(request);
            case "template.delete":
                return handleDelete(request);
            case "template.apply":
                return handleApply(request);
            case "template.export":
                return handleExport(request);
            case "template.import":
                return handleImport(request);
            default:
                return Mono.just(RSocketResponse.methodNotFound(method));
        }
    }

    private Mono<RSocketResponse> handleList(RSocketRequest request) {
        if (templateManager == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Template manager not available"));
        }

        List<MachineTemplate> templates = templateManager.list();
        List<Map<String, Object>> templateList = new ArrayList<>();

        for (MachineTemplate template : templates) {
            templateList.add(templateToSummary(template));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("templates", templateList);
        response.put("count", templateList.size());

        return Mono.just(RSocketResponse.success(response));
    }

    private Mono<RSocketResponse> handleGet(RSocketRequest request) {
        if (templateManager == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Template manager not available"));
        }

        String id = request.getString("id");
        if (id == null || id.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: id"));
        }

        return templateManager.get(id)
                .map(template -> RSocketResponse.success(templateToMap(template)))
                .map(Mono::just)
                .orElse(Mono.just(RSocketResponse.error(
                        RSocketResponse.ErrorCode.NOT_FOUND,
                        "Template not found: " + id)));
    }

    private Mono<RSocketResponse> handleCreate(RSocketRequest request) {
        if (templateManager == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Template manager not available"));
        }

        String machineId = request.getString("machineId");
        String name = request.getString("name");

        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: machineId"));
        }
        if (name == null || name.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: name"));
        }

        try {
            MachineTemplate template = templateManager.createFromMachine(machineId, name);

            // Apply optional fields
            String description = request.getString("description");
            if (description != null) {
                template.setDescription(description);
            }
            String author = request.getString("author");
            if (author != null) {
                template.setAuthor(author);
            }

            templateManager.save(template);

            return Mono.just(RSocketResponse.success(templateToMap(template)));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INTERNAL_ERROR,
                    "Failed to create template: " + e.getMessage()));
        }
    }

    private Mono<RSocketResponse> handleSave(RSocketRequest request) {
        if (templateManager == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Template manager not available"));
        }

        String id = request.getString("id");
        String name = request.getString("name");

        if (id == null || id.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: id"));
        }

        return templateManager.get(id)
                .map(template -> {
                    // Update fields
                    if (name != null && !name.isEmpty()) {
                        template.setName(name);
                    }
                    String description = request.getString("description");
                    if (description != null) {
                        template.setDescription(description);
                    }
                    String author = request.getString("author");
                    if (author != null) {
                        template.setAuthor(author);
                    }

                    templateManager.save(template);

                    Map<String, Object> response = new HashMap<>();
                    response.put("id", template.getId());
                    response.put("saved", true);
                    return RSocketResponse.success(response);
                })
                .map(Mono::just)
                .orElse(Mono.just(RSocketResponse.error(
                        RSocketResponse.ErrorCode.NOT_FOUND,
                        "Template not found: " + id)));
    }

    private Mono<RSocketResponse> handleDelete(RSocketRequest request) {
        if (templateManager == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Template manager not available"));
        }

        String id = request.getString("id");
        if (id == null || id.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: id"));
        }

        boolean deleted = templateManager.delete(id);

        Map<String, Object> response = new HashMap<>();
        response.put("id", id);
        response.put("deleted", deleted);

        return Mono.just(RSocketResponse.success(response));
    }

    private Mono<RSocketResponse> handleApply(RSocketRequest request) {
        if (templateManager == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Template manager not available"));
        }

        String templateId = request.getString("templateId");
        String machineId = request.getString("machineId");

        if (templateId == null || templateId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: templateId"));
        }
        if (machineId == null || machineId.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: machineId"));
        }

        boolean applied = templateManager.applyToMachine(templateId, machineId);

        Map<String, Object> response = new HashMap<>();
        response.put("templateId", templateId);
        response.put("machineId", machineId);
        response.put("applied", applied);

        return Mono.just(RSocketResponse.success(response));
    }

    private Mono<RSocketResponse> handleExport(RSocketRequest request) {
        if (templateManager == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Template manager not available"));
        }

        String id = request.getString("id");
        if (id == null || id.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: id"));
        }

        return templateManager.exportToJson(id)
                .map(json -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("id", id);
                    response.put("json", json);
                    return RSocketResponse.success(response);
                })
                .map(Mono::just)
                .orElse(Mono.just(RSocketResponse.error(
                        RSocketResponse.ErrorCode.NOT_FOUND,
                        "Template not found: " + id)));
    }

    private Mono<RSocketResponse> handleImport(RSocketRequest request) {
        if (templateManager == null) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.SERVICE_UNAVAILABLE,
                    "Template manager not available"));
        }

        String json = request.getString("json");
        if (json == null || json.isEmpty()) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Missing required parameter: json"));
        }

        try {
            MachineTemplate template = templateManager.importFromJson(json);
            return Mono.just(RSocketResponse.success(templateToMap(template)));
        } catch (Exception e) {
            return Mono.just(RSocketResponse.error(
                    RSocketResponse.ErrorCode.INVALID_PARAMS,
                    "Failed to import template: " + e.getMessage()));
        }
    }

    private Map<String, Object> templateToSummary(MachineTemplate template) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", template.getId());
        map.put("name", template.getName());
        if (template.getDescription() != null) {
            map.put("description", template.getDescription());
        }
        if (template.getAuthor() != null) {
            map.put("author", template.getAuthor());
        }
        map.put("updatedAt", TIMESTAMP_FORMATTER.format(template.getUpdatedAt()));
        return map;
    }

    private Map<String, Object> templateToMap(MachineTemplate template) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", template.getId());
        map.put("name", template.getName());
        map.put("description", template.getDescription());
        map.put("author", template.getAuthor());
        map.put("createdAt", TIMESTAMP_FORMATTER.format(template.getCreatedAt()));
        map.put("updatedAt", TIMESTAMP_FORMATTER.format(template.getUpdatedAt()));
        map.put("settings", template.getSettings());
        map.put("enabledActuators", template.getEnabledActuators());
        map.put("disabledActuators", template.getDisabledActuators());
        map.put("metadata", template.getMetadata());
        return map;
    }
}
