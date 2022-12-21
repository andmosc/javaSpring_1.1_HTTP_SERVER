package ru.andmosc;

import java.io.BufferedOutputStream;

public interface Handler {
    void handle(Request request, BufferedOutputStream bufferedOutputStream);
}
