import com.google.common.util.concurrent.Futures;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.*;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

@RunWith(MockitoJUnitRunner.class)
public class ClientTest {
    @Mock
    private ExecutorService executorService;
    @Mock
    private Socket socket;
    @Mock
    private InetAddress inetAddress;

    private Client client;
    private List<Socket> sockets;

    private final List<String> output = new ArrayList<>(Arrays.asList("123", "456", "789"));
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final String frequent_command = "grep a vmtest.log";
    private final String inFrequent_command = "grep abc vmtest.log";
    private final String someWhatFrequent_command = "grep z vmtest.log";

    @Before
    public void setUp() throws Exception {
        client = new Client(executorService);
        sockets = new ArrayList<>(Arrays.asList(socket));
        System.setOut(new PrintStream(outContent));
    }

    @Test
    public void testConnection() throws Exception {
        doReturn(Futures.immediateFuture(socket)).when(executorService).submit(any(Callable.class));
        List<Socket> actual = client.connect(1);
        assertTrue(actual.size() == 1);
        assertEquals(sockets, actual);
    }

    @Test
    public void testConnectionException() {
        doReturn(Futures.immediateFailedFuture(new Exception())).when(executorService).submit(any(Callable.class));
        List<Socket> actual = client.connect(1);
        assertTrue(actual.size() == 0);
    }

    @Test
    public void testGrep() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream("test_command".getBytes());
        System.setIn(in);
        doReturn(Futures.immediateFuture(output.size())).when(executorService).submit(any(Callable.class));

        client.grep(sockets, true);
        assertEquals("Enter a command: \nTotal : 3\n", outContent.toString());
    }

    @Test
    public void testCreateGrepTask() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(frequent_command.getBytes());
        System.setIn(in);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        doReturn(out).when(socket).getOutputStream();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);

        oos.writeObject(output);

        oos.flush();
        oos.close();

        InputStream input = new ByteArrayInputStream(baos.toByteArray());
        doReturn(input).when(socket).getInputStream();

        doReturn(inetAddress).when(socket).getInetAddress();
        doReturn("Hostname").when(inetAddress).getHostName();

        Client client1 = new Client(Executors.newFixedThreadPool(1));
        client1.grep(sockets, true);

        assertEquals(String.format("Enter a command: \nHostname : Hostname\nNumber of lines : 3\nTotal : 3\n"), outContent.toString());
    }

    @Test
    public void testCreateGrepTaskException() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(frequent_command.getBytes());
        System.setIn(in);
        doThrow(Exception.class).when(socket).getOutputStream();

        Client client1 = new Client(Executors.newFixedThreadPool(1));
        client1.grep(sockets, true);

        assertEquals(String.format("Enter a command: \nTotal : 0\n"), outContent.toString());
    }
}