/*
 * Copyright 2018 BloomReach Inc. (http://www.bloomreach.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.bloomreach.forge.webarchiving.archivemanagers.archiefweb;

import java.util.Map;

import org.bloomreach.forge.webarchiving.cms.util.Discoverable;
import org.bloomreach.forge.webarchiving.cms.util.PlatformManaged;
import org.bloomreach.forge.webarchiving.common.api.WebArchiveManager;
import org.bloomreach.forge.webarchiving.common.error.WebArchiveUpdateException;
import org.bloomreach.forge.webarchiving.common.model.WebArchiveUpdate;
import org.hippoecm.hst.site.HstServices;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.onehippo.cms7.crisp.api.broker.ResourceServiceBroker;
import org.onehippo.cms7.crisp.api.exchange.ExchangeHintBuilder;
import org.onehippo.cms7.crisp.api.resource.Resource;
import org.onehippo.cms7.crisp.hst.module.CrispHstServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ArchiefWebArchiveManager implements WebArchiveManager, PlatformManaged, Discoverable {
    private static final Logger log = LoggerFactory.getLogger(ArchiefWebArchiveManager.class);

    protected static final String CONFIG_PROP_AUTH_ENDPOINT = "archiefweb.api.endpoint.auth";
    protected static final String CONFIG_PROP_REQUEST_ENDPOINT = "archiefweb.api.endpoint.request";
    protected static final String CONFIG_PROP_USERNAME = "archiefweb.api.username";
    protected static final String CONFIG_PROP_PASSWORD = "archiefweb.api.password";

    private String authEndpoint;
    private String requestEndpoint;
    private String username;
    private String password;

    @Override
    public void initialize(final Map<String, String> props) throws WebArchiveUpdateException {
        authEndpoint = props.get(CONFIG_PROP_AUTH_ENDPOINT);
        if (authEndpoint == null) {
            throw new WebArchiveUpdateException("Please configure the authentication endpoint (" + CONFIG_PROP_AUTH_ENDPOINT + ")");
        }
        requestEndpoint = props.get(CONFIG_PROP_REQUEST_ENDPOINT);
        if (requestEndpoint == null) {
            throw new WebArchiveUpdateException("Please configure the request endpoint (" + CONFIG_PROP_REQUEST_ENDPOINT + ")");
        }
        username = props.get(CONFIG_PROP_USERNAME);
        if (username == null) {
            throw new WebArchiveUpdateException("Please configure api username (" + CONFIG_PROP_USERNAME + ")");
        }
        password = props.get(CONFIG_PROP_PASSWORD);
        if (password == null) {
            throw new WebArchiveUpdateException("Please configure api password (" + CONFIG_PROP_PASSWORD + ")");
        }
    }

    @Override
    public void destroy() {
        log.debug("Destroying {}", this.getClass().getName());
    }

    @Override
    public synchronized void requestUpdate(final WebArchiveUpdate update) throws WebArchiveUpdateException {
        try {
            ResourceServiceBroker broker = CrispHstServices.getDefaultResourceServiceBroker(HstServices.getComponentManager());
            Resource authInfo = broker.resolve("archiefWebEndpoint", authEndpoint,
                    ExchangeHintBuilder.create()
                            .methodName("POST")
                            .requestHeader("Content-Type", "application/json;charset=UTF-8")
                            .requestBody("{\"email\":\"" + username + "\",\n" +
                                    " \"password\":\"" + password + "\"}")
                            .build());

            if ("Authentication successful.".equals(authInfo.getValue("message"))) {
                String jwtToken = authInfo.getValue("jwt", String.class);
                Resource operationResource = broker.resolve("archiefWebEndpoint", requestEndpoint,
                        ExchangeHintBuilder.create()
                                .methodName("POST")
                                .requestHeader("Content-Type", "application/json;charset=UTF-8")
                                .requestHeader("Authorization", "Bearer " + jwtToken)
                                .requestBody(createRequestBody(update))
                                .build());

                System.out.println(operationResource.getValue("message"));
                if ("Action completed successfully.".equals(operationResource.getValue("message"))) {
                    System.out.println("SUCCESS!!");
                } else {
                    throw new WebArchiveUpdateException("Web archive update failed " + update);
                }

            } else {
                throw new WebArchiveUpdateException("Authentication failed, web archive update " + update);
            }
        } catch (Throwable e) {
            //TODO DO SOMETHING
        }
    }

    private String createRequestBody(final WebArchiveUpdate update) throws WebArchiveUpdateException {
        JSONObject payload = new JSONObject();
        try {
            payload.put("url", new JSONArray(update.getUrls()));
            payload.put("action", "add");
        } catch (JSONException e) {
            throw new WebArchiveUpdateException("Error while creating web archive update request", e);
        }


        return payload.toString();
        /*return "{\"url\":\n" +
                " [\"https://www.politie.nl/aangifte-of-meldingdoen\",\"https://www.politie.nl/mijnbuurt\"],\n" +
                " \"action\":\"add\"\n" +
                "}";*/
    }
}
