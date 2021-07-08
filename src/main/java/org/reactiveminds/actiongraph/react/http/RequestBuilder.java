package org.reactiveminds.actiongraph.react.http;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple REST client for JSON payloads.
 */
public class RequestBuilder {

    private static final Map<Integer, String> HTTP_CODES = new HashMap<>();
    static {
        Arrays.asList(HttpURLConnection.class.getDeclaredFields()).stream()
                .filter(field -> Modifier.isStatic(field.getModifiers()) && Modifier.isPublic(field.getModifiers()) && Modifier.isFinal(field.getModifiers()) && field.getType() == Integer.TYPE)
                .forEach(field -> {
                    field.setAccessible(true);
                    try {
                        HTTP_CODES.put(field.getInt(null), field.getName());
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    }
                });
    }
    private RequestBuilder(URLConnection urlConnection){
        this.urlConnection = (HttpURLConnection) urlConnection;
        isHttps = urlConnection instanceof HttpsURLConnection;
        addHeader("Accept-Charset", "utf-8");
        addHeader("Accept", "application/json");
    }
    private final boolean isHttps;
    private final HttpURLConnection urlConnection;
    public static RequestBuilder open(String url, Map<String, String> params) throws IOException {
        StringBuilder urlStr = new StringBuilder(url);
        if(params != null && !params.isEmpty()){
            urlStr.append("?");
            boolean first = true;
            for (Map.Entry<String, String> param : params.entrySet()) {
                if(first){
                    urlStr.append(param.getKey()).append("=").append(URLEncoder.encode(param.getValue(), "utf-8"));
                    first = false;
                }
                else {
                    urlStr.append("&").append(param.getKey()).append("=").append(URLEncoder.encode(param.getValue(), "utf-8"));
                }
            }

        }
        return new RequestBuilder(new URL(urlStr.toString()).openConnection());
    }
    public RequestBuilder addHeader(String key, String value){
        urlConnection.addRequestProperty(key, value);
        return this;
    }
    public RequestBuilder setContentType(String value){
        return addHeader("Content-Type", value);
    }
    public RequestBuilder setReadTimeout(Duration duration){
        urlConnection.setReadTimeout((int) duration.toMillis());
        return this;
    }
    public RequestBuilder setConnectTimeout(Duration duration){
        urlConnection.setConnectTimeout((int) duration.toMillis());
        return this;
    }
    private String readResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))){
            String line;
            while ((line = reader.readLine()) != null){
                response.append(line);
            }
        }
        return response.toString();
    }
    private String readError() throws IOException {
        StringBuilder response = new StringBuilder();
        try(BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getErrorStream()))){
            String line;
            while ((line = reader.readLine()) != null){
                response.append(line);
            }
        }
        return response.toString();
    }
    public Response doGet(){
        try {
            urlConnection.setRequestMethod("GET");
            int responseCode = urlConnection.getResponseCode();
            Response response = new Response();
            if(!String.valueOf(responseCode).startsWith("2")){
                response.setSuccess(false);
                response.setContent(readError());
            }else {
                response.setSuccess(true);
                response.setContent(readResponse());
            }
            response.setCode(responseCode);
            response.setStatus(HTTP_CODES.getOrDefault(responseCode, ""));
            return response;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        finally {
            urlConnection.disconnect();
        }
    }
    public Response doPost(String content){
        try {
            setContentType("application/json");
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoOutput(true);
            OutputStream out = urlConnection.getOutputStream();
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
            int responseCode = urlConnection.getResponseCode();
            Response response = new Response();
            if(!String.valueOf(responseCode).startsWith("2")){
                response.setSuccess(false);
                response.setContent(readError());
            }else {
                response.setSuccess(true);
                response.setContent(readResponse());
            }
            response.setCode(responseCode);
            response.setStatus(HTTP_CODES.getOrDefault(responseCode, ""));
            return response;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        finally {
            urlConnection.disconnect();
        }
    }
}
