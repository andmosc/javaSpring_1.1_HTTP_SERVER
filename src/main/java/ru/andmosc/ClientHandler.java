package ru.andmosc;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.*;
import java.net.Socket;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;

public class ClientHandler implements Runnable {
    private final static List<String> validPaths = List.of("/index.html", "/spring.svg", "/spring.png"
            , "/resources.html", "/styles.css", "/app.js", "/links.html", "/forms.html"
            , "/classic.html", "/events.html", "/events.js");
    public static final String GET = "GET";
    public static final String POST = "POST";
    private final Socket socket;
    private final static List<String> allowedMethods = List.of("GET", "POST");

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (
                socket;
                final BufferedInputStream in = new BufferedInputStream(new BufferedInputStream(socket.getInputStream()));
                final BufferedOutputStream out = new BufferedOutputStream(socket.getOutputStream())
        ) {
            Request request = createRequest(in, out);
            Handler handler = Server.getHandlers().get(request.getMethod()).get(request.getPath());
            
            if (handler == null) {
                out.write(errResponse().getBytes());
                out.flush();
            } else {
                handler.handle(request, out);
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Request createRequest(BufferedInputStream in, BufferedOutputStream out) throws IOException, URISyntaxException {
        final int limit = 4096;

        in.mark(limit);
        final byte[] buffer = new byte[limit];
        final int read = in.read(buffer);

        final byte[] requestLineDelimeter = new byte[]{'\r', '\n'};

        final int requestLineEnd = indexOf(buffer, requestLineDelimeter, 0, read);

        if (requestLineEnd == -1) {
            out.write(errResponse().getBytes());
            out.flush();
            return null;
        }

//requestLine
        final String[] requestLine = new String(Arrays.copyOf(buffer, requestLineEnd)).split(" ");
        if (requestLine.length != 3) {
            out.write(errResponse().getBytes());
            out.flush();
            return null;
        }

        final String method = requestLine[0];
        final String pathParam = requestLine[1];
        final String versionalHTTP = requestLine[2];

        if (!allowedMethods.contains(method)) {
            out.write(errResponse().getBytes());
            out.flush();
            return null;
        }

        System.out.println(method);

//headers
        final byte[] headersDelimeter = new byte[]{'\r', '\n', '\r', '\n'};
        final int headersStart = requestLineEnd + requestLineDelimeter.length;

        final int headersEnd = indexOf(buffer, headersDelimeter, headersStart, read);

        if (headersEnd == -1) {
            out.write(errResponse().getBytes());
            out.flush();
            return null;
        }

        in.reset();
        in.skip(headersStart);

        final byte[] headersBytes = in.readNBytes(headersEnd - headersStart);
        final List<String> headers = Arrays.asList(new String(headersBytes).split("\r\n"));
        System.out.println(headers);

//for POST
        String body = null;
        if (method.equals(POST)) {
            in.skip(headersDelimeter.length);
            final Optional<String> contentLenght = extractHeaders(headers, "Content-Lenght");
            if (contentLenght.isPresent()) {
                final int lenght = Integer.parseInt(contentLenght.get());
                final byte[] bodyBytes = in.readNBytes(lenght);
                body = new String(bodyBytes);
                System.out.println(body);
            }
        }

//queryString
        final URI uri = new URI(pathParam);
        final String path;

        List<NameValuePair> queryString = URLEncodedUtils.parse(uri, StandardCharsets.UTF_8);
        if (!queryString.isEmpty()) {
            path = pathParam.substring(0, pathParam.indexOf('?'));
        } else {
            path = pathParam;
        }

        return new Request(method, path, queryString, versionalHTTP, headers, body);
    }

    private Optional<String> extractHeaders(List<String> headers, String header) {
        return headers.stream().filter(h -> h.startsWith(header))
                .map(h -> h.substring(h.indexOf(" ")))
                .map(String::trim)
                .findFirst();
    }

    private static int indexOf(byte[] array, byte[] target, int start, int max) {
        outer:
        for (int i = start; i < max - target.length + 1; i++) {
            for (int j = 0; j < target.length; j++) {
                if (array[i + j] != target[j]) {
                    continue outer;
                }
            }
            return i;
        }
        return -1;
    }


    public static void responseServer(Request request, BufferedOutputStream out) throws IOException {
        final Path filePath = Path.of(".", "public", request.getPath());
        final String mimeType = Files.probeContentType(filePath);

        if (request.getPath().equals("/classic.html")) {
            classicResponse(out, filePath, mimeType);
            return;
        }

        final long length = Files.size(filePath);
        out.write(okResponse(mimeType, length).getBytes());
        Files.copy(filePath, out);
        out.flush();
    }

    private static void classicResponse(BufferedOutputStream out, Path filePath, String mimeType) throws IOException {
        final String template = Files.readString(filePath);
        final byte[] content = template.replace(
                "{time}",
                LocalDateTime.now().toString()
        ).getBytes();
        out.write(okResponse(mimeType, content.length).getBytes());
        out.write(content);
        out.flush();
    }

    private static String errResponse() {
        return "HTTP/1.1 404 Not Found\r\n" +
                "Content-Length: 0\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    private static String okResponse(String mimeType, long length) {
        return "HTTP/1.1 200 OK\r\n" +
                "Content-Type: " + mimeType + "\r\n" +
                "Content-Length: " + length + "\r\n" +
                "Connection: close\r\n" +
                "\r\n";
    }

    public static List<String> getValidPaths() {
        return validPaths;
    }
}
