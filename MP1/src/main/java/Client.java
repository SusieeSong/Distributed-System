package com.company;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class Client {
    private static ExecutorService executorService;

    private static final int vMnumber = 10;
    private static final int clientPort = 9090;

    public Client() { }

    public Client(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public static void main(String[] args) {
        executorService = Executors.newFixedThreadPool(vMnumber);
        runClient();
        executorService.shutdown();
    }

    /**
     * Run Client
     */
    public static void runClient() {
        List<Socket> sockList = connect(vMnumber);
        grep(sockList, false);
    }

    /**
     * Connect other virtual machines
     * @param vMnumber virtual machine number
     * @return list of sockets
     */
    static List<Socket> connect(int vMnumber) {

        //list of futures of tasks submitted to the executor service that
        //create socket connections with other VMs
        List<Future<Socket>> socketFuture = new ArrayList<>();
        for (int i = 0; i < vMnumber; i++) {
            String num;
            if (i < 10) {
                num = "0" + i;
            } else {
                num = "" + i;
            }

            String hostName = String.format("Sp18-cs425-g55-%s.cs.illinois.edu", num);

            //Get callable socket task
            Callable<Socket> callableTask = createSocket(hostName);
            //Submit task to executor service
            Future<Socket> future = executorService.submit(callableTask);
            socketFuture.add(future);
        }

        List<Socket> socketList = new ArrayList<>();
        while (socketFuture.size() > 0) {
            List<Future> finishedWork = new ArrayList<>();
            for (Future future : socketFuture) {
                if (future.isDone()) {
                    try {
                        Socket socket = (Socket)future.get();
                        socketList.add(socket);
                    } catch (Exception e) {

                    } finally {
                        finishedWork.add(future);
                    }
                }
            }
            socketFuture.removeAll(finishedWork);
        }
        return socketList;
    }

    /**
     * Perform grep operation on all servers
     * @param socketList
     * @param unitTest
     */
    static void grep(List<Socket> socketList, boolean unitTest) {
        List<Future<Integer>> countFutures = new ArrayList<>();
        while (true) {
            //Get Input Command
            Scanner scanner = new Scanner(System.in);
            System.out.println("Please Enter a Command: ");
            String command = scanner.nextLine();
            long startTime = System.currentTimeMillis();

            //For each socket, create grep task and submit to executor service
            for (Socket socket: socketList) {
                Callable<Integer> callableTask = createGrepTask(socket, command);
                countFutures.add(executorService.submit(callableTask));
            }

            int total = 0;
            while (countFutures.size() > 0) {
                List<Future> finishedWork = new ArrayList<>();
                for (Future future : countFutures) {
                    if (future.isDone()) {
                        try {
                            int size = (int) future.get();
                            total += size;
                        } catch (Exception e) {

                        } finally {
                            finishedWork.add(future);
                        }
                    }
                }
                countFutures.removeAll(finishedWork);
            }
            long endTime = System.currentTimeMillis();
            System.out.println("Total Number of lines: " + total);
            if (unitTest) {
                break;
            } else {
                System.out.println("Total time is: " + (endTime - startTime) + " ms");
            }
        }
    }

    /**
     * create callable task that sends out grep commands to the server
     * and get the grep result from the server
     * @param socket connecting to the server
     * @param command of the grep operation
     * @return
     */
    static Callable<Integer> createGrepTask(Socket socket, String command) {
        Callable<Integer> callableTask = () -> {
            try {
                OutputStream outToServer = socket.getOutputStream();
                DataOutputStream out = new DataOutputStream(outToServer);
                InetAddress inetAddress = socket.getInetAddress();
                String hostName = inetAddress.getHostName();
                //Send output command to server
                out.writeUTF(command);

                //Get grep result back from server
                InputStream inFromServer = socket.getInputStream();
                ObjectInputStream in = new ObjectInputStream(inFromServer);
                List<String> result = (List<String>) in.readObject();
                System.out.println(String.format("Hostname : %s\nNumber of lines : %d", hostName, result.size()));

                //Write result tp txt file
                FileWriter writer = new FileWriter(hostName +".txt", true);
                writer.write(new SimpleDateFormat("yyyy/MM/dd HH:mm:ss").
                                format(Calendar.getInstance().getTime()) + "\n");
                for (String str : result) {
                    writer.write(str + "\n");
                }
                writer.close();
                return result.size();

            } catch (Exception e) {
                return null;
            }
        };
        return callableTask;
    }

    /**
     * Function for creating a callable task that creates a connection with the server
     * @param hostName of the server
     * @return the callable task
     */
    static Callable<Socket> createSocket(String hostName) {
        Callable<Socket> callableTask = () -> {
            try {
                Socket socket = new Socket(hostName, clientPort);
                return socket;
            } catch (Exception e) {
                return null;
            }
        };
        return callableTask;
    }

}
