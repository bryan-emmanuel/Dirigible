package com.piusvelte.dirigible.web;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLEncoder;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class DirigibleServlet extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        super.doGet(req, resp);

        String uri = req.getRequestURI();
        URL url = new URL(uri);
        String path = url.getPath();
        String realPath = getServletContext().getRealPath(path);
        File file = new File(realPath);

        if (!file.exists() || file.isDirectory()) {
            resp.setContentType("application/json");

            File[] files = file.listFiles();
            PrintWriter writer = resp.getWriter();
            writer.write("{\"data\":[");

            if (files != null) {
                boolean addComma = false;

                for (File child : files) {
                    String name = child.getName();
                    if (name.startsWith(".")) continue;

                    if (name.endsWith(".mp4") || child.isDirectory()) {
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
            resp.setContentType("image/jpeg");
            writeFile(resp, file);
        } else if (realPath.endsWith(".mp4")) {
            resp.setContentType("video/mp4");
            writeFile(resp, file);
        } else {
            resp.setContentType("application/json");
            resp.getWriter().write("{\"data\":[]}");
        }
    }

    private void writeFile(HttpServletResponse response, File file) {
        FileInputStream in = null;
        OutputStream out = null;

        try {
            in = new FileInputStream(file);
            out = response.getOutputStream();

            // Copy the contents of the file to the output stream
            byte[] buf = new byte[1024];
            int count = 0;
            while ((count = in.read(buf)) >= 0) {
                out.write(buf, 0, count);
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
