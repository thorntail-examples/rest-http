package io.openshift.booster;

import java.util.concurrent.TimeUnit;

import com.jayway.restassured.RestAssured;
import com.jayway.restassured.response.Response;
import io.openshift.booster.test.OpenShiftTestAssistant;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.jayway.awaitility.Awaitility.await;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;
import static org.hamcrest.Matchers.containsString;

/**
 * @author Heiko Braun
 */
public class OpenshiftIT {

    private static final OpenShiftTestAssistant openshift = new OpenShiftTestAssistant();

    @BeforeClass
    public static void setup() throws Exception {
        // the application itself
        openshift.deployApplication();

        // wait until the pods & routes become available
        openshift.awaitApplicationReadinessOrFail();

        await().atMost(5, TimeUnit.MINUTES).until(() -> {
            try {
                Response response = get();
                return response.getStatusCode() == 200;
            } catch (Exception e) {
                return false;
            }
        });

        RestAssured.baseURI = RestAssured.baseURI + "/api/greeting";
    }

    @AfterClass
    public static void teardown() throws Exception {
        openshift.cleanup();
    }

    @Test
    public void testServiceInvocation() {
        when()
                .get()
        .then()
                .statusCode(200)
                .body(containsString("Hello, World!"));
    }

    @Test
    public void testServiceInvocationWithParam() {
        given()
                .queryParam("name", "Peter")
        .when()
                .get()
        .then()
                .statusCode(200)
                .body(containsString("Hello, Peter!"));
    }
}
