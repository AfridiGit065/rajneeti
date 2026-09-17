package com.rajneeti.bot.ai;

/**
 * Abstraction over the LLM HTTP call so the decision provider can be unit
 * tested with a stub transport (no real network / no real API key required).
 */
public interface AiHttpTransport {

    /**
     * Performs a single chat-completions POST.
     *
     * @param url        the provider endpoint (already includes the base URL)
     * @param apiKey     the Bearer API key to send
     * @param requestJson the serialized chat-completions request body
     * @param timeoutMs  request timeout in milliseconds
     * @return the raw response body on 2xx
     * @throws AiHttpException     on a non-2xx provider response
     * @throws java.io.IOException on transport-level failures (timeout, refused)
     * @throws InterruptedException if the caller thread is interrupted
     */
    String postChat(String url, String apiKey, String requestJson, long timeoutMs)
            throws AiHttpException, java.io.IOException, InterruptedException;

    /**
     * Performs a single POST to a provider that authenticates with the
     * {@code x-api-key} header instead of {@code Authorization: Bearer}
     * (e.g. the Anthropic Messages API). Implementations may fall back to
     * {@link #postChat} when the provider does not need the extra header.
     *
     * @return the raw response body on 2xx
     * @throws AiHttpException     on a non-2xx provider response
     * @throws java.io.IOException on transport-level failures (timeout, refused)
     * @throws InterruptedException if the caller thread is interrupted
     */
    default String postMessages(String url, String apiKey, String requestJson, long timeoutMs)
            throws AiHttpException, java.io.IOException, InterruptedException {
        return postChat(url, apiKey, requestJson, timeoutMs);
    }

    /**
     * Performs a single POST to a provider that authenticates with the
     * {@code x-goog-api-key} header instead of {@code Authorization: Bearer}
     * (the Google Gemini {@code generateContent} endpoint). Implementations may
     * fall back to {@link #postChat}.
     *
     * @return the raw response body on 2xx
     * @throws AiHttpException     on a non-2xx provider response
     * @throws java.io.IOException on transport-level failures (timeout, refused)
     * @throws InterruptedException if the caller thread is interrupted
     */
    default String postGenerate(String url, String apiKey, String requestJson, long timeoutMs)
            throws AiHttpException, java.io.IOException, InterruptedException {
        return postChat(url, apiKey, requestJson, timeoutMs);
    }
}