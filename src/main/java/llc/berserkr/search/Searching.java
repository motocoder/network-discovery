package llc.berserkr.search;

import llc.berserkr.serve.ServingData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;

/**
 * This class searches a list of hosts for the Serving service. If found it calls a callback
 * with the port read from the hosts Serving service.
 */
public class Searching {

    private static final Logger logger = LoggerFactory.getLogger(Searching.class);

    private final ExecutorService executor;
    private final Set<Future<?>> connectingThreads = new HashSet<>();

    private final List<String> hosts;
    private final List<Integer> ports;

    private final Object foundLock = new Object();
    private final int timeout;
    private boolean found;
    private Thread runningTread;

    /**
     *
     * @param hosts
     * @param ports
     * @param timeout
     * @param maxThreads
     */
    public Searching(
        final List<String> hosts,
        final List<Integer> ports,
        final int timeout,
        final int maxThreads
    ) {

        this.timeout = timeout;
        this.hosts = hosts;
        this.ports = ports;
        executor = Executors.newFixedThreadPool(maxThreads);

    }

    /**
     * default to 500ms timeout, 100 max threads
     *
     * @param hosts
     * @param ports
     */
    public Searching(
        final List<String> hosts,
        final List<Integer> ports
    ) {

        this.timeout = 500;
        this.hosts = hosts;
        this.ports = ports;
        executor = Executors.newFixedThreadPool(100);

    }

    private int runCount = 0;

    public synchronized void start(Consumer<FoundConnection> parentCallback) {

        //if it's already running stop it
        stop();

        //used to tell the thread if we're currently still supposed to run
        final int myRunCount = ++runCount;

        runningTread = new Thread(() -> {

            final Consumer<FoundConnection> callback = (data) -> {

                //ensure the callback only fires once.
                synchronized (foundLock) {
                    if(found) {
                        return;
                    }

                    found = true;
                }
                parentCallback.accept(data);

            };

            for (final String host : this.hosts) {
                for (final int port : this.ports) {

                    if (runCount != myRunCount) {
                        callback.accept(null); //returns null if interrupted
                        return;
                    }
                    connect(host, port, callback);
                }
            }

            logger.info("Finished connecting, waiting for running threads to finish.");

            while(runCount == myRunCount) {
                synchronized (connectingThreads) {

                    final Set<Future<?>> toRemove = new HashSet<>();

                    for(Future<?> future : connectingThreads) {
                        if(future.isDone()) {
                            toRemove.add(future);
                        }
                    }

                    connectingThreads.removeAll(toRemove);

                    if(connectingThreads.isEmpty()) {
                        break;
                    }

                    try {
                        Thread.sleep(10);//arbitrary delay so it doesn't spin too fast.
                    } catch (InterruptedException e) {
                        logger.warn(e.getMessage());
                        break;
                    }
                }
            }

          logger.info("Finished connecting, nothing found returning null");

          callback.accept(null); //returns null if nothing found


        });
        runningTread.start();

    }

    public synchronized void stop() {

        if(runningTread != null) {

            logger.info("Stopping searching...");

            runningTread.interrupt(); //interrupt the starting thread if it's going

            try {
                runningTread.join(); //wait for previous to terminate
            } catch (InterruptedException e) {
                logger.error(e.getMessage(), e);
                throw new RuntimeException(e);
            }
            runningTread = null;

            logger.info("searching Stopped...");
        }

    }

    private void connect(
        final String host,
        final int port,
        final Consumer<FoundConnection> callback
    ) {

        final Future<?> connectionThread = executor.submit(new Runnable() {
            @Override
            public void run() {
                logger.info("attempting to connect to " + host + ":" + port);

                try {

                    try(final Socket socket = new Socket()) {

                        socket.connect(new InetSocketAddress(host, port), timeout);

                        final BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

                        final String firstLine = in.readLine();

                        logger.info("read firstLine: " + firstLine);

                        if (firstLine.equals(ServingData.FOUND_TERM)) {

                            String secondLine = in.readLine();
                            List<String> data = new ArrayList<>();

                            while(secondLine != null) {

                                data.add(secondLine);
                                secondLine = in.readLine();

                            }

                            logger.info("found at " + host + ":" + port);
                            callback.accept(new FoundConnection(host, port, data));
                            return;
                        }
                    }

                } catch (IOException e) {
                    logger.warn(e.getMessage(), e);
                }

                logger.info("failed to connect to " + host + ":" + port);
            }
        });

        synchronized (connectingThreads) {
            connectingThreads.add(connectionThread);
        }

    }

    public boolean isSearching() {

        final Thread myRunningThread = runningTread;

        if(myRunningThread != null) {
            return myRunningThread.isAlive();
        }
        synchronized (connectingThreads) {
            return !connectingThreads.isEmpty();
        }

    }

    public record FoundConnection(String host, int port, List<String> data) {
    }

}
