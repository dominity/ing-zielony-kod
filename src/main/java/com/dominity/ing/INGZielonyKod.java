package com.dominity.ing;

import com.dominity.ing.atm.service.ATMService;
import com.dominity.ing.onlinegame.service.OnlineGameService;
import com.dominity.ing.transactions.service.TransactionsService;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;

public class INGZielonyKod {
    private static final ATMService ATM_SERVICE = new ATMService();
    private static final TransactionsService TRANSACTIONS_SERVICE = new TransactionsService();
    private static final OnlineGameService ONLINE_GAME_SERVICE = new OnlineGameService();

    public static void main(String[] args) {
        var host = "localhost";
        var port = 8080;
        var backlog = 100;
        var httpRequestHandlingThreads = 30;
        HttpServer httpServer = null;
        try {
            httpServer = HttpServer.create(new InetSocketAddress(host, port), backlog);
        } catch (IOException e) {
            e.printStackTrace();
        }
        httpServer.setExecutor(Executors.newFixedThreadPool(httpRequestHandlingThreads));
        httpServer.createContext("/transactions/report", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                try (var inputStream = exchange.getRequestBody();
                        var outputStream = exchange.getResponseBody()) {
                    exchange.sendResponseHeaders(200, 0);
                    TRANSACTIONS_SERVICE.prepareTotal(inputStream, outputStream);
                }
            } else {
                System.out.println("Unexpected method");
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().close();
            }
        });
        httpServer.createContext("/onlinegame/calculate", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                try (var inputStream = exchange.getRequestBody();
                        var outputStream = exchange.getResponseBody()) {
                    exchange.sendResponseHeaders(200, 0);
                    ONLINE_GAME_SERVICE.groupClans(inputStream, outputStream);
                }
            } else {
                System.out.println("Unexpected method");
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().close();
            }
        });
        httpServer.createContext("/atms/calculateOrder", exchange -> {
            if ("POST".equals(exchange.getRequestMethod())) {
                try (var inputStream = exchange.getRequestBody();
                        var outputStream = exchange.getResponseBody()) {
                    exchange.sendResponseHeaders(200, 0);
                    ATM_SERVICE.prepareRoute(inputStream, outputStream);
                }
            } else {
                System.out.println("Unexpected method");
                exchange.sendResponseHeaders(404, 0);
                exchange.getResponseBody().close();
            }
        });
        httpServer.start();
    }
}
