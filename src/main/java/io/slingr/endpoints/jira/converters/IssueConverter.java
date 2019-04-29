package io.slingr.endpoints.jira.converters;

import io.slingr.endpoints.jira.services.FieldsCache;
import io.slingr.endpoints.utils.Json;

import java.math.BigDecimal;
import java.util.List;

/**
 * Converts between the different formats for an issue.
 *
 * Created by dgaviola on 4/6/15.
 */
public class IssueConverter {
    private FieldsCache fieldsCache;

    public IssueConverter(FieldsCache fieldsCache) {
        this.fieldsCache = fieldsCache;
    }

    /**
     * Converts each issue from the search result using {@link #fromJiraToApp(Json)}.
     *
     * @param searchResult the result of a query to JIRA
     * @return the list of issues in application format
     */
    public Json searchResultFromJiraToApp(Json searchResult) {
        if (searchResult == null) {
            return null;
        }
        Json json = Json.map();
        json.set("total", searchResult.integer("total"));
        Json list = Json.list();
        for (Json issue : searchResult.jsons("issues")) {
            list.push(fromJiraToApp(issue));
        }
        json.set("items", list);
        return json;
    }

    /**
     * Converts the issue from JIRA into a format more friendly for application. Also removes some
     * information that probably it isn't needed in most cases.
     *
     * @param jiraIssue the JSON of the issue comming from JIRA
     * @return the JSON of the issue for application
     */
    public Json fromJiraToApp(Json jiraIssue) {
        Json issue = Json.map();
        issue.set("id", jiraIssue.string("id"));
        issue.set("key", jiraIssue.string("key"));
        Json fields = jiraIssue.json("fields");
        issue.set("issueType", convertFieldValueToApp(fields.json("issuetype"), "issuetype"));
        if (fields.contains("issuetype") && fields.json("issuetype").bool("subtask", false)) {
            issue.set("subTask", true);
        } else {
            issue.set("subTask", false);
        }
        issue.set("project", convertFieldValueToApp(fields.json("project"), "project"));
        issue.set("timeSpent", TimeUtils.parseSeconds(fields.integer("timespent")));
        issue.set("aggregateTimeSpent", TimeUtils.parseSeconds(fields.integer("aggregatetimespent")));
        issue.set("timeEstimate", TimeUtils.parseSeconds(fields.integer("timeestimate")));
        issue.set("aggregateTimeEstimate", TimeUtils.parseSeconds(fields.integer("aggregatetimeestimate")));
        issue.set("timeOriginalEstimate", TimeUtils.parseSeconds(fields.integer("timeoriginalestimate")));
        issue.set("aggregateOriginalTimeEstimate", TimeUtils.parseSeconds(fields.integer("aggregateoriginaltimeestimate")));
        if (fields.contains("timetracking")) {
            issue.set("remainingEstimate", TimeUtils.parseSeconds(fields.json("timetracking").integer("remainingEstimateSeconds")));
        } else {
            issue.set("remainingEstimate", 0l);
        }
        if (fields.contains("progress")) {
            Integer percent = fields.json("progress").integer("percent");
            if (percent == null) percent = 0;
            issue.set("progress", Json.map()
                    .set("progress", TimeUtils.parseSeconds(fields.json("progress").integer("progress")))
                    .set("total", TimeUtils.parseSeconds(fields.json("progress").integer("total")))
                    .set("percent", new BigDecimal(percent).divide(BigDecimal.valueOf(100)))
            );
        } else {
            issue.set("progress", Json.map()
                            .set("progress", 0l)
                            .set("total", 0l)
                            .set("percent", 0l)
            );
        }
        if (fields.contains("aggregateprogress")) {
            Integer percent = fields.json("aggregateprogress").integer("percent");
            if (percent == null) percent = 0;
            issue.set("aggregateProgress", Json.map()
                            .set("progress", TimeUtils.parseSeconds(fields.json("aggregateprogress").integer("progress")))
                            .set("total", TimeUtils.parseSeconds(fields.json("aggregateprogress").integer("total")))
                            .set("percent", new BigDecimal(percent).divide(BigDecimal.valueOf(100)))
            );
        } else {
            issue.set("aggregateProgress", Json.map()
                            .set("progress", 0l)
                            .set("total", 0l)
                            .set("percent", 0l)
            );
        }
        issue.set("versions", convertManyFieldValueToApp(fields.jsons("versions"), "version"));
        issue.set("fixVersions", convertManyFieldValueToApp(fields.jsons("fixVersions"), "version"));
        issue.set("status", convertFieldValueToApp(fields.json("status"), "status"));
        issue.set("created", TimeUtils.parseJiraDate(fields.string("created")));
        issue.set("updated", TimeUtils.parseJiraDate(fields.string("updated")));
        issue.set("dueDate", fields.string("duedate"));
        issue.set("resolution", convertFieldValueToApp(fields.json("resolution"), "resolution"));
        issue.set("resolutionDate", TimeUtils.parseJiraDate(fields.string("resolutiondate")));
        issue.set("priority", convertFieldValueToApp(fields.json("priority"), "priority"));
        issue.set("labels", fields.objects("labels"));
        issue.set("issueLinks", issueLinks(fields.jsons("issuelinks")));
        issue.set("components", convertManyFieldValueToApp(fields.jsons("components"), "component"));
        issue.set("environment", fields.string("environment"));
        if (fields.contains("votes")) {
            issue.set("votes", fields.json("votes").integer("votes"));
        } else {
            issue.set("votes", 0);
        }
        issue.set("assignee", convertFieldValueToApp(fields.json("assignee"), "user"));
        issue.set("reporter", convertFieldValueToApp(fields.json("assignee"), "user"));
        issue.set("creator", convertFieldValueToApp(fields.json("assignee"), "user"));
        issue.set("summary", fields.string("summary"));
        issue.set("descriptionHtml", TextConverter.convertWikiToHtml(fields.string("description")));
        issue.set("descriptionText", TextConverter.convertWikiToText(fields.string("description")));
        issue.set("descriptionWiki", fields.string("description"));
        issue.set("parent", issueRef(fields.json("parent")));
        issue.set("subTasks", issueRefs(fields.jsons("subtasks")));
        issue.set("customFields", getCustomFields(fields));
        if (fields.contains("comment") && fields.json("comment").contains("comments")) {
            Json comments = Json.list();
            for (Json jiraComment : fields.json("comment").jsons("comments")) {
                comments.push(commentFromJiraToApp(jiraComment));
            }
            issue.set("comments", comments);
        }
        if (fields.contains("worklog") && fields.json("worklog").contains("worklogs")) {
            Json workLogs = Json.list();
            for (Json jiraWorkLog : fields.json("worklog").jsons("worklogs")) {
                workLogs.push(workLogFromJiraToApp(jiraWorkLog));
            }
            issue.set("workLogs", workLogs);
        }
        if (fields.contains("attachment")) {
            Json attachments = Json.list();
            for (Json jiraAttachment : fields.jsons("attachment")) {
                attachments.push(attachmentFromJiraToApp(jiraAttachment));
            }
            issue.set("attachments", attachments);
        }
        return issue;
    }

    /**
     * Converts an issue from application edit/create format to JIRA format.
     *
     * @param i2Issue the JSON of the issue in application edit/create format
     * @return the JSON in JIRA format
     */
    public Json fromAppToJira(Json i2Issue) {
        Json issue = Json.map();
        Json fields = Json.map();
        if (i2Issue.contains("key")) {
            issue.set("key", i2Issue.string("key"));
        }
        if (i2Issue.contains("project")) {
            fields.set("project", Json.map().set("key", i2Issue.string("project")));
        }
        if (i2Issue.contains("issueType")) {
            fields.set("issuetype", Json.map().set("name", i2Issue.string("issueType")));
        }
        if (i2Issue.contains("summary")) {
            fields.set("summary", i2Issue.string("summary"));
        }
        String descriptionFormat = i2Issue.string("descriptionFormat");
        String description;
        if ("html".equalsIgnoreCase(descriptionFormat)) {
            description = TextConverter.convertHtmlToWiki(i2Issue.string("description"));
        } else if ("wiki".equalsIgnoreCase(descriptionFormat)) {
            description = i2Issue.string("description");
        } else {
            description = TextConverter.convertTextToWiki(i2Issue.string("description"));
        }
        if (i2Issue.contains("description")) {
            fields.set("description", description);
        }
        if (i2Issue.contains("dueDate")) {
            fields.set("duedate", i2Issue.string("dueDate"));
        }
        if (i2Issue.contains("assignee")) {
            fields.set("assignee", Json.map().set("name", i2Issue.string("assignee")));
        }
        if (i2Issue.contains("reporter")) {
            fields.set("reporter", Json.map().set("name", i2Issue.string("reporter")));
        }
        if (i2Issue.contains("labels")) {
            fields.set("labels", i2Issue.strings("labels"));
        }
        if (i2Issue.contains("priority")) {
            fields.set("priority", Json.map().set("name", i2Issue.string("priority")));
        }
        if (i2Issue.contains("versions")) {
            Json jiraVersions = Json.list();
            for (String versionName : i2Issue.strings("versions")) {
                jiraVersions.push(Json.map().set("name", versionName));
            }
            fields.set("versions", jiraVersions);
        }
        if (i2Issue.contains("components")) {
            Json jiraComponents = Json.list();
            for (String componentName : i2Issue.strings("components")) {
                jiraComponents.push(Json.map().set("name", componentName));
            }
            fields.set("components", jiraComponents);
        }
        // find custom fields and convert them
        for (String key : i2Issue.keys()) {
            String customFieldId = fieldsCache.getCustomFieldId(key);
            if (customFieldId != null) {
                String type = fieldsCache.getCustomFieldType(customFieldId);
                if (fieldsCache.isCustomFieldArray(customFieldId)) {
                    fields.set(customFieldId, convertManyFieldValueToJira(i2Issue.object(key), type));
                } else {
                    fields.set(customFieldId, convertFieldValueToJira(i2Issue.object(key), type));
                }
            }
        }
        issue.set("fields", fields);
        return issue;
    }

    /**
     * Converts a comment in JIRA format to more convenient format for application.
     *
     * @param jiraComment the JSON of the JIRA comment
     * @return the JSON of the application comment
     */
    public Json commentFromJiraToApp(Json jiraComment) {
        Json comment = Json.map();
        comment.set("id", jiraComment.string("id"));
        comment.set("author", convertFieldValueToApp(jiraComment.json("author"), "user"));
        comment.set("created", TimeUtils.parseJiraDate(jiraComment.string("created")));
        comment.set("bodyHtml", TextConverter.convertWikiToHtml(jiraComment.string("body")));
        comment.set("bodyText", TextConverter.convertWikiToText(jiraComment.string("body")));
        comment.set("bodyWiki", jiraComment.string("body"));
        return comment;
    }

    /**
     * Converts a comment in application format to JIRA format (for adding).
     *
     * @param i2Comment comment in application
     * @return the comment to be added to JIRA
     */
    public Json commentFromAppToJira(Json i2Comment) {
            String bodyFormat = i2Comment.string("bodyFormat");
        String body = null;
        if ("html".equalsIgnoreCase(bodyFormat)) {
            body = TextConverter.convertHtmlToWiki(i2Comment.string("body"));
        } else if ("wiki".equalsIgnoreCase(bodyFormat)) {
            body = i2Comment.string("body");
        } else {
            body = TextConverter.convertTextToWiki(i2Comment.string("body"));
        }
        return Json.map()
                .set("issueKey", i2Comment.string("issueKey"))
                .set("body", body);
    }

    /**
     * Converts a work log in JIRA format to more convenient format for application.
     *
     * @param jiraWorkLog the JSON of the JIRA work log
     * @return the JSON of the application work log
     */
    public Json workLogFromJiraToApp(Json jiraWorkLog) {
        Json worklog = Json.map();
        worklog.set("id", jiraWorkLog.string("id"));
        worklog.set("author", convertFieldValueToApp(jiraWorkLog.json("author"), "user"));
        worklog.set("created", TimeUtils.parseJiraDate(jiraWorkLog.string("created")));
        worklog.set("started", TimeUtils.parseJiraDate(jiraWorkLog.string("started")));
        worklog.set("timeSpent", TimeUtils.parseSeconds(jiraWorkLog.integer("timeSpentSeconds")));
        worklog.set("commentHtml", TextConverter.convertWikiToHtml(jiraWorkLog.string("comment")));
        worklog.set("commentText", TextConverter.convertWikiToText(jiraWorkLog.string("comment")));
        worklog.set("commentWiki", jiraWorkLog.string("comment"));
        return worklog;
    }

    /**
     * Converts a attachment reference in JIRA format to more convenient format for application.
     *
     * @param jiraAttachment the JSON of the JIRA attachment reference
     * @return the JSON of the application attachment reference
     */
    public Json attachmentFromJiraToApp(Json jiraAttachment) {
        Json attachment = Json.map();
        attachment.set("id", jiraAttachment.string("id"));
        attachment.set("author", convertFieldValueToApp(jiraAttachment.json("author"), "user"));
        attachment.set("created", TimeUtils.parseJiraDate(jiraAttachment.string("created")));
        attachment.set("filename", jiraAttachment.string("filename"));
        attachment.set("mimeType", jiraAttachment.string("mimeType"));
        attachment.set("size", jiraAttachment.integer("size"));
        attachment.set("contentUrl", jiraAttachment.integer("contentUrl"));
        return attachment;
    }

    private Json getCustomFields(Json issue) {
        Json customFields = Json.map();
        for (String key : issue.keys()) {
            if (key.startsWith("customfield_")) {
                String customFieldName = fieldsCache.getCustomFieldName(key);
                String type = fieldsCache.getCustomFieldType(key);
                if (fieldsCache.isCustomFieldArray(key)) {
                    customFields.set(customFieldName, convertManyFieldValueToApp(issue.object(key), type));
                } else {
                    customFields.set(customFieldName, convertFieldValueToApp(issue.object(key), type));
                }
            }
        }
        return customFields;
    }

    private Json convertManyFieldValueToApp(Object list, String type) {
        if (list == null) {
            return null;
        }
        Json newList = Json.list();
        if (list instanceof Json && ((Json) list).isList()) {
            for (Object item : ((Json) list).toList()) {
                newList.push(convertFieldValueToApp(item, type));
            }
        } else if (list instanceof List) {
            for (Object item : (List) list) {
                newList.push(convertFieldValueToApp(item, type));
            }
        } else {
            // seems like JIRA sends one value outside a list in some cases, even when the field
            // type is an array
            newList.push(convertFieldValueToApp(list, type));
        }
        return newList;
    }

    private Json convertManyFieldValueToJira(Object list, String type) {
        if (list == null) {
            return null;
        }
        Json newList = Json.list();
        if (list instanceof Json && ((Json) list).isList()) {
            for (Object item : ((Json) list).toList()) {
                newList.push(convertFieldValueToJira(item, type));
            }
        } else if (list instanceof List) {
            for (Object item : (List) list) {
                newList.push(convertFieldValueToJira(item, type));
            }
        }
        return newList;
    }

    private Object convertFieldValueToJira(Object value, String type) {
        if (value == null) {
            return null;
        }
        switch (type) {
            case "string":
            case "number":
            case "date":
                return value;

            case "datetime":
                return TimeUtils.formatJiraDate((Long) value);

            case "user":
            case "version":
            case "component":
            case "priority":
            case "resolution":
            case "issuetype":
            case "status":
                return Json.map().set("name", value);

            case "project":
                return Json.map().set("key", value);

            default:
                return value;
        }
    }

    private Object convertFieldValueToApp(Object value, String type) {
        if (value == null) {
            return null;
        }
        switch (type) {
            case "string":
            case "number":
            case "date":
                return value;

            case "datetime":
                return TimeUtils.parseJiraDate((String) value);

            case "user":
                return convertEnum((Json) value, "key", "emailAddress", "displayName", "active");

            case "version":
                return convertEnum((Json) value, "archived", "released", "releaseDate");

            case "component":
            case "priority":
            case "resolution":
            case "issuetype":
            case "status":
                return convertEnum((Json) value);

            case "project":
                return convertEnum((Json) value, "key");

            case "issuelinks":
                return issueRef((Json) value);

            default:
                return value;
        }
    }

    private Json convertEnum(Json json, String ...  additionalFields) {
        if (json == null) {
            return null;
        }
        Json enumJson = Json.map();
        if (json.contains("id")) {
            enumJson.set("id", json.string("id"));
        }
        enumJson.set("name", json.string("name"));
        for (String field : additionalFields) {
            enumJson.set(field, json.object(field));
        }
        return enumJson;
    }

    private Json issueRef(Json json) {
        if (json == null) {
            return null;
        }
        Json ref = Json.map();
        ref.set("id", json.string("id"));
        ref.set("key", json.string("key"));
        ref.set("summary", json.json("fields").string("summary"));
        return ref;
    }

    private Json issueRefs(List<Json> refs) {
        if (refs == null) {
            return null;
        }
        Json list = Json.list();
        for (Json item : refs) {
            Json ref = issueRef(item);
            list.push(ref);
        }
        return list;
    }

    private Json issueLink(Json json) {
        if (json == null) {
            return null;
        }
        Json link;
        Json linkType = json.json("type");
        // in relationship we store the type of link looking from this issue
        // for example if this issue blocks another issue, then relationship value will be "blocks"
        // if this issue is blocked by another issue, then the relationship value will be "is blocked by"
        if (json.contains("outwardIssue")) {
            link = issueRef(json.json("outwardIssue"));
            link.set("relationship", linkType.string("outward"));
        } else {
            link = issueRef(json.json("inwardIssue"));
            link.set("relationship", linkType.string("inward"));
        }
        return link;
    }

    private Json issueLinks(List<Json> links) {
        if (links == null) {
            return null;
        }
        Json list = Json.list();
        for (Json item : links) {
            Json link = issueLink(item);
            list.push(link);
        }
        return list;
    }
}