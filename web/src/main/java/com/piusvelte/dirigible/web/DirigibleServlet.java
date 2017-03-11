package com.piusvelte.dirigible.web;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DirigibleServlet extends HttpServlet {

    private static final int BUFFER_SIZE = 128 * 1024;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI();
        String realPath = URLDecoder.decode(getServletContext().getRealPath(path), "UTF-8");
        File file = new File(realPath);

        if (file.exists()) {
            if (file.isDirectory()) {
                resp.setContentType("application/json");

                File[] files = file.listFiles();
                PrintWriter writer = resp.getWriter();
                writer.write("{\"data\":[");

                if (files != null) {
                    boolean addComma = false;

                    for (File child : files) {
                        String name = child.getName();

                        if (!name.startsWith(".")
                                && !"WEB-INF".equals(name)
                                && !"META-INF".equals(name)
                                && (name.endsWith(".mp4") || child.isDirectory())) {
                            if (addComma) {
                                writer.write(",");
                            }

                            writer.write("\"");
                            writer.write(URLEncoder.encode(name, "UTF-8"));
                            writer.write("\"");
                            addComma = true;
                        }
                    }
                }

                writer.write("]}");
            } else if (realPath.endsWith(".jpg")) {
                writeFile(resp, file, "image/jpeg");
            } else if (realPath.endsWith(".mp4")) {
                writeFile(resp, file, "video/mp4");
            } else {
                writeEmptyData(resp);
            }
        } else {
            writeEmptyData(resp);
        }
    }

    private void writeEmptyData(HttpServletResponse response) throws IOException {
        response.setContentType("application/json");
        response.getWriter().write("{\"data\":[]}");
    }

    private void writeFile(HttpServletResponse response, File file, String mimeType) {
        response.setContentType(mimeType);

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(file);
            out = response.getOutputStream();

            byte[] buffer = new byte[BUFFER_SIZE];

            for (int read; (read = in.read(buffer)) >= 0; ) {
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            closeQuietly(out);
            closeQuietly(in);
        }
    }

    private void closeQuietly(Closeable c) {
        if (c == null) return;

        try {
            c.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
