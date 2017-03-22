package com.fullcontact.api.libs.fullcontact4j.http;

import com.fullcontact.api.libs.fullcontact4j.FullContactApi;

public interface FCRequestHandler {

    public void notifyRateLimits(FCResponse res, FCRateLimits rateLimits);

    public void shutdown();

    public <T extends FCResponse> void sendRequestAsync(final FullContactApi api, final FCRequest<T> req,
                                                        final FCRetrofitCallback<T> callback);

    public <T extends FCResponse> T sendRequest(final FullContactApi api, final FCRequest<T> req);

    class NoRateLimitRequestHandler implements FCRequestHandler {

        public void notifyRateLimits(FCResponse res, FCRateLimits rateLimits) {}

        public void shutdown() {}

        public <T extends FCResponse> void sendRequestAsync(FullContactApi api, FCRequest<T> req, FCRetrofitCallback
                    <T> callback) {
            req.makeRequest(api, callback);
        }

        public <T extends FCResponse> T sendRequest(FullContactApi api, FCRequest<T> req) {
            return req.makeRequest2(api);
        }
    }
}
