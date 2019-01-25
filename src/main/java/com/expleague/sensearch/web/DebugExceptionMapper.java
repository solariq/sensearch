package com.expleague.sensearch.web;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Provider
public class DebugExceptionMapper implements ExceptionMapper<Throwable> {

  @Override
  public Response toResponse(Throwable t) {
    t.printStackTrace();

    return Response.serverError()
        .entity(ExceptionUtils.getStackTrace(t))
        .build();
  }
}