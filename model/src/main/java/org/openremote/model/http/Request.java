/*
 * Copyright 2016, OpenRemote Inc.
 *
 * See the CONTRIBUTORS.txt file in the distribution for a
 * full listing of individual contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.openremote.model.http;

import jsinterop.annotations.JsMethod;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = "openremote.REST")
public class Request {

    @JsType(isNative = true, namespace= JsPackage.GLOBAL)
    public interface XMLHttpRequest {
        @JsMethod
        String getResponseHeader(String header);
    }

    /**
     * Sets the Accept request header. Defaults to star/star.
     */
    native public void setAccepts(String acceptHeader);

    /**
     * Sets the request credentials.
     */
    native public void setCredentials(String username, String password);

    /**
     * Sets the request entity.
     */
    native public void setEntity(String entity);

    /**
     * Sets the Content-Type request header.
     */
    native public void setContentType(String contentTypeHeader);

    /**
     * Sets the request URI. This should be an absolute URI.
     */
    native public void setURI(String uri);

    /**
     * Sets the request method. Defaults to GET.
     */
    native public void setMethod(String method);

    /**
     * Controls whether the request should be asynchronous. Defaults to true.
     */
    native public void setAsync(boolean async);

    /**
     * Sets the given cookie in the current document when executing the request. Beware that this will
     * be persistent in your browser.
     */
    native public void addCookie(String name, String value);

    /**
     * Adds a query parameter to the URI query part.
     */
    native public void addQueryParameter(String name, String value);

    /**
     * Adds a matrix parameter (path parameter) to the last path segment of the request URI.
     */
    native public void addMatrixParameter(String name, String value);

    /**
     * Adds a request header.
     */
    native public void addHeader(String name, String value);
}
