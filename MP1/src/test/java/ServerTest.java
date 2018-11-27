import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.net.Socket;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class ServerTest {
    @Mock
    private Socket socket;
    @Mock
    private ObjectOutputStream objectOutputStream;

    private Server server;

    private final Path path = Paths.get(System.getProperty("user.dir") + "/test.txt");
    private final String command = "grep -n apple test.txt";
    private final List<String> expected = Arrays.asList("1:apple pie", "2:apple juice");
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final String frequent_command = "grep a vmtest.log";
    private final String inFrequent_command = "grep abc vmtest.log";
    private final String someWhatFrequent_command = "grep z vmtest.log";

    @Before
    public void setUp() throws Exception {
        server = new Server();
        List<String> testLog = Arrays.asList("apple pie", "apple juice", "orange juice");
        Files.write(path, testLog, Charset.forName("UTF-8"));
        System.setOut(new PrintStream(outContent));
    }

    @Test
    public void testGrep() throws Exception {
        List<String> grepResult = server.grep(command);
        assertEquals(expected, grepResult);
    }

    @Test
    public void testGrepSequential() throws Exception {
        List<String> grepResult = server.grep(frequent_command);
        assertTrue(grepResult.size() == 302812);

        grepResult = server.grep(inFrequent_command);
        assertTrue(grepResult.size() == 75);

        grepResult = server.grep(someWhatFrequent_command);
        assertTrue(grepResult.size() == 9060);
    }

    @Test
    public void testGrepMultiThread() throws Exception {
        ExecutorService executorService1 = Executors.newFixedThreadPool(3);
        List<Future<List<String>>> countFutures = new ArrayList<>();
        countFutures.add(executorService1.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return server.grep(frequent_command);
            }
        }));
        countFutures.add(executorService1.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return server.grep(inFrequent_command);
            }
        }));
        countFutures.add(executorService1.submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                return server.grep(someWhatFrequent_command);
            }
        }));

        int total = 0;
        while (countFutures.size() > 0) {
            List<Future> done = new ArrayList<>();
            for (Future future : countFutures) {
                if (future.isDone()) {
                    try {
                        List<String> result = (List<String>) future.get();
                        total += result.size();
                    } catch (Exception e) {

                    } finally {
                        done.add(future);
                    }
                }
            }
            countFutures.removeAll(done);
        }

        assertEquals(total,311947);
    }


    @Test
    public void testCommunicate() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        DataOutputStream dataOutputStream = new DataOutputStream(baos);
        dataOutputStream.writeUTF(command);
        dataOutputStream.flush();
        dataOutputStream.close();

        InputStream in = new ByteArrayInputStream(baos.toByteArray());
        doReturn(in).when(socket).getInputStream();
        doReturn(objectOutputStream).when(socket).getOutputStream();

        server.communicate(socket, true);

        assertTrue(outContent.toString().contains("Total Time"));
    }

    @Test
    public void testCommunicateException() throws Exception {
        doThrow(Exception.class).when(socket).getInputStream();

        server.communicate(socket, true);

        assertEquals("Connection reset.\n", outContent.toString());
    }

    @After
    public void tearDown() throws Exception {
        Files.delete(path);
    }
}