package com.fulfilment.application.monolith;

import static io.restassured.RestAssured.given;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class HealthCheckTest {

  @Test
  public void testHealthEndpointIsUp() {
    given().when().get("/q/health").then().statusCode(200).body("status", org.hamcrest.Matchers.equalTo("UP"));
  }

  @Test
  public void testLivenessEndpointIsUp() {
    given().when().get("/q/health/live").then().statusCode(200).body("status", org.hamcrest.Matchers.equalTo("UP"));
  }

  @Test
  public void testReadinessEndpointIsUp() {
    given().when().get("/q/health/ready").then().statusCode(200).body("status", org.hamcrest.Matchers.equalTo("UP"));
  }
}
