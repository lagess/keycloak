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


        import javax.servlet.ServletInputStream;
        import javax.servlet.http.HttpServletRequest;
        import javax.servlet.http.HttpServletRequestWrapper;
        import java.io.ByteArrayOutputStream;
        import java.io.InputStream;
        import java.io.IOException;

/*
 * Original Authors: (Special Thanks to...)
 * http://blog.honestyworks.jp/blog/archives/162   (real original author?)
 * http://d.hatena.ne.jp/machi_pon/20090120/1232420325
 * http://ameblo.jp/vashpia77/entry-10826082231.html
 */
public class BufferedRequestWrapper extends HttpServletRequestWrapper {

    // private byte[] buffer;
    private BufferedServletInputStream is;
    private boolean retry = false;

    public BufferedRequestWrapper(HttpServletRequest request) throws IOException {
        super(request);

        InputStream is = request.getInputStream();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte buff[] = new byte[1024];
        int read;
        while((read = is.read(buff)) > 0) {
            baos.write(buff, 0, read);
        }
        byte[] buffer = baos.toByteArray();
        this.is = new BufferedServletInputStream(buffer);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if(retry){
            retry = false;
            is.reset();
        }

        return is;
    }

    public void retryRequest(){
        retry = true;
    }

}