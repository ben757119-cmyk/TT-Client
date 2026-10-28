package com.ttclient.util;

import java.util.Base64;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.zip.InflaterInputStream;

public final class StorkImageData {
    private StorkImageData() {}

    public static byte[] jpegBytes() {
        try {
            String b64 = StorkImageDataP0.PART + StorkImageDataP1.PART + StorkImageDataP2.PART
                    + StorkImageDataP3.PART + StorkImageDataP4.PART + StorkImageDataP5.PART;
            byte[] z = Base64.getDecoder().decode(b64);
            InflaterInputStream in = new InflaterInputStream(new ByteArrayInputStream(z));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
            in.close();
            return out.toByteArray();
        } catch (Throwable t) {
            return new byte[0];
        }
    }
}
