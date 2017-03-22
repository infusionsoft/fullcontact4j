package com.fullcontact.api.libs.fullcontact4j;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fullcontact.api.libs.fullcontact4j.enums.RateLimiterConfig;
import com.fullcontact.api.libs.fullcontact4j.http.FCCallback;
import com.fullcontact.api.libs.fullcontact4j.http.FCRequest;
import com.fullcontact.api.libs.fullcontact4j.http.FCRequestHandler;
import com.fullcontact.api.libs.fullcontact4j.http.FCResponse;
import com.fullcontact.api.libs.fullcontact4j.http.FCRetrofitCallback;
import com.fullcontact.api.libs.fullcontact4j.http.RequestExecutorHandler;
import retrofit.RestAdapter;
import retrofit.client.Client;
import retrofit.converter.Converter;
import retrofit.converter.JacksonConverter;

import java.util.concurrent.ExecutorService;

/**
 * This interface holds references to all the objects used by a FullContact client to communicate with the api,
 * convert responses, etc.
 */
public class FullContactHttpInterface {

    /**
     * This handles requests made by the client.
     * When a request is made, it is sent to an ExecutorService which
     * accounts for rate limiting and then sends the request.
     */
    private FCRequestHandler requestHandler;
    private final Converter jsonConverter;
    private final String baseUrl;

    /**
     * Handles communication (using Retrofit) with the FullContact api
     */
    private FullContactApi fullContactApi;

    public FullContactHttpInterface(Client httpClient, RateLimiterConfig rateLimiterConfig, String baseUrl,
                                    ExecutorService executorService) {
        ObjectMapper mapper = new ObjectMapper();
        //Properties not present in the POJO are ignored instead of throwing exceptions
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        //An empty string ("") is interpreted as null
        mapper.enable(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT);

        if(rateLimiterConfig == RateLimiterConfig.DISABLED) {
            requestHandler = new FCRequestHandler.NoRateLimitRequestHandler();
        } else {
            requestHandler = new RequestExecutorHandler(rateLimiterConfig, executorService);
        }

        jsonConverter = new JacksonConverter(mapper);
        RestAdapter adapter = new RestAdapter.Builder().setEndpoint(baseUrl)
            .setClient(httpClient).setConverter(jsonConverter)
            .setLogLevel(RestAdapter.LogLevel.NONE)
            .build();
        fullContactApi = adapter.create(FullContactApi.class);
        this.baseUrl = baseUrl;
    }

    /**
     * Makes a request just like an async request, but uses a synchronous callback to do so.
     */
    public <T extends FCResponse> T sendRequest(FCRequest<T> req) throws FullContactException {
        return requestHandler.sendRequest(fullContactApi, req);
    }

    public <T extends FCResponse> void sendRequest(FCRequest<T> req, FCCallback<T> callback) {
        if(callback == null) {
            if(!req.hasParam(FCConstants.PARAM_WEBHOOK_URL)) {
                throw new IllegalArgumentException(
                        "Cannot make an asynchronous request without either a callback or a webhook");
            }

            //if the user didn't specify a callback, create a no-op callback instead
            callback = new FCCallback<T>() {
                @Override
                public void success(T response) {}

                @Override
                public void failure(FullContactException exception) {}
            };
        }
        //make a retrofit request with a callback that will call FCCallback
        requestHandler.sendRequestAsync(fullContactApi, req, new FCRetrofitCallback<T>(callback, this));
    }

    public FCRequestHandler getRequestHandler() { return requestHandler; }

    public String getBaseUrl() { return baseUrl; }

    public Converter getJsonConverter() {
        return jsonConverter;
    }

    protected void setRequestHandler(FCRequestHandler handler) {
        requestHandler = handler;
    }

}
