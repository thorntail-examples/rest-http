package io.openshift.booster;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.expect;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Heiko Braun
 */
public class OpenshiftIT {

    private static final OpenshiftTestAssistant openshift = new OpenshiftTestAssistant();

    @BeforeClass
    public static void setup() throws Exception {
        // the application itself
        openshift.deployApplication();

        // wait until the pods & routes become available
        openshift.awaitApplicationReadinessOrFail();
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
                get(openshift.getBaseUrl() + "/api/greeting");
    }

    @Test
    public void testServiceInvocationWithParam() {

        given().
                param("name", "Peter").
                expect().
                statusCode(200).
                body(containsString("Hello, Peter!")).
                when().
                get(openshift.getBaseUrl() + "/api/greeting");
    }
}
