package io.quarkiverse.ironjacamar.reactive.it;

import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.quarkus.artemis.test.ArtemisTestResource;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;

@QuarkusTest
@QuarkusTestResource(value = ArtemisTestResource.class, restrictToAnnotatedClass = true)
public class ReactiveMessagingTest {

    @BeforeEach
    void setup() {
        given().when().delete("/messages").then().statusCode(204);
    }

    @Test
    public void shouldReceiveMessageViaReactiveMessaging() {
        given().queryParam("text", "Hello Reactive").post("/messages")
                .then().statusCode(204);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            given().when().get("/messages/count")
                    .then().statusCode(200).body(is("1"));
        });

        given().when().get("/messages")
                .then().statusCode(200).body("$", hasItem("Hello Reactive"));
    }

    @Test
    public void shouldReceiveMultipleMessages() {
        given().queryParam("text", "msg-1").post("/messages").then().statusCode(204);
        given().queryParam("text", "msg-2").post("/messages").then().statusCode(204);
        given().queryParam("text", "msg-3").post("/messages").then().statusCode(204);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            given().when().get("/messages/count")
                    .then().statusCode(200).body(is("3"));
        });

        given().when().get("/messages")
                .then().statusCode(200)
                .body("$", hasItem("msg-1"))
                .body("$", hasItem("msg-2"))
                .body("$", hasItem("msg-3"));
    }

    @Test
    public void shouldSendMessageViaOutgoing() {
        given().queryParam("text", "Hello Outgoing").post("/messages/outgoing")
                .then().statusCode(204);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            given().when().get("/messages/outgoing")
                    .then().statusCode(200).body(is("Hello Outgoing"));
        });
    }

    @Test
    public void shouldSendAndReceiveJsonPayload() {
        given().queryParam("orderId", "ORD-42").queryParam("quantity", 5)
                .post("/messages/outgoing/order")
                .then().statusCode(204);

        await().atMost(10, TimeUnit.SECONDS).untilAsserted(() -> {
            given().when().get("/messages/outgoing/order")
                    .then().statusCode(200)
                    .contentType(ContentType.JSON)
                    .body("orderId", is("ORD-42"))
                    .body("quantity", is(5));
        });
    }
}
