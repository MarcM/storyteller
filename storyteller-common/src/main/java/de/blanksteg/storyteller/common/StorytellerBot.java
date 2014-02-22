package de.blanksteg.storyteller.common;

import java.util.Collection;

/**
 * Defines methods common to classes representing a bot inside the Storyteller
 * project. Generally, bots are a collection of {@link StorytellerPack}s
 * associated with a specific {@link StorytellerChannel}, further characterized
 * by a nick name. Additionally, bots have a flag expressing whether or not they
 * support XDCC listing.
 * 
 * @author Marc Müller
 */
public interface StorytellerBot {

	/**
	 * Returns this bot's nick name.
	 * 
	 * @return
	 */
	public String getName();

	/**
	 * Returns the channel this bot is residing in.
	 * 
	 * @return
	 */
	public StorytellerChannel getChannel();

	/**
	 * Returns the packs this bot offers.
	 * 
	 * @return
	 */
	public Collection<? extends StorytellerPack> getRelatedPacks();

	/**
	 * Returns whether or not this bot supports XDCC listing.
	 * 
	 * @return
	 */
	public boolean isListEnabled();
}
