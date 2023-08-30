package io.quarkiverse.ironjacamar.it;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Topic;

import org.junit.jupiter.api.Test;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = ArtemisTestResource.class, restrictToAnnotatedClass = true)
public class ReactiveResourceTest {

    @Inject
    ConnectionFactory connectionFactory;

    @Test
    public void testProducer() throws Exception {
        try (JMSContext context = connectionFactory.createContext()) {
            Topic inventory = context.createTopic("inventory");
            JMSConsumer consumer = context.createConsumer(inventory);
            given().when().formParam("name", "George").post("/jca").then().statusCode(204);
            assertThat(consumer.receiveBody(String.class, 1000)).isEqualTo("Hello Reactive Messaging : Hello George");

            given().when().formParam("name", "rollback").post("/jca").then().statusCode(204);
            assertThat(consumer.receive(1000)).isNull();
        }
    }
}
