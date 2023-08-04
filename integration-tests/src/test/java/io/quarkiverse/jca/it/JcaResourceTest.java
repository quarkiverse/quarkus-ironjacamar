package io.quarkiverse.jca.it;

import jakarta.inject.Inject;

import org.junit.jupiter.api.Test;

import io.quarkiverse.jca.runtime.JCAConfig;
import io.quarkus.test.junit.QuarkusTest;

@QuarkusTest
public class JcaResourceTest {

    @Inject
    JCAConfig config;

    @Test
    public void testHelloEndpoint() {
        System.out.println(config.namedResourceAdapters());
    }
}
