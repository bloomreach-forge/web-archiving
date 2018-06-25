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

package org.onehippo.forge.webarchiving.cms.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;

import org.onehippo.repository.util.JcrConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CmsUtils {

    private static final Logger log = LoggerFactory.getLogger(CmsUtils.class);


    /**
     * Optional JCR property containing the name of the system property specifying the environment
     */
    private static final String ENVIRONMENT_SYSTEM_PROPERTY = "environment.system.property";

    /**
     * Default name of the system property specifying the environment
     */
    private static final String DEFAULT_ENVIRONMENT_SYSTEM_PROPERTY = "hippo.environment";


    /**
     * Get the environment specifier as set by System property 'hippo.environment' or by a configured one.
     *
     * @param configNode the node of the module config or as service sub-node config
     * @return System property value or 'default' if there is none.
     */
    public static String getEnvironment(final Node configNode) {

        String envProperty = DEFAULT_ENVIRONMENT_SYSTEM_PROPERTY;
        try {
            Node config = configNode;

            // argument may be service config
            if (!config.hasProperty(ENVIRONMENT_SYSTEM_PROPERTY)) {
                log.debug("No property {}/{} found, trying parent", config.getPath(), ENVIRONMENT_SYSTEM_PROPERTY);
                config = config.getParent();
            }

            if (config.hasProperty(ENVIRONMENT_SYSTEM_PROPERTY)) {
                log.debug("Reading configured system property name from JCR property {}/{}", config.getPath(), ENVIRONMENT_SYSTEM_PROPERTY);
                envProperty = config.getProperty(ENVIRONMENT_SYSTEM_PROPERTY).getString();
            } else {
                log.debug("No property {}/{} found, defaulting to {}", config.getPath(), ENVIRONMENT_SYSTEM_PROPERTY, DEFAULT_ENVIRONMENT_SYSTEM_PROPERTY);
            }
        } catch (RepositoryException re) {
            log.error("Error getting system property name from module config", re);
        }

        final String env = System.getProperty(envProperty, "default");
        log.debug("Used system property '{}' to read environment: result is '{}'", envProperty, env);
        return env;
    }

    /**
     * Get a map of configuration properties for a service, possibly overridden by an environment specific sub-node.
     */
    public static Map<String, String> getServiceConfiguration(final Node configNode, final String environment, final String... propertyNames) throws RepositoryException {
        final Map<String, String> config = getConfigurationProperties(configNode, propertyNames);
        log.debug("Got default properties {} for {}", config, configNode.getPath());

        if (configNode.hasNode(environment)) {
            final Map<String, String> envProps = getConfigurationProperties(configNode.getNode(environment), propertyNames);
            log.debug("Overriding default properties with environment properties {} from subnode {}", envProps, environment);
            config.putAll(envProps);
        }
        return config;
    }

    /**
     * Get properties from a node, except binary ones and the primary type.
     */
    public static Map<String, String> getConfigurationProperties(final Node configNode, final String... propertyNames) throws RepositoryException {
        final Map<String, String> props = new LinkedHashMap<>();

        final List<String> filterPropertyNames = (propertyNames == null) ? new ArrayList() : Arrays.asList(propertyNames);

        for (PropertyIterator propIter = configNode.getProperties(); propIter.hasNext(); ) {
            Property prop = propIter.nextProperty();
            // check binary, primary type
            if (!prop.isMultiple() && prop.getType() != PropertyType.BINARY && !prop.getName().equals(JcrConstants.JCR_PRIMARY_TYPE)) {
                // check argument filter, if any
                if (filterPropertyNames.isEmpty() || filterPropertyNames.contains(prop.getName())) {
                    props.put(prop.getName(), prop.getString());
                }
            }
        }
        return props;
    }
}
