package org.keycloak.services.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletOutputStream;

/**
 * ServletOutputStream implementation that buffers all data written to
 * the outputstream.  This allows the data to be captured without
 * being sent to the client.
 */
public class BufferedServletOutputStream extends ServletOutputStream {

    // buffer to memory for now
    private ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    @Override
    public void write(int b) throws IOException {
        buffer.write(b);
    }

    /**
     * @return inputstream to buffered data
     */
    public InputStream getBufferInputStream() {
        return new ByteArrayInputStream(buffer.toByteArray());
    }

    /**
     * @return true if buffered data is empty, otherwise false
     */
    public boolean isEmpty() {
        return buffer.size() == 0;
    }

}
