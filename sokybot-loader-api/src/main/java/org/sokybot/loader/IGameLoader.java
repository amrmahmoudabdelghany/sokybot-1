package org.sokybot.loader;

/**
 * Interface for game loader implementations that can launch game clients.
 */
public interface IGameLoader {

    /**
     * Launch a game client with default command.
     * 
     * @param clientPath actual path to a client
     * @return Process handle that can be used to manipulate the opened process
     */
    public int launch(String clientPath);

    /**
     * Launch a game client with a custom command.
     * 
     * @param clientPath actual path to a client
     * @param command command that passed to the client
     * @return Process handle that can be used to manipulate the opened process
     */
    public int launch(String clientPath, String command);

    /**
     * Return unique name for this implementation that used to identify it.
     * 
     * @return unique name
     */
    public String getName();

    /**
     * Get the minimum supported game version.
     * -1 indicates unlimited (no minimum).
     * 
     * @return minimum supported game version
     */
    public Integer minimumVersion();

    /**
     * Get the maximum supported game version.
     * -1 indicates unlimited (no maximum).
     * 
     * @return maximum supported game version
     */
    public Integer maximumVersion();
}
