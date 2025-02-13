package io.quarkiverse.ironjacamar.classloader;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import jakarta.inject.Inject;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.JMSConsumer;
import jakarta.jms.JMSContext;
import jakarta.jms.Message;
import jakarta.jms.Queue;

import org.junit.jupiter.api.Test;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
@QuarkusTestResource(value = ArtemisTestResource.class, restrictToAnnotatedClass = true)
public class MyResourceTest {

    @Inject
    ConnectionFactory connectionFactory;

    @Test
    void should_send_message() throws Exception {
        given()
                .when().post("/hello")
                .then()
                .statusCode(204);
        try (JMSContext context = connectionFactory.createContext()) {
            Queue queue = context.createQueue("reply");
            JMSConsumer consumer = context.createConsumer(queue);
            Message receive = consumer.receive(2000L);
            assertThat(receive).isNotNull();
            assertThat(receive.getBody(String.class)).isEqualTo("Hello, World!");
        }

    }
}
