package de.blanksteg.storyteller.common;

import java.util.Collection;

/**
 * Defines methods common to classes representing an IRC server inside the
 * Storyteller project. Servers are defined by their host name and store a
 * collection of channels that are tracked on the server. Additionally, data
 * about the identity the server is joined with is also made available by the
 * server instead of an extra class.
 * 
 * @author Marc Müller
 */
public interface StorytellerServer {

	/**
	 * Returns this server's host name.
	 * 
	 * @return
	 */
	public String getHost();

	/**
	 * Returns this port to connect on.
	 * 
	 * @return
	 */
	public int getPort();

	/**
	 * Returns the nickname to join with.
	 * 
	 * @return
	 */
	public String getNickName();

	/**
	 * Returns the user name to join with.
	 * 
	 * @return
	 */
	public String getUserName();

	/**
	 * Returns the real name to join with.
	 * 
	 * @return
	 */
	public String getRealName();

	/**
	 * Returns the {@link Authentication} method to connect with.
	 * 
	 * @return
	 */
	public Authentication getAuthentification();

	/**
	 * Returns whether or not this server has a user identification password.
	 * 
	 * @return
	 */
	public boolean hasUserPassword();

	/**
	 * Possibly returns the user password set for the connection. If the
	 * authentication method does not require a password, this can be null.
	 * 
	 * @return
	 */
	public String getUserPassword();

	/**
	 * Returns whether or not this server has a connection password.
	 * 
	 * @return
	 */
	public boolean hasPassword();

	/**
	 * Returns this server's connection password. Might be null if the server has
	 * none.
	 * 
	 * @return
	 */
	public String getPassword();

	/**
	 * Returns the channels tracked on this server.
	 * 
	 * @return
	 */
	public Collection<? extends StorytellerChannel> getRelatedChannels();
}
