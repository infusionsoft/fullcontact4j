package com.fullcontact.api.libs.fullcontact4j.http;
/*
 * Copyright (C) 2013 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import com.fullcontact.api.libs.fullcontact4j.FCConstants;
import com.fullcontact.api.libs.fullcontact4j.Utils;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.Request;
import retrofit.client.Response;
import retrofit.mime.TypedOutput;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FCGAEUrlClient implements Client {
    private static final int CHUNK_SIZE = 4096;
    private static final int CONNECT_TIMEOUT_MILLIS = 15 * 1000; // 15s
    private static final int READ_TIMEOUT_MILLIS = 20 * 1000; // 20s

    private Map<String, String> headers = new HashMap<String, String>();
    private Client client;
    private String apiKey;
    private String userAgent;
    public FCGAEUrlClient(String userAgent, Map<String, String> customHeaders, Client client, String apiKey) {
        this.client = client;

        if(customHeaders != null) {
            this.headers = customHeaders;
        }

        //disallow api key, token, or user agent headers to be supplied by the user
        boolean removedBlocked = headers.remove(FCConstants.HEADER_AUTH_API_KEY) != null;
        removedBlocked |= headers.remove(FCConstants.HEADER_AUTH_ACCESS_TOKEN) != null;
        if(removedBlocked) {
            Utils.info("Custom FullContact header for api key or access token was supplied. It has been ignored.");
        }

        this.userAgent = userAgent;
        this.apiKey = apiKey;
    }

    public Response execute(Request request) throws IOException {
        Request modifiedRequest = prepareRequest(request);
        return client.execute(modifiedRequest);
    }

    protected Request prepareRequest(Request request) throws IOException {
        List<Header> requestHeaders = new ArrayList(request.getHeaders());

        //add custom global headers
        for(Map.Entry<String, String> header : headers.entrySet()) {
            if(header.getKey() != null && header.getValue() != null) {
                requestHeaders.add(new Header(header.getKey(), header.getValue()));
            } else {
                Utils.verbose("Ignored null header in request (Key: " + header.getKey() + ", Value: " + header.getValue() + ")");
            }
        }

        boolean hasAuthToken = false;
        for(Header header : request.getHeaders()) {
            if(header.getName().equals(FCConstants.HEADER_AUTH_ACCESS_TOKEN)) {
                hasAuthToken = true;
            }
        }
        if(!hasAuthToken) {
            requestHeaders.add(new Header(FCConstants.HEADER_AUTH_API_KEY, apiKey));
            Utils.verbose("Added API key to headers");
        } else {
            Utils.verbose("Added auth token instead of API key to headers");
        }
        requestHeaders.add(new Header(FCConstants.HEADER_USER_AGENT, FCConstants.USER_AGENT_BASE + " " + userAgent));

        TypedOutput body = request.getBody();
        if (body != null) {
            requestHeaders.add(new Header("Content-Type", body.mimeType()));
        }

        Request retval = new Request(request.getMethod(), request.getUrl(), requestHeaders, request.getBody());
        return retval;
    }
}
