package mazealgo.model.server;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * What the server does with a connected client's streams. One strategy
 * per "endpoint" (generate a maze, solve a maze, ...). The strategy
 * does not own the socket and must not close the streams it's handed —
 * {@link MyServer} closes the socket for it.
 */
@FunctionalInterface
public interface IServerStrategy {
    void serverStrategy(InputStream inFromClient, OutputStream outToClient) throws Exception;
}
