/*
 * Copyright 2007 Open Source Applications Foundation
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
package org.keycloak.services.util;


import org.apache.commons.io.IOUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.Part;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;

/*
 * Original Authors: (Special Thanks to...)
 * http://blog.honestyworks.jp/blog/archives/162   (real original author?)
 * http://d.hatena.ne.jp/machi_pon/20090120/1232420325
 * http://ameblo.jp/vashpia77/entry-10826082231.html
 */
public class BufferedRequestWrapper extends HttpServletRequestWrapper {

    private byte[] buffer = null;
    private BufferedServletInputStream is;

    public BufferedRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);
    }

    private HttpServletRequest _getHttpServletRequest() {
        return (HttpServletRequest) super.getRequest();
    }

    @Override
    public ServletInputStream getInputStream() throws IOException{
        if(buffer == null){
            loadInputStream();
        }

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(buffer);
        ServletInputStream servletInputStream = new ServletInputStream() {
            @Override
            public int read() throws IOException {
                return byteArrayInputStream.read();
            }
        };

        return servletInputStream;
    }

    /**
     * The default behavior of this method is to return
     * getParameter(String name) on the wrapped request object.
     */
    @Override
    public String getParameter(String name) {
        String parameter =  this._getHttpServletRequest().getParameter(name);

        if(buffer == null){
            loadInputStream();
        }


        return parameter;
    }


    /**
     * The default behavior of this method is to return getParameterMap()
     * on the wrapped request object.
     */
    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> parameterMap = this._getHttpServletRequest().getParameterMap();

        if(buffer == null){
            loadInputStream();
        }

        return parameterMap;
    }


    /**
     * The default behavior of this method is to return getParameterNames()
     * on the wrapped request object.
     */
    @Override
    public Enumeration<String> getParameterNames() {
        Enumeration<String> parameterNames = this._getHttpServletRequest().getParameterNames();

        if(buffer == null){
            loadInputStream();
        }

        return parameterNames;
    }


    /**
     * The default behavior of this method is to return
     * getParameterValues(String name) on the wrapped request object.
     */
    @Override
    public String[] getParameterValues(String name) {
        String[] parameterValues = this._getHttpServletRequest().getParameterValues(name);

        if(buffer == null){
            loadInputStream();
        }

        return parameterValues;
    }





    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        Collection<Part> parts = this._getHttpServletRequest().getParts();

        if(buffer == null){
            loadInputStream();
        }

        return parts;
    }

    @Override
    public Part getPart(final String name) throws IOException, ServletException {
        Part part = this._getHttpServletRequest().getPart(name);

        if(buffer == null){
            loadInputStream();
        }

        return part;
    }


    private void loadInputStream() {
        try(InputStream stream = this._getHttpServletRequest().getInputStream()) {
            buffer = IOUtils.toByteArray(stream);
        } catch (IOException e) {
            //TODO
            e.printStackTrace();
        }
    }



}