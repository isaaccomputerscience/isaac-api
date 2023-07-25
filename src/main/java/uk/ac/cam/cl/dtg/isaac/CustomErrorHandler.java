package uk.ac.cam.cl.dtg.isaac;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;

import java.io.IOException;

public class CustomErrorHandler extends ErrorHandler {
    @Override
    public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) throws IOException {
        // Set the status code
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);

        // Set the content type
        response.setContentType("text/html; charset=utf-8");

        // Write the custom error message
        response.getWriter().println("<h1>Custom Error Page</h1>");
        response.getWriter().println("<p>An error occurred while processing your request.</p>");

        // Mark the request as handled so Jetty doesn't continue trying to handle it
        baseRequest.setHandled(true);
    }
}
