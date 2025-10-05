package llc.berserkr.serve;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Serving {

    private final List<String> data;

    private static final Logger logger = LoggerFactory.getLogger(Serving.class);
    private final int port;

    public Serving(int port, final List<String> data) {
        this.port = port;
        this.data = data;
    }

    public void start() {

        final Thread thread = new Thread(() -> {

            try(final ServerSocket serverSocket =
                new ServerSocket(port)) {

                logger.info("Server listening on port " + port);

                try(final Socket clientSocket = serverSocket.accept()) {

                    logger.info("Client connected from " + clientSocket.getInetAddress());

                    try {

                        final PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);

                        for (final String data : data) {
                            out.println(data);
                        }

                        out.flush();

                    }
                    catch (IOException e) {
                        logger.error(e.getMessage(), e);
                    }

                }

            }
            catch (IOException e) {
                logger.error("Could not listen on port " + port + ": " + e.getMessage());
            }
            finally {
                logger.info("Serving stopped");
            }

        });

        thread.start();

    }

}
