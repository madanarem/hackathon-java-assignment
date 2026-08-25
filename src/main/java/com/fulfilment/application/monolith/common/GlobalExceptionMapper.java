package com.fulfilment.application.monolith.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.jboss.logging.Logger;

/**
 * Single application-wide JAX-RS exception mapper.
 *
 * <p>Previously {@code ProductResource} and {@code StoreResource} each declared their own
 * {@code @Provider ExceptionMapper<Exception>}. JAX-RS providers are registered application-wide
 * regardless of which resource class declares them, so having two mappers for the same generic
 * {@code Exception} type left it implementation-defined which one handled a given failure. This
 * class replaces both with one well-defined mapper, shared by every resource (Product, Store,
 * Warehouse).
 */
@Provider
public class GlobalExceptionMapper implements ExceptionMapper<Exception> {

  private static final Logger LOGGER = Logger.getLogger(GlobalExceptionMapper.class);

  @Inject ObjectMapper objectMapper;

  @Override
  public Response toResponse(Exception exception) {
    int code = 500;
    if (exception instanceof WebApplicationException webApplicationException) {
      code = webApplicationException.getResponse().getStatus();
    }

    if (code >= 500) {
      // Unexpected failure: log with full stack trace so it can be diagnosed.
      LOGGER.error("Unhandled exception while processing request", exception);
    } else {
      // Expected client error (validation, not-found, etc.): log briefly, no stack trace noise.
      LOGGER.warnf("Request rejected with status %d: %s", code, exception.getMessage());
    }

    ObjectNode exceptionJson = objectMapper.createObjectNode();
    exceptionJson.put("exceptionType", exception.getClass().getName());
    exceptionJson.put("code", code);

    if (exception.getMessage() != null) {
      exceptionJson.put("error", exception.getMessage());
    }

    return Response.status(code).entity(exceptionJson).build();
  }
}
