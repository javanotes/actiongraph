package org.reactiveminds.actiongraph.server;

import akka.actor.AbstractActor;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestHandler extends AbstractActor {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestHandler.class);

    @Override
    public Receive createReceive() {
        return receiveBuilder()
                .match(
                        HttpExchange.class,
                        message -> {
                            LOGGER.info("Handling request path {}", message.getRequestURI());
                            try {
                                HttpService.Response response = service(message);
                                if(response == null){
                                    message.sendResponseHeaders(HttpURLConnection.HTTP_NOT_FOUND, -1);
                                }
                                else{
                                    if(response.headers != null)
                                        message.getResponseHeaders().putAll(response.headers);
                                    message.getResponseHeaders().set("Content-Type", response.contentType);
                                    message.sendResponseHeaders(response.statusCode, response.content != null ? response.content.length() : -1);
                                    if (response.content != null) {
                                        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(message.getResponseBody()))){
                                            writer.write(response.content);
                                            writer.flush();
                                        }
                                    }
                                }

                            }
                            catch (UnsupportedOperationException e) {
                                message.sendResponseHeaders(HttpURLConnection.HTTP_NOT_IMPLEMENTED, -1);
                            }
                            catch (Exception e) {
                                LOGGER.error("uncaught service invocation error", e);
                                message.sendResponseHeaders(HttpURLConnection.HTTP_INTERNAL_ERROR, -1);
                            }
                            finally {
                                message.close();
                            }

                        }).build();
    }

    private static String getRequestBody(InputStream stream)  {
        StringBuilder str = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(stream))){
            String line;
            while((line = reader.readLine()) != null)
                str.append(line);
        } catch (IOException e) {
            LOGGER.error("cannot read request stream", e);
        }
        return str.toString();
    }
    private static Map<String, String> queryParams(URI uri){
        if(uri.getQuery() == null)
            return Collections.emptyMap();
        return Arrays.asList(uri.getQuery().split("&")).stream()
        .map(s -> {
            String[] split = s.split("=");
            return new AbstractMap.SimpleEntry<>(split[0], split[1]);
        }).collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
    }
    private HttpService.Response service(HttpExchange message) {
        ServiceRegistry.ServiceWrapper service = ServiceRegistry.getService(message.getRequestURI().getPath());
        if(service == null){
            LOGGER.warn("Unmapped url: {}", message.getRequestURI());
            return null;
        }
        Map<String, String> queryParams = queryParams(message.getRequestURI());
        Map<String, String> pathsParams = service.urlPattern.match(message.getRequestURI().getPath());

        HttpService.Request request = new HttpService.Request();
        request.queryParams = queryParams;
        request.pathParams = pathsParams;
        request.headers = message.getRequestHeaders();
        if("GET".equalsIgnoreCase(message.getRequestMethod())){
            return service.service.doGet(request);
        }
        else if("POST".equalsIgnoreCase(message.getRequestMethod())){
            String body = getRequestBody(message.getRequestBody());
            request.content = body;
            return service.service.doPost(request);
        }
        else
            throw new UnsupportedOperationException(message.getRequestMethod());
    }
}
