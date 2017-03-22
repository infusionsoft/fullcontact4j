package com.fullcontact.api.libs.fullcontact4j;


import com.fullcontact.api.libs.fullcontact4j.enums.RateLimiterConfig;
import com.fullcontact.api.libs.fullcontact4j.http.*;
import com.fullcontact.api.libs.fullcontact4j.http.cardreader.*;
import com.fullcontact.api.libs.fullcontact4j.http.company.CompanyRequest;
import com.fullcontact.api.libs.fullcontact4j.http.email.*;
import com.fullcontact.api.libs.fullcontact4j.http.location.LocationEnrichmentRequest;
import com.fullcontact.api.libs.fullcontact4j.http.location.LocationNormalizationRequest;
import com.fullcontact.api.libs.fullcontact4j.http.misc.AccountStatsRequest;
import com.fullcontact.api.libs.fullcontact4j.http.misc.DisposableEmailRequest;
import com.fullcontact.api.libs.fullcontact4j.http.name.*;
import com.fullcontact.api.libs.fullcontact4j.http.person.PersonRequest;

import okhttp3.OkHttpClient;
import retrofit.appengine.UrlFetchClient;
import retrofit.client.Client;

import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.*;
import java.util.logging.Level;

public class FullContact {
    protected static Level logLevel = Level.OFF;

    /**
     * This interface holds references to all the objects used by a FullContact client to communicate with the api,
     * convert responses, etc.
     */
    protected FullContactHttpInterface httpInterface;
    private boolean isShutdown = false;

    protected FullContact(Client httpClient, RateLimiterConfig rateLimiterConfig, String baseUrl,
                          ExecutorService executorService) {
        httpInterface = new FullContactHttpInterface(httpClient, rateLimiterConfig, baseUrl, executorService);
        Utils.info("Created new FullContact client.");
    }

    /**
     * Set what level to log at (default OFF). Level.INFO will post important logs (client creations, errors).
     * Level.FINE will log FullContact4j's workflow.
     * @param log
     */
    public static void setLogLevel(Level log) {
        logLevel = log;
    }

    /**
     * Initializes a new FullContact Client Builder with an api key.
     * This is the most common way to create a client.
     * @param apiKey an api key associated with an account.
     * @return a new FullContact client builder
     */
    public static Builder withApiKey(String apiKey) {
        return new Builder(apiKey);
    }

    //TODO update /developer/docs/libraries/
    /////API Methods//////

    /**
     * Creates a new Person search.
     */
    public PersonRequest.Builder buildPersonRequest() { return new PersonRequest.Builder(); }

    /**
     * Creates a new Company search.
     */
    public CompanyRequest.Builder buildCompanyRequest() { return new CompanyRequest.Builder(); }

    /**
     * Upload a new card to be processed by Card Reader.
     * @param front a ByteArrayOutputStream representing the picture of the front of the card
     */
    public CardReaderUploadRequest.Builder buildUploadCardRequest(InputStream front) { return new CardReaderUploadRequest.Builder().cardFront(front); }

    /**
     * View a single card.
     * @param id the card's ID
     */
    public CardReaderViewRequest.Builder buildCardReaderViewRequest(String id) { return new CardReaderViewRequest.Builder().cardId(id); }

    /**
     * View a history of your Card Reader requests with this api key, beginning with the earliest.
     */
    public CardReaderViewAllRequest.Builder buildCardReaderViewAllRequest() { return new CardReaderViewAllRequest.Builder(); }

    /**
     * Create a new disposable email check
     * @return a disposable email request builder with a pre-configured email to check
     */
    public DisposableEmailRequest.Builder buildDisposableEmailRequest(String email) { return new DisposableEmailRequest.Builder().email(email); }

    /**
     * Normalize a given name
     * @return a name normalization request builder
     */
    public NameNormalizationRequest.Builder buildNameNormalizationRequest(String name) { return new NameNormalizationRequest.Builder().query(name); }

    /**
     * Determine, given two names, which was a family name and which was a first name.
     * @return a parse request builder, pre-configured with the name
     */
    public NameParseRequest.Builder buildNameParseRequest(String name) { return new NameParseRequest.Builder().name(name); }

    /**
     * Deduces someone's real name from a username or email.
     */
    public NameDeduceRequest.Builder buildNameDeduceRequest() { return new NameDeduceRequest.Builder(); }

    /**
     * Use a variety of algorithms to determine the similarity of two names.
     * @return a name similarity request builder, pre-configured with the two names
     */
    public NameSimilarityRequest.Builder buildNameSimilarityRequest(String name1, String name2) { return new NameSimilarityRequest.Builder().name1(name1).name2(name2); }

    public NameStatsRequest.Builder buildNameStatsRequest() { return new NameStatsRequest.Builder(); }

    /**
     * Normalize a location from a given string
     * @return a location normalization request builder, pre-configured with the location
     */
    public LocationNormalizationRequest.Builder buildLocationNormalizationRequest(String place) { return new LocationNormalizationRequest.Builder().place(place); }

    /**
     * Enrich (gather more data about) a given location from a string.
     * @return a location enrichment request builder, pre-configured with the location
     */
    public LocationEnrichmentRequest.Builder buildLocationEnrichmentRequest(String place) { return new LocationEnrichmentRequest.Builder().place(place); }

    /**
     * Request stats about the api key in use.
     */
    public AccountStatsRequest.Builder buildAccountStatsRequest() { return new AccountStatsRequest.Builder(); }

    /**
     * Creates a new Email Verification request.
     */
    public EmailVerificationRequest.Builder buildEmailVerificationRequest() { return new EmailVerificationRequest.Builder(); }

    /**
     * Creates a new Email Verification Batch Submit request.
     */
    public EmailVerificationBatchRequest.Builder buildEmailVerificationBatchRequest() {
        return new EmailVerificationBatchRequest.Builder();
    }

    /**
     * Creates a new Email Verification Batch Get request.
     */
    public EmailVerificationGetBatchRequest.Builder buildEmailVerificationGetBatchRequest() {
        return new EmailVerificationGetBatchRequest.Builder();
    }

    /**
     * Makes a synchronous request to the FullContact APIs.
     * @throws FullContactException if the request fails, this method will throw a FullContactException with a reason.
     * @param req the request, generated with a call to build____Request().
     * @param <T> the Response type
     * @return if the request is successful, this method returns the corresponding {@link com.fullcontact.api.libs.fullcontact4j.http.FCResponse}.
     */
    public <T extends FCResponse> T sendRequest(FCRequest<T> req) throws FullContactException {
        return httpInterface.sendRequest(req);
    }

    /**
     * Makes an asynchronous request to the FullContact APIs.
     * Exceptions will call {@link com.fullcontact.api.libs.fullcontact4j.http.FCCallback#failure(FullContactException)}.
     * Successful responses will call {@link com.fullcontact.api.libs.fullcontact4j.http.FCCallback#success(com.fullcontact.api.libs.fullcontact4j.http.FCResponse)}
     * @param req the request, generated with a call to build____Request().
     * @param callback your callback
     * @param <T> the Response type
     */
    public <T extends FCResponse> void sendRequest(FCRequest<T> req, FCCallback<T> callback) {
        if(isShutdown) {
            throw new IllegalArgumentException("this client cannot make requests -- shutdown() has already been called");
        }
        httpInterface.sendRequest(req, callback);
    }

    public void shutdown() {
        isShutdown = true;
        httpInterface.getRequestHandler().shutdown();
    }


    /////////////////////


    public static class Builder {

        private String authKey;
        private Map<String, String> headers;
        private OkHttpClient httpClient = new OkHttpClient();
        private Client client = null;
        private String userAgent = "";
        private String baseUrl = FCConstants.API_BASE_DEFAULT;
        private RateLimiterConfig rateLimiterConfig = RateLimiterConfig.SMOOTH;
        private ExecutorService rateLimitExecutorService = Executors.newCachedThreadPool();

        public Builder(String apiKey) {
            //default client is OkHttpClient
            this.authKey = apiKey;
        }

        /**
         * What HTTP client FullContact should use; useful to configure read timeouts, etc.
         * By default, FullContact uses a default {@link retrofit.client.OkClient}.
         * @param client the configured http client
         * @return
         */
        public Builder httpClient(OkHttpClient client) {
            httpClient = client;
            return this;
        }

        public Builder client(Client client) {
            this.client = client;
            return this;
        }

        public Builder customHeaders(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        /**
         * How many threads to use to execute API queries (default 1).
         * @param threads
         * @return
         */
        public Builder threadCount(Integer threads) {
            rateLimitExecutorService = Executors.newFixedThreadPool(threads);
            return this;
        }

        /**
         * Configure the thread pool used to coordinate the rate limiting functionality of the client.
         * Defaults to Executors.newCachedThreadPool().
         */
        public Builder rateLimitExecutorService(ExecutorService rateLimitExecutorService) {
            this.rateLimitExecutorService = rateLimitExecutorService;
            return this;
        }

        /**
         * Sets the read timeout.
         *
         * @deprecated use OkHttpClient.Builder.readTimeout and the httpClient method on this builder.
         */
        @Deprecated
        public Builder setDefaultClientReadTimeout(Integer timeoutMs) {
            httpClient = httpClient.newBuilder()
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
            return this;
        }

        /**
         * Sets the connect timeout.
         *
         * @deprecated use OkHttpClient.Builder.connectTimeout and the httpClient method on this builder.
         */
        public Builder setDefaultClientConnectTimeout(Integer timeoutMs) {
            httpClient = httpClient.newBuilder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();
            return this;
        }

        /**
         * Sets the user agent string used in all headers by the FullContact client (not just the default).
         */
        public Builder setUserAgent(String agent) {
            userAgent = agent;
            return this;
        }

        /**
         * Sets the base URL that the client accesses FullContact's endpoints from.
         * This defaults to the standard api.fullcontact.com/v2/.
         * @param url
         * @return
         */
        public Builder baseUrl(String url) {
            baseUrl = url;
            return this;
        }

        /**
         * @see com.fullcontact.api.libs.fullcontact4j.enums.RateLimiterConfig
         */
        public Builder rateLimiterConfig(RateLimiterConfig rateLimiterConfig) {
            this.rateLimiterConfig = rateLimiterConfig;
            return this;
        }

        /**
         * Builds a new FullContact client.
         * @return a new, fully configured, FullContact client.
         */
        public FullContact build() {
            if(authKey == null || authKey.isEmpty()) {
                throw new IllegalArgumentException("Authentication key cannot be null");
            }
            if(rateLimiterConfig == null || baseUrl == null || rateLimitExecutorService == null ||
                userAgent == null || (httpClient == null && client == null)) {
                throw new IllegalArgumentException("One of the builder parameters was null");
            }

            if (client != null) {
                return new FullContact(new FCGAEUrlClient(userAgent, headers, client, authKey), rateLimiterConfig, baseUrl, rateLimitExecutorService);
            } else {
                return new FullContact(new FCUrlClient(userAgent, headers, httpClient, authKey), rateLimiterConfig, baseUrl, rateLimitExecutorService);
            }
        }
    }
}
