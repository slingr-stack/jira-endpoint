package io.slingr.endpoints.jira;

import io.slingr.endpoints.Endpoint;
import io.slingr.endpoints.exceptions.EndpointException;
import io.slingr.endpoints.framework.annotations.EndpointFunction;
import io.slingr.endpoints.framework.annotations.EndpointProperty;
import io.slingr.endpoints.framework.annotations.EndpointWebService;
import io.slingr.endpoints.framework.annotations.SlingrEndpoint;
import io.slingr.endpoints.jira.converters.IssueConverter;
import io.slingr.endpoints.jira.services.FieldsCache;
import io.slingr.endpoints.jira.services.JiraApi;
import io.slingr.endpoints.jira.services.JiraEvents;
import io.slingr.endpoints.services.rest.RestMethod;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.WebServiceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>JIRA endpoint
 *
 * <p>Created by dgaviola on 03/06/15.
 */
@SlingrEndpoint(name = "jira")
public class JiraEndpoint extends Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(JiraEndpoint.class);

    @EndpointProperty
    private String username;

    @EndpointProperty
    private String password;

    @EndpointProperty
    private String jiraUrl;

    private JiraApi jiraApi = null;
    private FieldsCache fieldsCache = null;
    private IssueConverter issueConverter = null;
    private JiraEvents jiraEvents = null;

    @Override
    public void endpointStarted() {
        setupRetryableExceptionsProperties(6, 1000);

        jiraApi = new JiraApi(this.jiraUrl, this.username, this.password, this.properties().isDebug());
        fieldsCache = new FieldsCache(jiraApi);
        issueConverter = new IssueConverter(fieldsCache);
        jiraEvents = new JiraEvents(issueConverter, jiraApi, fieldsCache);

        // inits fields cache at the beginning
        try {
            fieldsCache.refresh();
        } catch (Exception e) {
            logger.warn(String.format("Problem trying to init fields cache. We will try later when we need it - exception: [%s]", e.getMessage()), e);
        }

        logger.info(String.format("Configured JIRA endpoint: username [%s], JIRA URL [%s]", this.username, this.jiraUrl));
    }

    /**
     * find issues
     */
    @EndpointFunction
    public Json findIssues(Json params){
        final Json issues = jiraApi.findIssues(params);
        return issueConverter.searchResultFromJiraToApp(issues);
    }

    /**
     * find issue
     */
    @EndpointFunction
    public Json findIssue(Json params){
        final Json issue = jiraApi.findIssue(params);
        return issueConverter.fromJiraToApp(issue);
    }

    /**
     * create issue
     */
    @EndpointFunction
    public Json createIssue(Json params){
        final Json newIssue = issueConverter.fromAppToJira(params);
        final Json createdIssue = jiraApi.createIssue(newIssue);
        final Json issue = jiraApi.findIssue(createdIssue);
        return issueConverter.fromJiraToApp(issue);
    }

    /**
     * update issue
     */
    @EndpointFunction
    public Json updateIssue(Json params){
        final Json newIssue = issueConverter.fromAppToJira(params);
        final Json updatedIssue = jiraApi.updateIssue(newIssue);
        final Json issue = jiraApi.findIssue(updatedIssue);
        return issueConverter.fromJiraToApp(issue);
    }

    /**
     * add comment to issue
     */
    @EndpointFunction
    public Json addComment(Json params){
        final Json newComment = issueConverter.commentFromAppToJira(params);
        final Json comment = jiraApi.addComment(newComment);
        return issueConverter.commentFromJiraToApp(comment);
    }

    /**
     * do transition on issue
     */
    @EndpointFunction
    public Json doTransition(Json params){
        final Json transition = jiraApi.doTransition(params);
        return issueConverter.fromJiraToApp(transition);
    }

    // TODO add work log to issue
    // TODO attach file to issue

    /**
     * delete issue
     */
    @EndpointFunction
    public Json deleteIssue(Json params){
        return jiraApi.deleteIssue(params);
    }

    /**
     * get server info
     */
    @EndpointFunction
    public Json serverInfo(Json params){
        return jiraApi.serverInfo(params);
    }

    @EndpointWebService(methods = RestMethod.POST)
    public Json jiraWebhooks(Json request){
        logger.info("Event arrived");

        final String user = jiraEvents.detectUser(request);
        if(!username.equals(user)){
            // we filter events coming from the endpoint user
            logger.info("Event not from endpoint");

            final String eventType = jiraEvents.detectEvent(request);
            switch (eventType){
                case "issue-created": {
                    // issue created event
                    logger.info("Issue created event arrived");
                    Json event = jiraEvents.convertCreatedIssue(request);
                    events().send("issueCreated", event);
                    logger.info("Issue created event sent to application");

                    break;
                }
                case "issue-updated": {
                    logger.info("Issue updated event arrived");
                    Json event = jiraEvents.convertUpdatedIssue(request);
                    events().send("issueUpdated", event);
                    logger.info("Issue updated event sent to application");

                    break;
                }
                case "issue-deleted": {
                    logger.info("Issue deleted event arrived");
                    Json event = jiraEvents.convertDeletedIssue(request);
                    events().send("issueDeleted", event);
                    logger.info("Issue deleted event sent to application");

                    break;
                }
                case "comment-created": {
                    logger.info("Comment created event arrived");
                    Json event = jiraEvents.convertCreatedComment(request);
                    events().send("commentCreated", event);
                    logger.info("Comment created event sent to application");

                    break;
                }
                case "version-released": {
                    logger.info("Version released event arrived");
                    try {
                        Json event = jiraEvents.convertReleasedVersion(request);
                        events().send("versionReleased", event);
                        logger.info("Version released event sent to application");
                    } catch (Exception ex){
                        logger.warn(String.format("Error to process version release event: %s", ex.getMessage()), ex);
                    }

                    break;
                }
                default:
                    logger.info("Unrecognized event");
                    break;
            }

        }
        return Json.map().set("status", "ok");
    }
}
