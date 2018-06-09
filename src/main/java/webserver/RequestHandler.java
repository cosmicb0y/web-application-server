package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public User user;

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            String url = HttpRequestUtils.parseURL(line);
            while (!"".equals(line)) {
                if (line == null) { return;}
                line = br.readLine();
            }

            httpResponse(out, url);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void httpResponse(OutputStream out, String url) throws IOException {
        DataOutputStream dos = new DataOutputStream(out);
        File urlFile = new File("./webapp" + url);

        if (url.contains("/user/create")) {
            int index = url.indexOf("?");
            String param = url.substring(index+1);
            Map<String, String> paramMap = HttpRequestUtils.parseQueryString(param);
            user = new User(paramMap.get("userId"), paramMap.get("password"), paramMap.get("name"), paramMap.get("email"));
            urlFile = new File("./webapp" + "/index.html");
        }

        byte[] body = Files.readAllBytes(urlFile.toPath());
        response200Header(dos, body.length);
        responseBody(dos, body);
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

}
