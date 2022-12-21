package ru.andmosc;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Server {
    private final int PORT;
    private final ExecutorService threadPool;
    private static final Map<String, Map<String, Handler>> handlers = new ConcurrentHashMap<>();

    public Server(int PORT, int poolSize) {
        this.PORT = PORT;
        threadPool = Executors.newFixedThreadPool(poolSize);
    }

    public void listen() {
        try (final ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                final Socket socket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(socket);
                threadPool.execute(clientHandler);
            }
        } catch (IOException e) {
            try {
                if (!threadPool.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                    threadPool.shutdownNow();
                }
            } catch (InterruptedException exception) {
                threadPool.shutdownNow();
            }
        }
    }

    public void addHandler(String method, String path, Handler handler) {
        if(!handlers.containsKey(method)) {
            Map<String,Handler> map = new ConcurrentHashMap<>();
            map.put(path,handler);
            handlers.put(method,map);
        } else {
            handlers.get(method).put(path,handler);
        }
    }

    public static Map<String, Map<String, Handler>> getHandlers() {
        return handlers;
    }
}
