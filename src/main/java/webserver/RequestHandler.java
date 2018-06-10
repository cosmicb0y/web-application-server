package webserver;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.Map;

import db.DataBase;
import model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import util.HttpRequestUtils;
import util.IOUtils;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
    }

    public DataBase db;

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.

            if (db == null) {
                db = new DataBase();
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line = br.readLine();
            String action = line.split(" ")[0];
            String url = HttpRequestUtils.parseURL(line);
            int length = 0;
            while (!"".equals(line)) {
                if (line == null) { return;}
                if (line.contains("Content-Length")) {
                    length = Integer.parseInt(line.split(" ")[1]);
                }
                log.debug(line);
                line = br.readLine();
            }

            DataOutputStream dos = new DataOutputStream(out);
            File urlFile = new File("./webapp" + url);

            if (url.contains("/user/create")) {
                signUp(br, length, dos);
                return;
            }

            if (action.equals("POST") && url.contains("/user/login")) {
                String param = IOUtils.readData(br, length);
                Map<String, String> paramMap = HttpRequestUtils.parseQueryString(param);
                User signInUser = db.findUserById(paramMap.get("userId"));
                if (signInUser != null && signInUser.getPassword().equals(paramMap.get("password"))) {
                    responseSignInHeader(dos, "logined=true", "/index.html");
                    return;
                }
                responseSignInHeader(dos, "logined=false", "/user/login_failed.html");
                return;
            }

            byte[] body = Files.readAllBytes(urlFile.toPath());
            response200Header(dos, body.length);
            responseBody(dos, body);

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void signUp(BufferedReader br, int length, DataOutputStream dos) throws IOException {
        String param = IOUtils.readData(br, length);
        Map<String, String> paramMap = HttpRequestUtils.parseQueryString(param);
        db.addUser(new User(paramMap.get("userId"), paramMap.get("password"), paramMap.get("name"), paramMap.get("email")));
        response302Header(dos,  "/index.html");
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

    private void responseSignInHeader(DataOutputStream dos, String cookie, String redirectURL) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Set-Cookie: " + cookie + "\r\n");
            dos.writeBytes("Location: " + redirectURL + "\r\n");
            dos.writeBytes("\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String redirectURL) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found\r\n");
            dos.writeBytes("Location: "+ redirectURL + "\r\n");
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
