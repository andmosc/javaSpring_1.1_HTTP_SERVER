package ru.andmosc;

import java.io.IOException;

public class Main {

    public static final int PORT = 8080;
    public static final int SIZE_POOL = 64;

    public static void main(String[] args) {

        final Server server = new Server(PORT, SIZE_POOL);

        for (String validPath : ClientHandler.getValidPaths()) {
            server.addHandler("GET", validPath, (request, out) -> {
                try {
                    ClientHandler.responseServer(request,out);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }

        server.addHandler("POST", "/index.html", (request, out) -> {
            try {
                ClientHandler.responseServer(request,out);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });

        server.listen();
    }

}



