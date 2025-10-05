package llc.berserkr.serve;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;

public class Serving {

    public static void main(String[] args) throws IOException {
        new Serving(1500).start();
    }

    private static final Logger logger = LoggerFactory.getLogger(Serving.class);
    private final int port;

    public Serving(int port) {
        this.port = port;
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

                        for (final String data : ServingData.DATA) {
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
