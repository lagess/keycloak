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
package org.keycloak.services.filters;

import java.io.IOException;
import java.io.ByteArrayInputStream;

import javax.servlet.ServletInputStream;

/*
 * Original Authors: (Special Thanks to...)
 * http://blog.honestyworks.jp/blog/archives/162   (real original author?)
 * http://d.hatena.ne.jp/machi_pon/20090120/1232420325
 * http://ameblo.jp/vashpia77/entry-10826082231.html
 */
public class BufferedServletInputStream extends ServletInputStream {

    private ByteArrayInputStream is;

    public BufferedServletInputStream(byte[] buffer) {
        this.is = new ByteArrayInputStream(buffer);
    }

    @Override
    public int available() throws IOException {
        return is.available();
    }

    @Override
    public int read() throws IOException {
        return is.read();
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return is.read(b, off, len);
    }

    @Override
    public void reset() throws IOException {
        is.reset();
    }
}

