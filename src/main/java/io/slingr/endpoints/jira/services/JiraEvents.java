package io.slingr.endpoints.jira.services;

import io.slingr.endpoints.jira.converters.IssueConverter;
import io.slingr.endpoints.jira.converters.TimeUtils;
import io.slingr.endpoints.utils.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;

/**
 * Created by dgaviola on 5/6/15.
 */
public class JiraEvents {
    private static final Logger logger = LoggerFactory.getLogger(JiraEvents.class);

    private IssueConverter issueConverter;
    private JiraApi jiraApi;
    private FieldsCache fieldsCache;

    public JiraEvents(IssueConverter issueConverter, JiraApi jiraApi, FieldsCache fieldsCache) {
        this.issueConverter = issueConverter;
        this.jiraApi = jiraApi;
        this.fieldsCache = fieldsCache;
    }

    public String detectUser(Json body) {
        if (body.contains("user")) {
            return body.json("user").string("name");
        } else {
            return "__system_user__";
        }
    }

    public String detectEvent(Json body) {
        final String jiraEvent = body.string("webhookEvent");
        switch (jiraEvent) {
            case "jira:issue_created":
                return "issue-created";

            case "jira:issue_updated":
                if (body.contains("comment")) {
                    return "comment-created";
                } else {
                    return "issue-updated";
                }

            case "jira:issue_deleted":
                return "issue-deleted";

            case "jira:version_released":
                return "version-released";
        }
        logger.info(String.format("Event [%s] unknown", jiraEvent));
        return "unknown";
    }

    public Json convertCreatedIssue(Json body) {
        Json issue = issueConverter.fromJiraToApp(body.json("issue"));
        return issue;
    }

    public Json convertUpdatedIssue(Json body) {
        Json issue = issueConverter.fromJiraToApp(body.json("issue"));
        Json fields = Json.list();
        if (body.contains("changelog") && body.json("changelog").contains("items")) {
            for (Json item : body.json("changelog").jsons("items")) {
                String fieldName = item.string("field");
                if (fieldName.startsWith("customfield_")) {
                    fieldName = fieldsCache.getCustomFieldName(fieldName);
                }
                fields.push(fieldName);
            }
        }
        issue.set("modifiedFields", fields);
        return issue;
    }

    public Json convertDeletedIssue(Json body) {
        Json issue = issueConverter.fromJiraToApp(body.json("issue"));
        return issue;
    }

    public Json convertCreatedComment(Json body) {
        Json comment = issueConverter.commentFromJiraToApp(body.json("comment"));
        comment.set("issueKey", body.json("issue").string("key"));
        return comment;
    }

    public Json convertReleasedVersion(Json body) throws ParseException {
        Json version = Json.map();
        version.set("id", body.string("id"));
        version.set("name", body.json("version").string("name"));
        version.set("description", body.json("version").string("description"));
        version.set("releaseDate", TimeUtils.parseVersionDate(body.json("version").string("userReleaseDate")));
        Json project = jiraApi.findProject(body.json("version").string("projectId"));
        version.set("project", project.string("key"));
        return version;
    }
}
