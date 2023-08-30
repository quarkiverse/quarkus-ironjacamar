package io.quarkiverse.ironjacamar.it;

import static io.restassured.RestAssured.given;

import org.junit.jupiter.api.Test;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = ArtemisTestResource.class, restrictToAnnotatedClass = true)
public class JcaResourceTest {

    @Test
    public void testProducer() {
        given().when().get("/jca?name=George").then().statusCode(204);
    }

    @Test
    public void testProducerRollback() {
        given().when().get("/jca?name=rollback").then().statusCode(204);
    }

}
