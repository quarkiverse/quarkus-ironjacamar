package io.quarkiverse.jca.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

import org.junit.jupiter.api.Test;

import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JcaResourceTest {

    @Test
    public void testTransacted() {
        given().when().get("/jca/transacted").then().statusCode(200).body(is("true"));
    }
}
