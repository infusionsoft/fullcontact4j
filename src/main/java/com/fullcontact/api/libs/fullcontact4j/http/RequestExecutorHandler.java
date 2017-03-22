package com.fullcontact.api.libs.fullcontact4j.http;

import com.fullcontact.api.libs.fullcontact4j.FCConstants;
import com.fullcontact.api.libs.fullcontact4j.FullContact;
import com.fullcontact.api.libs.fullcontact4j.FullContactApi;
import com.fullcontact.api.libs.fullcontact4j.Utils;
import com.fullcontact.api.libs.fullcontact4j.enums.RateLimiterConfig;
import com.fullcontact.api.libs.fullcontact4j.guava.SmoothRateLimiter;
import com.fullcontact.api.libs.fullcontact4j.http.person.PersonRequest;
import com.fullcontact.api.libs.fullcontact4j.http.person.PersonResponse;
import retrofit.client.Header;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This class handles requests made by the client.
 * When a request is made, it is sent to an ExecutorService which
 * accounts for rate limiting and then sends the request.
 */
public class RequestExecutorHandler implements FCRequestHandler {
    // how often to check for a rate limit change
    private static final long RATE_LIMIT_CHECK_INTERVAL_MS = TimeUnit.MINUTES.convert(5, TimeUnit.MILLISECONDS);

    //will execute the requests on a separate thread.
    protected final ExecutorService executorService;

    //if not null, will limit the request rate.
    private SmoothRateLimiter.SmoothBursty rateLimiter;
    private double apiKeyRequestsPerSecond;
    private FCRateLimits lastKnownRateLimits;
    private volatile long lastRateLimitCheck = 0;

    private RequestDebtTracker requestDebtTracker = new RequestDebtTracker();

    public RequestExecutorHandler(RateLimiterConfig rateLimiterConfig, ExecutorService executorService) {
        this.executorService = executorService;
        apiKeyRequestsPerSecond = rateLimiterConfig.getInitReqsPerSec();
        rateLimiter = rateLimiterConfig.createRateLimiter();
    }
    /**
     * If the check interval time has passed, update the rate limit
     */
    private void updateRateLimit() {
        rateLimiter.setRate(apiKeyRequestsPerSecond);
        lastRateLimitCheck = System.currentTimeMillis();
    }

    public synchronized void notifyRateLimits(FCResponse res, FCRateLimits rateLimits) {
        if(!(res instanceof PersonResponse)) {
            return; // not person api headers, ignore
        }

        int requestsRemaining = rateLimits.getRequestsRemaining();
        int secondsToReset = rateLimits.getSecondsToReset();

        lastKnownRateLimits = rateLimits;

        if (shouldUpdateRateLimit()) {
            updateRateLimit();
        }
        
        //are we out of requests for this session?
        if(requestsRemaining <= apiKeyRequestsPerSecond && lastKnownRateLimits.getSecondsToReset() != 0) {
            Utils.info("To keep in line with rate limit headers, FC4J is waiting " + secondsToReset + "s " +
             "to the new rate limit period.");
            requestDebtTracker.registerDebt(secondsToReset * 1000);
        }
    }

    /**
     * Has RATE_LIMIT_CHECK time passed since we last made a rate limit update?
     */
    private boolean shouldUpdateRateLimit() {
        return System.currentTimeMillis() - lastRateLimitCheck > RATE_LIMIT_CHECK_INTERVAL_MS;
    }

    public <T extends FCResponse> void sendRequestAsync(final FullContactApi api, final FCRequest<T> req,
                                                        final FCRetrofitCallback<T> callback) {
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                // account for rate limits for Person API only
                if(req instanceof PersonRequest) {
                    //wait until this request would be made within API key limits
                    waitForPermit();
                    //wait until this request would be made within rate limit header limits
                    requestDebtTracker.consumeDebt();
                }

                Utils.verbose("Sending a new asynchronous " + req.getClass().getSimpleName());
                req.makeRequest(api, callback);
            }
        });
    }

    public <T extends FCResponse> T sendRequest(FullContactApi api, FCRequest<T> req) {
        return req.makeRequest2(api);
    }

    protected void waitForPermit() {
        if(rateLimiter != null) {
            Utils.verbose("Waiting for ratelimiter to allow a request... (" + rateLimiter.getRate() + " reqs/s)");
            rateLimiter.acquire();
        }
    }

    public void shutdown() {
        executorService.shutdown();
    }


    /**
     * FullContact will provide headers with the amount of requests remaining in the current rate limit session.
     * If we have 0 requests remaining in the current period we can have the client
     * sleep the client until we would not exceed rate limit.
     */
    private class RequestDebtTracker {

        /**
         * The amount, in milliseconds, to sleep.
         */
        private volatile int debt = 0;

        /**
         * Registers the amount of debt to consume (as long as it's more debt than registered right now).
         */
        public synchronized void registerDebt(int debt) {
            //disable creating permits until this debt is paid off
            RequestExecutorHandler.this.rateLimiter.disableBursting();
            if (this.debt < debt) {
                Utils.verbose("Registering debt of " + debt + "ms to account for rate limit headers.");
                this.debt = debt;
            }
        }

        /**
         * Sleep the current thread until the debt is consumed.
         * Other threads will block until this debt is consumed.
         * If more debt is registered while this is being consumed, it'll consume that, as well.
         */
        private synchronized void consumeDebt() {
            while (debt > 0) {
                int copy = debt;
                debt = 0;
                try {
                    Utils.verbose(System.currentTimeMillis() + " Consuming " + copy + "ms of request debt.");
                    Thread.sleep(copy);
                } catch (InterruptedException e) {
                    Utils.info("[WARN] Interrupted while consuming request debt! Exception: " + e.getMessage());
                }
            }
            //allow creating permits
            RequestExecutorHandler.this.rateLimiter.enableBursting(apiKeyRequestsPerSecond);
        }

    }
}
