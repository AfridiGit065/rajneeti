package com.rajneeti.bot.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Default {@link AiHttpTransport} backed by the JDK {@link HttpClient}.
 *
 * <p>Each call gets its own request timeout and a shared connect timeout. The
 * HTTP body failures are translated into {@link AiHttpException} (auth status
 * codes let the {@link AiKeyManager} mark a key permanently invalid), while
 * transport failures surface as plain {@link IOException}.
 */
@Slf4j
@Component
public class HttpAiTransport implements AiHttpTransport {

    private final HttpClient client;

    public HttpAiTransport() {
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
    }

    @Override
    public String postChat(String url, String apiKey, String requestJson, long timeoutMs)
            throws AiHttpException, IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            log.debug("AI provider responded HTTP {} for {}", status, url);
            throw new AiHttpException(status,
                    "AI provider returned HTTP " + status
                            + (response.body() != null && !response.body().isBlank()
                            ? ": " + response.body() : ""));
        }
        return response.body();
    }

    @Override
    public String postMessages(String url, String apiKey, String requestJson, long timeoutMs)
            throws AiHttpException, IOException, InterruptedException {

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(timeoutMs))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                .build();

        HttpResponse<String> response = client.send(
                request, HttpResponse.BodyHandlers.ofString());

        int status = response.statusCode();
        if (status < 200 || status >= 300) {
            log.debug("AI provider responded HTTP {} for {}", status, url);
            throw new AiHttpException(status,
                    "AI provider returned HTTP " + status
                            + (response.body() != null && !response.body().isBlank()
                            ? ": " + response.body() : ""));
        }
        return response.body();
    }
}