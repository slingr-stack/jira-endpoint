package io.slingr.endpoints.jira.services;

import io.slingr.endpoints.services.rest.RestClient;
import io.slingr.endpoints.utils.Json;
import org.apache.commons.lang.StringUtils;

import javax.ws.rs.client.WebTarget;

/**
 * Talks to the JIRA REST API.
 *
 * Created by dgaviola on 3/6/15.
 */
public class JiraApi extends RestClient {

    public JiraApi(String jiraUrl, String username, String password, boolean debug) {
        super(jiraUrl + "/rest/api/2");
        setDebug(debug);
        setupBasicAuthentication(username, password);
    }

    public Json findIssues(Json params) {
        WebTarget target = getApiTarget()
                .path("/search")
                .queryParam("jql", params.string("query"));
        if (!params.isEmpty("offset")) {
            target = target.queryParam("startAt", params.integer("offset"));
        }
        if (!params.isEmpty("size")) {
            if (params.integer("size") > 1000) {
                throw new IllegalArgumentException("Size cannot be greater than 1,000");
            }
            target = target.queryParam("maxResults", params.integer("size"));
        }
        return get(target);
    }

    public Json findIssue(Json query) {
        WebTarget target = getApiTarget().path("/issue/" + query.string("key"));
        return get(target);
    }

    public Json createIssue(Json issue) {
        WebTarget target = getApiTarget().path("/issue");
        return post(target, issue);
    }

    public Json updateIssue(Json issue) {
        WebTarget target = getApiTarget().path("/issue/" + issue.string("key"));
        Json res = put(target, issue);
        res.set("key", issue.string("key"));
        return res;
    }

    public Json deleteIssue(Json query) {
        WebTarget target = getApiTarget().path("/issue/" + query.string("key"));
        Json res = delete(target);
        res.set("key", query.string("key"));
        return res;
    }

    public Json addComment(Json comment) {
        WebTarget target = getApiTarget().path("/issue/" + comment.string("issueKey") + "/comment");
        return post(target, comment);
    }

    public Json doTransition(Json transitionInfo) {
        // first we need to get valid transitions for this issue
        if (transitionInfo.isEmpty("issueKey")) {
            throw new IllegalArgumentException("You need to specify issueKey");
        }
        Json validTransitions = findValidTransitions(transitionInfo.string("issueKey"));
        // verify the requested transition is valid
        String transition = null;
        String transitionId = null;
        if (!transitionInfo.isEmpty("transitionId")) {
            transition = transitionInfo.string("transitionId");
        } else {
            transition = transitionInfo.string("transitionName");
        }
        if (StringUtils.isBlank(transition)) {
            throw new IllegalArgumentException("You have to specify either transitionId or transitionName");
        }
        if (validTransitions == null || validTransitions.isEmpty("transitions")) {
            throw new IllegalArgumentException(String.format("There are no possible transitions for issue [%s]", transitionInfo.string("issueKey")));
        }
        boolean valid = false;
        for (Json t : validTransitions.jsons("transitions")) {
            if (transition.equals(t.string("id")) || transition.equalsIgnoreCase(t.string("name"))) {
                transitionId = t.string("id");
                valid = true;
                break;
            }
        }
        if (!valid) {
            throw new IllegalArgumentException(String.format("Transition [%s] is not valid for issue [%s]", transition, transitionInfo.string("issueKey")));
        }
        // perform transition
        WebTarget target = getApiTarget().path("/issue/" + transitionInfo.string("issueKey") + "/transitions");
        Json body = Json.map()
                .set("transition", Json.map()
                        .set("id", transitionId)
                );
        Json res = post(target, body);
        if (res != null) {
            return findIssue(Json.map().set("key", transitionInfo.string("issueKey")));
        } else {
            throw new RuntimeException(String.format("Error executing transition [%s] on issue [%s]", transition, transitionInfo.string("issueKey")));
        }
    }

    public Json findFields() {
        WebTarget target = getApiTarget().path("/field");
        return get(target);
    }

    public Json findProject(String projectId) {
        WebTarget target = getApiTarget().path("/project/" + projectId);
        return get(target);
    }

    public Json findValidTransitions(String issueKey) {
        WebTarget target = getApiTarget().path("/issue/" + issueKey + "/transitions");
        return get(target);

    }

    public Json serverInfo(Json body) {
        WebTarget target = getApiTarget().path("/serverInfo");
        target = target.queryParam("doHealthCheck", body.bool("doHealthCheck", false) ? "true" : "false");
        return get(target);
    }
}
