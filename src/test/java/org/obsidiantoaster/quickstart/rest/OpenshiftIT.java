package org.obsidiantoaster.quickstart.rest;

import org.jboss.arquillian.container.test.api.RunAsClient;
import org.jboss.arquillian.junit.Arquillian;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Heiko Braun
 */
@RunWith(Arquillian.class)
@RunAsClient
public class OpenshiftIT {

    private static final String APPLICATION_NAME = System.getProperty("app.name");

    private static final OpenshiftTestAssistant openshift = new OpenshiftTestAssistant(APPLICATION_NAME);

    private static String API_ENDPOINT;

    @BeforeClass
    public static void setup() throws Exception {

        Assert.assertNotNull(APPLICATION_NAME);

        // the application itself
        openshift.deployApplication();

        // wait until the pods & routes become available
        openshift.awaitApplicationReadinessOrFail();

        API_ENDPOINT = openshift.getBaseUrl() + "/api/greeting";
    }

    @AfterClass
    public static void teardown() throws Exception {
       openshift.cleanup();
    }

    @Test
    public void testServiceInvocation() {

        expect().
            statusCode(200).
            body(containsString("Hello, World!")).
        when().
            get(API_ENDPOINT);
    }

    @Test

    public void testServiceInvocationWithParam() {

        given().
            param("name", "Peter").
        expect().
                statusCode(200).
                body(containsString("Hello, Peter!")).
            when().
                get(API_ENDPOINT);
    }
}

