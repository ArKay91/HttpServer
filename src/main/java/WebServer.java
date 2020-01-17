import com.sun.deploy.net.HttpResponse;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class WebServer {

    private static final String TASK_ENDPOINT = "/task";
    private static final String STATUS_ENDPOINT = "/status";
    private int port;
    private HttpServer httpServer;

    public WebServer(int port){
        this.port = port;
    }

    public static void main(String[] args){
        int serverPort = 8080;
        if(args.length == 1){
            serverPort = Integer.parseInt(args[0]);
        }

        WebServer webServer = new WebServer(serverPort);
        webServer.startServer();

        System.out.println("Server is listening on port" + serverPort);
    }
    public void startServer(){
        try {
            this.httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        HttpContext taskContext = httpServer.createContext(TASK_ENDPOINT);
        HttpContext statusContext = httpServer.createContext(STATUS_ENDPOINT);

        statusContext.setHandler(this::handleStatusCheckRequest);
        taskContext.setHandler(this::handlerTaskRequest);

        httpServer.setExecutor(Executors.newFixedThreadPool(8));

        httpServer.start();
    }

    private void handlerTaskRequest(HttpExchange httpExchange) throws IOException {
        if(!httpExchange.getRequestMethod().equalsIgnoreCase("post ")){
            httpExchange.close();
            return;
        }
        Headers headers = httpExchange.getRequestHeaders();
        if(headers.containsKey("X-Test") && headers.get("X-Test").get(0).equalsIgnoreCase("true")){
            String dummyResponse = "123 /n";
            sendResponse(dummyResponse.getBytes(), httpExchange);
            return;
        }
        boolean isDebugMode = false;
        if(headers.containsKey("X-Debug") && headers.get("X-Debug").get(0).equalsIgnoreCase("true")){
            isDebugMode = true;
        }

        long startTime = System.nanoTime();

        String requestMessage = httpExchange.getRequestBody().toString();
        byte [] responseBytes = calculateResponse(requestMessage);

        long finishTime = System.nanoTime();
        if(isDebugMode) {
            String debugMessage = String.format("Operation took %d ns \n", finishTime-startTime);
            httpExchange.getResponseHeaders().put("X-Debug-Info", Arrays.asList(debugMessage));
        }

        sendResponse(responseBytes, httpExchange);

    }

    private byte [] calculateResponse(String requestBytes) {
        String bodyString = new String(requestBytes);
        String [] stringNumbers = bodyString.split(",");

        BigInteger result = BigInteger.ONE;
        for(String number : stringNumbers) {
            BigInteger bigInteger = new BigInteger(number);
            result = result.multiply(bigInteger);
        }

        return String.format("Result of the multiplicaiton is %s \n", result).getBytes();
    }



    private void handleStatusCheckRequest(HttpExchange httpExchange) throws IOException {
        if(!httpExchange.getRequestMethod().equalsIgnoreCase("get")){
            httpExchange.close();
            return;
        }
        String responseMessage = "Server is live";
        sendResponse(responseMessage.getBytes(), httpExchange);
    }

    private void sendResponse(byte [] responseBytes, HttpExchange httpExchange) throws IOException {
        httpExchange.sendResponseHeaders(200, responseBytes.length);
        OutputStream outputStream = httpExchange.getResponseBody();
        outputStream.write(responseBytes);
        outputStream.flush();
        outputStream.close();

    }
}
