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

    private static final int BUFFER_SIZE = 256 * 1024;

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
                String param = req.getParameter("test");

                if (param == null || param.length() == 0) {
                    writeFile(resp, file, "image/jpeg");
                } else {
                    test(resp, file, param);
                }
            } else if (realPath.endsWith(".mp4")) {
                String param = req.getParameter("test");

                if (param == null || param.length() == 0) {
                    writeFile(resp, file, "video/mp4");
                } else {
                    test(resp, file, param);
                }
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

    private void test(HttpServletResponse response, File file, String test) throws IOException {
        response.setContentType("text/html");
        response.getWriter().write("<html><body><p>");
        response.getWriter().write("default buffer size: ");
        response.getWriter().write(String.valueOf(response.getBufferSize()));
        response.getWriter().write("</p>");

        if ("read".equals(test)) {
            long start = System.currentTimeMillis();
            long totalRead = 0;
            InputStream in = null;

            try {
                in = new FileInputStream(file);

                byte[] buffer = new byte[BUFFER_SIZE];

                for (int read; (read = in.read(buffer)) >= 0; ) {
                    totalRead += read;
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                closeQuietly(in);
            }

            if (totalRead > 0) {
                response.getWriter().write("<p>read: ");
                response.getWriter().write(String.valueOf(totalRead / ((System.currentTimeMillis() - start) * 1000.f)));
                response.getWriter().write("m/s");
            } else {
                response.getWriter().write("no bytes read");
            }
        } else {
            response.getWriter().write("unknown test: ");
            response.getWriter().write(test);
        }
    }

    private void writeFile(HttpServletResponse response, File file, String mimeType) {
        response.setContentType(mimeType);
        response.setBufferSize(BUFFER_SIZE);

        InputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(file);
            out = response.getOutputStream();

            byte[] buffer = new byte[BUFFER_SIZE];
            int read;

            while ((read = in.read(buffer)) >= 0) {
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
