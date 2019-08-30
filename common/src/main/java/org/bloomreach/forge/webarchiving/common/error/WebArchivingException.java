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

package org.bloomreach.forge.webarchiving.common.error;

import org.slf4j.helpers.MessageFormatter;

/**
 * The <CODE>WebArchivingException</CODE> class defines a general exception
 * that the web archiving module can throw when it is unable to perform its operation
 * successfully.
 */
public class WebArchivingException extends Exception {
    
    public WebArchivingException(final String message) {
        super(message);
    }

    public WebArchivingException(final Throwable cause) {
        super(cause);
    }

    public WebArchivingException(final Throwable cause, final String message) {
        super(message, cause);
    }

    public WebArchivingException(final String messagePattern, final Object... args) {
        super(parseArrayMessage(messagePattern, args));
    }

    public WebArchivingException(final Throwable cause, final String messagePattern, final Object... args) {
        super(parseArrayMessage(messagePattern, args), cause);
    }

    protected static String parseArrayMessage(final String messagePattern, final Object[] args) {
        return MessageFormatter.arrayFormat(messagePattern, args).getMessage();
    }
}
