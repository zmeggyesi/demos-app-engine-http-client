package hu.skawa.skunkworks.demos.httpclient;

import com.sun.net.httpserver.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

public class AppEngineHttpClient {
	private static final Logger logger = LoggerFactory.getLogger(AppEngineHttpClient.class);

	public static void main(String[] args) {
		String port = System.getenv("PORT");
		logger.info("Creating HTTP server on port {}...", port);
		ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);

		try {
			HttpServer server = HttpServer.create(new InetSocketAddress("localhost", Integer.parseInt(port)), 0);
			server.createContext("/connect", exchange -> {
				logger.info("Starting outbound flight...");
				HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.of(5, ChronoUnit.SECONDS)).build();
				HttpRequest req = HttpRequest.newBuilder()
											 .GET()
											 .uri(URI.create("https://google.com/"))
											 .build();

				logger.info("Sending outbound request...");
				try {
					HttpResponse<String> response = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
					logger.info("Response code: {}", response.statusCode());

					byte[] successMessageBytes = SUCCESS_MESSAGE.getBytes(StandardCharsets.UTF_8);
					exchange.sendResponseHeaders(500, successMessageBytes.length);
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(successMessageBytes);
					outputStream.flush();
					outputStream.close();
				} catch (InterruptedException e) {
					logger.error("Request interrupted!", e);

					byte[] failureMessageBytes = FAILURE_MESSAGE.getBytes(StandardCharsets.UTF_8);
					exchange.sendResponseHeaders(500, failureMessageBytes.length);
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(failureMessageBytes);
					outputStream.flush();
					outputStream.close();
				} catch (Exception e) {
					logger.error("Some unknown error has occurred!", e);

					byte[] failureMessageBytes = FAILURE_MESSAGE.getBytes(StandardCharsets.UTF_8);
					exchange.sendResponseHeaders(500, failureMessageBytes.length);
					OutputStream outputStream = exchange.getResponseBody();
					outputStream.write(failureMessageBytes);
					outputStream.flush();
					outputStream.close();
				}

			});
			server.setExecutor(threadPoolExecutor);

			logger.info("Starting server...");
			server.start();

			logger.info("Server launched successfully...");
		} catch (IOException e) {
			logger.error("Could not start HTTP server!", e);
		}
	}

	private static final String SUCCESS_MESSAGE = "OP SUCCESS";

	private static final String FAILURE_MESSAGE = "OP FAILED";
}
