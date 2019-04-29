package io.slingr.endpoints.jira.services;

import io.slingr.endpoints.utils.Json;

import java.util.HashMap;
import java.util.Map;

/**
 * Keeps a cache of fields in JIRA in case we need to get information about them. This is
 * especially useful for custom fields in order to get the name and type.
 *
 * Created by dgaviola on 4/6/15.
 */
public class FieldsCache {
    private JiraApi jiraApi;
    private Map<String, Map<String, Object>> fieldsById = new HashMap<>();
    private Map<String, Map<String, Object>> fieldsByName = new HashMap<>();

    public FieldsCache(JiraApi jiraApi) {
        this.jiraApi = jiraApi;
    }

    public void refresh() {
        Json json = jiraApi.findFields();
        for (Object fieldObj : json.toList()) {
            Json field = (Json) fieldObj;
            Map<String, Object> fieldMap = fieldsById.get(field.string("id"));
            if (fieldMap == null) {
                fieldMap = new HashMap<>();
                fieldsById.put(field.string("id"), fieldMap);
                fieldsByName.put(field.string("name"), fieldMap);
            }
            fieldMap.put("id", field.string("id"));
            fieldMap.put("name", field.string("name"));
            if (field.contains("schema")) {
                if ("array".equals(field.json("schema").string("type"))) {
                    fieldMap.put("array", true);
                    fieldMap.put("type", field.json("schema").string("items"));
                } else {
                    fieldMap.put("array", false);
                    fieldMap.put("type", field.json("schema").string("type"));
                }
            }
        }
    }

    public String getCustomFieldId(String name) {
        Map<String, Object> field = getFieldByName(name);
        if (field != null) {
            return (String) field.get("id");
        }
        return null;
    }

    public String getCustomFieldType(String id) {
        Map<String, Object> field = getFieldById(id);
        if (field != null) {
            return (String) field.get("type");
        }
        return null;
    }

    public String getCustomFieldName(String id) {
        Map<String, Object> field = getFieldById(id);
        if (field != null) {
            return (String) field.get("name");
        }
        return null;
    }

    public boolean isCustomFieldArray(String id) {
        Map<String, Object> field = getFieldById(id);
        if (field != null) {
            return (boolean) field.get("array");
        }
        return false;
    }

    private Map<String, Object> getFieldById(String id) {
        Map<String, Object> field = fieldsById.get(id);
        if (field == null) {
            refresh();
            field = fieldsById.get(id);
            if (field == null) {
                return null;
            }
        }
        return field;
    }

    private Map<String, Object> getFieldByName(String name) {
        // in this case we won't refresh because due to the usage we make of this method it will
        // be too inefficient
        return fieldsByName.get(name);
    }
}
