package com.piusvelte.dirigible.web;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DirigibleServlet extends HttpServlet {

    private static final int MAX_BUFFER_SIZE = 64 * 1024;

    private static final String EXT_MP4 = ".mp4";
    private static final String EXT_JPG = ".jpg";
    private static final String EXT_MPD = ".mpd";
    private static final String EXT_M4S = ".m4s";

    private static final String JSON_FORMAT = "{\"data\":[%s]}";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.addHeader("Access-Control-Allow-Origin", "*");

        String path = req.getRequestURI();
        String realPath = URLDecoder.decode(getServletContext().getRealPath(path), "UTF-8");
        File file = new File(realPath);

        if (file.exists()) {
            if (file.isDirectory()) {
                resp.setContentType("application/json");

                File[] files = file.listFiles();
                StringBuffer buffer = new StringBuffer();
                boolean firstTime = true;

                for (File child : files) {
                    String name = child.getName();

                    if (!name.startsWith(".")
                            && !"WEB-INF".equals(name)
                            && !"META-INF".equals(name)
                            && (name.endsWith(EXT_MP4) || child.isDirectory())) {
                        if (firstTime) {
                            firstTime = false;
                        } else {
                            buffer.append(",");
                        }

                        if (child.isDirectory()) {
                            // check if this is an mpeg-dash directory

                            File[] streamFiles = child.listFiles();

                            for (File streamFile : streamFiles) {
                                if (streamFile.getName().endsWith(EXT_MPD)) {
                                    // the client needs to understand that this file is actually a directory for a stream
                                    name += EXT_MPD;
                                    break;
                                }
                            }
                        }

                        buffer.append("\"")
                                .append(URLEncoder.encode(name, "UTF-8"))
                                .append("\"");
                    }
                }

                writeJson(resp, buffer.toString());
            } else if (realPath.endsWith(EXT_JPG)) {
                writeFile(resp, file, "image/jpeg");
            } else if (realPath.endsWith(EXT_MP4) || realPath.endsWith(EXT_M4S)) {
                writeFile(resp, file, "video/mp4");
            } else if (realPath.endsWith(EXT_MPD)) {
                writeFile(resp, file, "application/dash+xml");
            } else {
                writeJson(resp, "");
            }
        } else {
            writeJson(resp, "");
        }
    }

    private void writeFile(HttpServletResponse response, File file, String mimeType) {
        response.setContentType(mimeType);

        setBufferSize(response, file.length());

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(file);
            out = response.getOutputStream();

            byte[] buffer = new byte[response.getBufferSize()];

            while (in.read(buffer) >= 0) {
                out.write(buffer);
            }
        } catch (IOException e) {
            getServletContext().log("error reading file " + file.getName(), e);
        } finally {
            closeQuietly(out);
            closeQuietly(in);
        }
    }

    private void setBufferSize(HttpServletResponse response, long size) {
        long blockSizeRoundedFileSize = (long) (512 * Math.ceil(size / 512.f));
        int bufferSize = (int) Math.max(Math.min(blockSizeRoundedFileSize, MAX_BUFFER_SIZE), response.getBufferSize());
        response.setBufferSize(bufferSize);
        response.setHeader("Content-Length", Long.toString(size));
    }

    private void writeJson(HttpServletResponse response, String body) throws UnsupportedEncodingException {
        response.setContentType("application/json");

        byte[] data = String.format(JSON_FORMAT, body).getBytes("UTF-8");
        setBufferSize(response, data.length);

        OutputStream out = null;

        try {
            out = response.getOutputStream();
            out.write(data);
        } catch (IOException e) {
            getServletContext().log("error writing data", e);
        } finally {
            closeQuietly(out);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c == null) return;

        try {
            c.close();
        } catch (IOException e) {
            getServletContext().log("error closing", e);
        }
    }
}
