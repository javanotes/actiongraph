package org.reactiveminds.actiongraph.server;

import java.util.List;
import java.util.Map;

public interface HttpService {
    Response doGet(Request request);
    Response doPost(Request request);
    String pathPattern();
    String method();
    class Response {
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        String content;
        int statusCode;
        Map<String, List<String>> headers;
        String contentType = "application/json";
    }
    class Request {
        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }

        public Map<String, List<String>> getHeaders() {
            return headers;
        }

        public void setHeaders(Map<String, List<String>> headers) {
            this.headers = headers;
        }

        public Map<String, String> getQueryParams() {
            return queryParams;
        }

        public void setQueryParams(Map<String, String> queryParams) {
            this.queryParams = queryParams;
        }

        public Map<String, String> getPathParams() {
            return pathParams;
        }

        public void setPathParams(Map<String, String> pathParams) {
            this.pathParams = pathParams;
        }

        String content;
        Map<String,List<String>> headers;
        Map<String, String> queryParams;
        Map<String, String> pathParams;
    }
}
