package ru.andmosc;

import org.apache.http.NameValuePair;
import java.util.List;
import java.util.stream.Collectors;

public class Request {
    private final String method;
    private final String path;
    private final String versionalHTTP;
    private final List<String> headers;
    private final String body;
    private final List<NameValuePair> queryString;

    public Request(
            String method, String path, List<NameValuePair> queryString,
            String versionalHTTP, List<String> headers, String body) {
        this.method = method;
        this.path = path;
        this.queryString = queryString;
        this.versionalHTTP = versionalHTTP;
        this.headers = headers;
        this.body = body;
    }

    public String getQueryParam(String name) {
        return queryString.stream().filter(item -> item.getName()
                        .equals(name)).map(NameValuePair::getValue)
                .collect(Collectors.joining("&"));
    }

    public List<NameValuePair> getQueryParams() {
        return queryString;
    }

    public String getMethod() {
        return method;
    }

    public String getPath() {
        return path;
    }
}
