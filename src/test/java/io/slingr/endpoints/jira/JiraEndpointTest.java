package io.slingr.endpoints.jira;

import io.slingr.endpoints.services.exchange.Parameter;
import io.slingr.endpoints.services.rest.RestMethod;
import io.slingr.endpoints.utils.converters.JsonSource;
import io.slingr.endpoints.utils.tests.EndpointTests;
import io.slingr.endpoints.utils.Json;
import io.slingr.endpoints.ws.exchange.WebServiceResponse;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 *
 * Created by dgaviola on 20/05/15.
 */
public class JiraEndpointTest {
    private static final Logger logger = LoggerFactory.getLogger(JiraEndpointTest.class);

    private static EndpointTests test;

    @BeforeClass
    public static void init() throws Exception {
        test = EndpointTests.start(new io.slingr.endpoints.jira.Runner(), "test.properties");
    }

    @Test
    public void testFindIssues() throws Exception {
        Json req = Json.map().set("query", "issueKey=TEST-1");
        Json res = test.executeFunction("findIssues", req);
        Json item = res.jsons("items").get(0);
        Assert.assertEquals("TEST-1", item.string("key"));

        logger.info("-- END");
    }

    @Test
    public void testConvertAllIssuesWithoutErrors() throws Exception {
        Json req = Json.map().set("query", "labels = test");
        Json res = test.executeFunction("findIssues", req);
        Assert.assertEquals(9, (int) res.integer("total"));
        Assert.assertEquals(9, res.jsons("items").size());

        logger.info("-- END");
    }

    @Test
    public void testFindIssue() throws Exception {
        Json req = Json.map().set("key", "TEST-1");
        Json res = test.executeFunction("findIssue", req);
        Assert.assertEquals("TEST-1", res.string("key"));

        logger.info("-- END");
    }

    @Test
    public void testErrorResponse() throws Exception {
        Json req = Json.map().set("size", "1000000");
        Json res = test.executeFunction("findIssues", req, true);
        assertNotNull(res);
        assertEquals(true, res.bool(Parameter.EXCEPTION_FLAG));
        assertEquals("general", res.json("error").string("code"));
        assertEquals("General exception", res.json("error").string("name"));
        assertNotNull(res.string(Parameter.EXCEPTION_MESSAGE));
        assertEquals("Size cannot be greater than 1,000", res.string(Parameter.EXCEPTION_MESSAGE));

        logger.info("-- END");
    }

    @Test
    public void testTestIssueConverter1() throws Exception {
        Json req = Json.map().set("key", "TEST-1");
        Json res = test.executeFunction("findIssue", req);
        assertEquals("TEST-1", res.string("key"));
        assertEquals("Task", res.json("issueType").string("name"));
        assertEquals("3", res.json("issueType").string("id"));
        assertEquals("TEST", res.json("project").string("key"));
        assertTrue(res.object("created") instanceof Long);
        Json link1 = res.jsons("issueLinks").get(0);
        assertEquals("TEST-8", link1.string("key"));
        assertEquals("Test bug - DO NOT TOUCH", link1.string("summary"));
        assertEquals("is blocked by", link1.string("relationship"));
        Json link2 = res.jsons("issueLinks").get(1);
        assertEquals("TEST-3", link2.string("key"));
        assertEquals("Test 3", link2.string("summary"));
        assertEquals("relates to", link2.string("relationship"));
        assertEquals("test", res.json("assignee").string("key"));
        assertEquals("test", res.json("assignee").string("name"));
        assertEquals("test@slingr.io", res.json("assignee").string("emailAddress"));
        assertEquals("Test User", res.json("assignee").string("displayName"));
        assertEquals(true, res.json("assignee").bool("active"));
        assertEquals("In Progress", res.json("status").string("name"));
        Json component1 = res.jsons("components").get(0);
        assertEquals("Client Side", component1.string("name"));
        Json component2 = res.jsons("components").get(1);
        assertEquals("Server Side", component2.string("name"));

        logger.info("-- END");
    }

    @Test
    public void testTestIssueConverter2() throws Exception {
        Json req = Json.map().set("key", "TEST-4");
        Json res = test.executeFunction("findIssue", req);
        assertEquals(25200000, (long) res.integer("aggregateTimeSpent"));
        assertEquals(21600000, (long) res.integer("aggregateTimeEstimate"));
        assertEquals(25200000, (long) res.json("aggregateProgress").integer("progress"));
        assertEquals(46800000, (long) res.json("aggregateProgress").integer("total"));
        assertEquals("0.53", "" + res.json("aggregateProgress").decimal("percent"));
        assertEquals("Test Epic - DO NOT TOUCH", res.string("summary"));
        Json subTask1 = res.jsons("subTasks").get(0);
        assertEquals("TEST-5", subTask1.string("key"));
        assertEquals("Sub task 1", subTask1.string("summary"));
        Json subTask2 = res.jsons("subTasks").get(1);
        assertEquals("TEST-6", subTask2.string("key"));
        assertEquals("Sub task 2", subTask2.string("summary"));

        logger.info("-- END");
    }

    @Test
    public void testTestIssueConverter3() throws Exception {
        Json req = Json.map().set("key", "TEST-6");
        Json res = test.executeFunction("findIssue", req);
        assertEquals(7200000, (long) res.integer("timeSpent"));
        assertEquals(7200000, (long) res.integer("aggregateTimeSpent"));
        assertEquals(14400000, (long) res.integer("timeEstimate"));
        assertEquals(14400000, (long) res.integer("aggregateTimeEstimate"));
        assertEquals(14400000, (long) res.integer("remainingEstimate"));
        assertEquals(7200000, (long) res.json("progress").integer("progress"));
        assertEquals(21600000, (long) res.json("progress").integer("total"));
        assertEquals("0.33", "" + res.json("progress").decimal("percent"));

        logger.info("-- END");
    }

    @Test
    public void testTestIssueConverter4() throws Exception {
        Json req = Json.map().set("key", "TEST-2");
        Json res = test.executeFunction("findIssue", req);
        assertEquals(3, res.jsons("comments").size());
        Json comment1 = res.jsons("comments").get(0);
        assertNotNull(comment1.string("id"));
        assertEquals("dgaviola", comment1.json("author").string("name"));
        assertEquals("comment 1\r\n\r\n*bold style*\r\n\r\nThis is a list:\r\n\r\n* item1\r\n* item2\r\n* item3\r\n\r\nEnd of comment.", comment1.string("bodyWiki"));
    }

    @Test
    public void testTestIssueConverter5() throws Exception {
        Json req = Json.map().set("key", "TEST-6");
        Json res = test.executeFunction("findIssue", req);
        assertEquals(1, res.jsons("workLogs").size());
        Json workLog1 = res.jsons("workLogs").get(0);
        assertNotNull(workLog1.string("id"));
        assertEquals("dgaviola", workLog1.json("author").string("name"));
        assertTrue(workLog1.object("created") instanceof Long);
        assertTrue(workLog1.object("started") instanceof Long);
        assertEquals(7200000, (long) workLog1.integer("timeSpent"));
        assertEquals("doing something", workLog1.string("commentWiki"));
        assertEquals("doing something", workLog1.string("commentText"));
        assertEquals("<p>doing something</p>", workLog1.string("commentHtml"));

        logger.info("-- END");
    }

    @Test
    public void testTestIssueConverter6() throws Exception {
        Json req = Json.map().set("key", "TEST-3");
        Json res = test.executeFunction("findIssue", req);
        assertEquals(2, res.jsons("attachments").size());
        Json attachment1 = res.jsons("attachments").get(0);
        assertNotNull(attachment1.string("id"));
        assertEquals("dgaviola", attachment1.json("author").string("name"));
        assertTrue(attachment1.object("created") instanceof Long);
        assertEquals("appicon.png", attachment1.string("filename"));
        assertEquals("image/png", attachment1.string("mimeType"));
        assertEquals(6313, (int) attachment1.integer("size"));

        logger.info("-- END");
    }

    @Test
    public void testCreateAndUpdateAndDeleteIssue() throws Exception {
        Json req = Json.map()
                .set("project", "TEST")
                .set("issueType", "Bug")
                .set("versions", Json.list().push("v1.0"))
                .set("labels", Json.list().push("label1").push("label2"))
                .set("components", Json.list().push("Client Side"))
                .set("assignee", "test")
                .set("reporter", "test")
                .set("summary", "testing jira integration")
                .set("descriptionFormat", "html")
                .set("description", "<p>things to do:</p><ul><li>thing 1</li><li>thing 2</li></ul>");
        Json res = test.executeFunction("createIssue", req);
        assertNotNull(res);
        assertEquals("testing jira integration", res.string("summary"));
        assertEquals("test", res.json("assignee").string("name"));
        assertEquals("test", res.json("reporter").string("name"));
        assertEquals(2, res.strings("labels").size());
        assertEquals("label1", res.strings("labels").get(0));
        assertEquals("label2", res.strings("labels").get(1));

        req = Json.map()
                .set("key", res.string("key"))
                .set("summary", "updated summary")
                .set("labels", Json.list().push("label2").push("label3"))
                .set("Main Reviewer", "test")   // custom field 10400
                .set("Story Points", 3);        // custom field 10004
        res = test.executeFunction("updateIssue", req);
        assertNotNull(res);
        assertEquals("updated summary", res.string("summary"));
        assertEquals(2, res.strings("labels").size());
        assertEquals("label2", res.strings("labels").get(0));
        assertEquals("label3", res.strings("labels").get(1));
        assertEquals("test", res.json("customFields").json("Main Reviewer").string("name"));
        assertEquals(3, (int) res.json("customFields").integer("Story Points"));
        req = Json.map()
                .set("key", res.string("key"));
        res = test.executeFunction("deleteIssue", req);
        assertNotNull(res);

        logger.info("-- END");
    }

    @Test
    public void testAddComment() throws Exception {
        Json req = Json.map()
                .set("project", "TEST")
                .set("issueType", "Story")
                .set("summary", "testing comments api");
        Json res = test.executeFunction("createIssue", req);
        assertNotNull(res);
        String issueKey = res.string("key");

        req = Json.map()
                .set("issueKey", issueKey)
                .set("bodyFormat", "html")
                .set("body", "<p>test</p><strong>bold string</strong>");
        test.executeFunction("addComment", req);

        req = Json.map()
                .set("key", issueKey);
        res = test.executeFunction("findIssue", req);
        assertNotNull(res);
        assertNotNull(res.jsons("comments"));
        assertEquals(1, res.jsons("comments").size());
        assertTrue(res.jsons("comments").get(0).string("bodyText").contains("bold string"));

        req = Json.map()
                .set("key", issueKey);
        test.executeFunction("deleteIssue", req);

        logger.info("-- END");
    }

    @Test
    public void testTransitions() throws Exception {
        Json req = Json.map()
                .set("project", "TEST")
                .set("issueType", "Story")
                .set("summary", "testing transitions api");
        Json res = test.executeFunction("createIssue", req);
        assertNotNull(res);
        String issueKey = res.string("key");

        req = Json.map()
                .set("issueKey", issueKey)
                .set("transitionName", "Selected for Development");
        test.executeFunction("doTransition", req);

        req = Json.map()
                .set("key", issueKey);
        res = test.executeFunction("findIssue", req);
        assertNotNull(res);
        assertEquals("Selected for Development", res.json("status").string("name"));

        req = Json.map()
                .set("key", issueKey);
        test.executeFunction("deleteIssue", req);

        logger.info("-- END");
    }

    @Test
    public void testServerInfo() throws Exception {
        Json req = Json.map().set("doHealthCheck", false);
        Json res = test.executeFunction("serverInfo", req);
        assertNotNull(res);
        assertEquals("JIRA", res.string("serverTitle"));
        assertFalse(res.contains("healthChecks"));

        req = Json.map().set("doHealthCheck", true);
        res = test.executeFunction("serverInfo", req);
        assertNotNull(res);
        assertEquals("JIRA", res.string("serverTitle"));
        assertFalse(res.jsons("healthChecks").isEmpty());

        logger.info("-- END");
    }

    @Test
    public void testIssueCreatedEvent() throws IOException, InterruptedException {
        WebServiceResponse response = test.executeWebServices(RestMethod.POST, "/", Json.fromInternalFile("issueCreated.json"));
        assertNotNull(response);
        Object body = response.getBody();
        assertTrue(body instanceof JsonSource);
        Json data = ((JsonSource) body).toJson();
        assertNotNull(data);
        assertEquals("TEST-34", data.string("key"));
        assertEquals("TEST", data.json("project").string("key"));
        assertEquals("test issue", data.string("summary"));

        logger.info("-- END");
    }

    @Test
    public void testIssueUpdatedEvent() throws IOException, InterruptedException {
        WebServiceResponse response = test.executeWebServices(RestMethod.POST, "/", Json.fromInternalFile("issueUpdated.json"));
        assertNotNull(response);
        Object body = response.getBody();
        assertTrue(body instanceof JsonSource);
        Json data = ((JsonSource) body).toJson();
        assertNotNull(data);
        assertEquals("TEST-34", data.string("key"));
        assertEquals("TEST", data.json("project").string("key"));
        assertEquals("test issue", data.string("summary"));
        assertEquals(4, data.strings("modifiedFields").size());
        assertEquals("labels", data.strings("modifiedFields").get(0));
        assertEquals("priority", data.strings("modifiedFields").get(1));
        assertEquals("Component", data.strings("modifiedFields").get(2));
        assertEquals("Rank", data.strings("modifiedFields").get(3));

        logger.info("-- END");
    }

    @Test
    public void testIssueDeletedEvent() throws IOException, InterruptedException {
        WebServiceResponse response = test.executeWebServices(RestMethod.POST, "/", Json.fromInternalFile("issueDeleted.json"));
        assertNotNull(response);
        Object body = response.getBody();
        assertTrue(body instanceof JsonSource);
        Json data = ((JsonSource) body).toJson();
        assertNotNull(data);
        assertEquals("TEST-34", data.string("key"));
        assertEquals("TEST", data.json("project").string("key"));
        assertEquals("test issue", data.string("summary"));

        logger.info("-- END");
    }

    @Test
    public void testCommentCreatedEvent() throws IOException, InterruptedException {
        WebServiceResponse response = test.executeWebServices(RestMethod.POST, "/", Json.fromInternalFile("commentCreated.json"));
        assertNotNull(response);
        Object body = response.getBody();
        assertTrue(body instanceof JsonSource);
        Json data = ((JsonSource) body).toJson();
        assertNotNull(data);
        assertEquals("TEST-34", data.string("issueKey"));
        assertEquals("dgaviola", data.json("author").string("name"));
        assertTrue(data.object("created") instanceof Long);
        assertEquals("test comment", data.string("bodyText"));
        assertEquals("*test comment*", data.string("bodyWiki"));
        assertEquals("<p><strong>test comment</strong></p>", data.string("bodyHtml"));
    }

    @Test
    public void testVersionReleasedEvent() throws IOException, InterruptedException {
        WebServiceResponse response = test.executeWebServices(RestMethod.POST, "/", Json.fromInternalFile("versionReleased.json"));
        assertNotNull(response);
        Object body = response.getBody();
        assertTrue(body instanceof JsonSource);
        Json data = ((JsonSource) body).toJson();
        assertNotNull(data);
        assertEquals("TEST", data.string("project"));
        assertEquals("v1.0.1", data.string("name"));
        assertEquals("2015-06-05", data.string("releaseDate"));

        logger.info("-- END");
    }
}
