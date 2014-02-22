package de.blanksteg.storyteller.common;

import java.util.Collection;

/**
 * Defines methods common to classes representing an IRC channel in the
 * Storyteller project. A channel offers a collection of related bots residing
 * inside it. Channels might be password protected, so the required password is
 * available.
 * 
 * @author Marc Müller
 */
public interface StorytellerChannel {

	/**
	 * Returns the server this channel is on.
	 * 
	 * @return
	 */
	public StorytellerServer getParentServer();

	/**
	 * Returns this channels name.
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Returns whether or not this channel requires a password to enter.
	 * 
	 * @return
	 */
	public boolean hasPassword();

	/**
	 * Returns this channel's password. Is null if
	 * {@link StorytellerChannel#hasPassword()} is true.
	 * 
	 * @return
	 */
	public String getPassword();

	/**
	 * Returns the tracked bots that are residing in this channel.
	 * 
	 * @return
	 */
	public Collection<? extends StorytellerBot> getRelatedBots();
}
