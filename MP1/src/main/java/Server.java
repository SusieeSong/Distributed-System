package com.company;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.Buffer;
import java.util.ArrayList;
import java.util.List;

public class Server {
    private ServerSocket serverSocket;

    private static final int serverPort = 9090;
    private static final int bufferSize = 20480000;

    public static void main(String[] args) throws Exception {
        Server server = new Server();
        server.start();
    }

    /**
     * start the server
     * @throws Exception
     */
    private void start() throws Exception{
        serverSocket = new ServerSocket(serverPort);
        while (true) {
            communicate(serverSocket.accept(), false);
        }
    }

    /**
     * Communicate with the client
     * @param socket
     * @param unitTest
     * @throws Exception
     */
    private void communicate(Socket socket, boolean unitTest) throws Exception{
        try {
            while (true) {
                DataInputStream in = new DataInputStream(socket.getInputStream());
                String command = in.readUTF();
                System.out.println(command);
                long startTime = System.currentTimeMillis();
                List<String> result = grep(command);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                send(out, result);

                long endTime = System.currentTimeMillis();
                System.out.println("Total time: " + (endTime - startTime) + " ms");
                if (unitTest) {
                    break;
                }
            }
        } catch (Exception e) {
            System.out.println("Connect reset");
        } finally {
            socket.close();
        }
    }

    /**
     * Perform the grep operation
     * @param command
     * @return the grep result
     * @throws Exception
     */
    List<String> grep (String command) throws Exception {
        //Execute grep command
        Process process = Runtime.getRuntime().exec(command);
        BufferedInputStream input = new BufferedInputStream(process.getInputStream(), bufferSize);
        BufferedReader reader = new BufferedReader(new InputStreamReader(input), bufferSize);
        List<String> output = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            output.add(line);
        }
        process.waitFor();
        return output;
    }

    /**
     * Send the grep results to output stream
     * @param out
     * @param result
     * @throws Exception
     */
    private void send(ObjectOutputStream out, List<String> result) throws Exception {
        out.writeObject(result);
        out.flush();
    }
}
