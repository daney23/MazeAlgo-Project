package io.github.daney23.mazealgo.model.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A small accept-loop server that dispatches each accepted client to an
 * {@link ExecutorService} thread pool. The pool size caps concurrent
 * client handling; additional clients queue until a worker is free.
 *
 * <p>Lifecycle:
 * <pre>
 *   MyServer server = new MyServer(5400, 1000, new SolveMazeStrategy());
 *   server.start();
 *   // ... clients connect, are handled in parallel ...
 *   server.stop();
 * </pre>
 *
 * <p>{@link #stop()} signals the accept loop via a flag and shuts the
 * pool down; {@link ServerSocket#setSoTimeout(int)} keeps the loop
 * responsive (the listening interval) so the flag is checked even when
 * no clients are connecting.
 */
public class MyServer {
    private static final Logger log = LogManager.getLogger(MyServer.class);
    private static final int DEFAULT_POOL_SIZE = 8;

    private final int port;
    private final int listeningIntervalMS;
    private final IServerStrategy strategy;
    private final ExecutorService executor;
    private final AtomicBoolean stop = new AtomicBoolean(false);

    private Thread acceptThread;
    private ServerSocket serverSocket;

    public MyServer(int port, int listeningIntervalMS, IServerStrategy strategy) {
        this(port, listeningIntervalMS, strategy, Executors.newFixedThreadPool(DEFAULT_POOL_SIZE));
    }

    public MyServer(int port, int listeningIntervalMS, IServerStrategy strategy,
                    ExecutorService executor) {
        this.port = port;
        this.listeningIntervalMS = listeningIntervalMS;
        this.strategy = strategy;
        this.executor = executor;
    }

    /**
     * Binds the listening socket synchronously and starts the accept
     * loop on a background daemon thread. Throws if the port is taken;
     * after this returns, {@link #getPort()} reports the bound port.
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(listeningIntervalMS);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to bind server socket on port " + port, e);
        }
        acceptThread = new Thread(this::runAcceptLoop,
                "MyServer-accept-" + serverSocket.getLocalPort());
        acceptThread.setDaemon(true);
        acceptThread.start();
    }

    /** Returns the port the server is bound to (useful when port 0 was passed). */
    public int getPort() {
        return serverSocket == null ? port : serverSocket.getLocalPort();
    }

    /**
     * Asks the accept loop to exit and waits up to a few seconds for
     * in-flight client work to finish. Safe to call from any thread.
     */
    public void stop() {
        stop.set(true);
        try {
            if (acceptThread != null) {
                acceptThread.join(2 * listeningIntervalMS + 1000L);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void runAcceptLoop() {
        log.info("Server listening on port {}", serverSocket.getLocalPort());
        try {
            while (!stop.get()) {
                try {
                    Socket client = serverSocket.accept();
                    executor.execute(() -> handleClient(client));
                } catch (SocketTimeoutException expected) {
                    // wakeup so we can re-check the stop flag
                }
            }
        } catch (IOException e) {
            if (!stop.get()) {
                log.error("Server accept loop failed", e);
            }
        } finally {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
                // best-effort
            }
            log.info("Server stopped");
        }
    }

    private void handleClient(Socket client) {
        try (Socket c = client;
             InputStream in = c.getInputStream();
             OutputStream out = c.getOutputStream()) {
            strategy.serverStrategy(in, out);
        } catch (Exception e) {
            log.warn("Error handling client {}: {}", client.getRemoteSocketAddress(), e.toString());
        }
    }
}
