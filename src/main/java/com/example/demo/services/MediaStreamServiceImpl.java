package com.example.demo.services;


import com.example.demo.utils.JavaCvFFmpegUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Service
public class MediaStreamServiceImpl {

    public static final int CHUNK_SIZE = 1024 * 1024 * 3;

    public static final int BYTE_RANGE = 1024;

    public static final String BYTES = "bytes";


    public StreamingResponseBody hlsPlay(String filename) {
        File file = new File(filename);
        if (!file.exists()) {
            return null;
        }
        return new StreamingResponseBody() {
            @Override
            public void writeTo(OutputStream outputStream) throws IOException {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    if (filename.contains(".m3u8")) {
                        byte[] data = new byte[BYTE_RANGE];
                        int nRead;
                        while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
                            outputStream.write(data, 0, nRead);
                        }
                        outputStream.flush();
                    } else if (filename.contains(".ts")) {
                        JavaCvFFmpegUtils.addWaterMark(inputStream, outputStream, String.valueOf(System.currentTimeMillis()));
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        };
    }

}
