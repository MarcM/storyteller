package de.blanksteg.storyteller.database;

import de.blanksteg.storyteller.common.Authentication;
import de.blanksteg.storyteller.common.StorytellerBot;
import de.blanksteg.storyteller.common.StorytellerChannel;
import de.blanksteg.storyteller.common.StorytellerPack;
import de.blanksteg.storyteller.common.StorytellerServer;

/**
 * The DatabaseObserver interface defines methods used to update classes
 * observing a {#link {@link DatabaseController} about its interactions with the
 * database. Classes implementing this should register themselves using the
 * {@link AsynchronousDatabaseController#addObserver(DatabaseObserver)} and
 * {@link DatabaseController#addObserver(DatabaseObserver)} methods.
 * 
 * @see AsynchronousDatabaseController
 * @see DatabaseController
 * @author Marc Müller
 */
public interface DatabaseObserver {
	/**
	 * A new server with the given properties has been added.
	 * 
	 * @param host
	 * @param port
	 * @param nick
	 * @param user
	 * @param real
	 * @param auth
	 * @param userPassword
	 * @param password
	 * @see StorytellerServer
	 */
	public void notifyServerAdded(String host, int port, String nick, String user, String real, Authentication auth, String userPassword, String password);

	/**
	 * A new channel with the given properties has been added.
	 * 
	 * @param host
	 * @param channel
	 * @param password
	 * @see StorytellerChannel
	 */
	public void notifyChannelAdded(String host, String channel, String password);

	/**
	 * A new bot with the given properties has been added.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @param listEnabled
	 * @see StorytellerBot
	 */
	public void notifyBotAdded(String host, String channel, String bot, boolean listEnabled);

	/**
	 * A new pack with the given properties has been added.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @param number
	 * @param file
	 * @param size
	 * @see StorytellerPack
	 */
	public void notifyPackAdded(String host, String channel, String bot, int number, String file, String size);

	/**
	 * An existing pack has been updated with a new file name and size.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @param number
	 * @param oldFile
	 * @param newFile
	 * @param oldSize
	 * @param newSize
	 */
	public void notifyPackUpdated(String host, String channel, String bot, int number, String oldFile, String newFile, String oldSize, String newSize);

	/**
	 * Identity properties of an existing server have been updated.
	 * 
	 * @param host
	 * @param oldNick
	 * @param newNick
	 * @param oldUser
	 * @param newUser
	 * @param oldReal
	 * @param newReal
	 * @param oldAuth
	 * @param newAuth
	 * @param oldUserPassword
	 * @param newUserPassword
	 */
	public void notifyServerIdentityChanged(String host, String oldNick, String newNick, String oldUser, String newUser, String oldReal, String newReal, Authentication oldAuth, Authentication newAuth, String oldUserPassword, String newUserPassword);

	/**
	 * The port of an existing server has been changed.
	 * 
	 * @param host
	 * @param oldPort
	 * @param newPort
	 */
	public void notifyServerPortChanged(String host, int oldPort, int newPort);

	/**
	 * The password of an existing server has been changed.
	 * 
	 * @param host
	 * @param oldPassword
	 * @param newPassword
	 */
	public void notifyServerPasswordChanged(String host, String oldPassword, String newPassword);

	/**
	 * The password of an existing channel has been changed.
	 * 
	 * @param host
	 * @param channel
	 * @param oldPassword
	 * @param newPassword
	 */
	public void notifyChannelPasswordChanged(String host, String channel, String oldPassword, String newPassword);

	/**
	 * The {#link {@link StorytellerBot#isListEnabled()} flag of an existing bot
	 * has been changed.
	 * 
	 * @param host
	 * @param channel
	 * @param oldFlag
	 * @param newFlag
	 */
	public void notifyBotListFlagChanged(String host, String channel, boolean oldFlag, boolean newFlag);

	/**
	 * An existing bot was moved to a different channel.
	 * 
	 * @param host
	 * @param oldChannel
	 * @param newChannel
	 * @param bot
	 */
	public void notifyBotMoved(String host, String oldChannel, String newChannel, String bot);

	/**
	 * A server has been deleted.
	 * 
	 * @param host
	 * @param port
	 * @param nick
	 * @param user
	 * @param real
	 * @param auth
	 * @param userPassword
	 * @param password
	 */
	public void notifyServerDeleted(String host, int port, String nick, String user, String real, Authentication auth, String userPassword, String password);

	/**
	 * A channel has been deleted. Note: Also called due to server deletion for
	 * every channel deleted.
	 * 
	 * @param host
	 * @param channel
	 * @param password
	 */
	public void notifyChannelDeleted(String host, String channel, String password);

	/**
	 * A bot has been deleted. Note: Also called due to channel deletion for every
	 * bot deleted.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @param listEnabled
	 */
	public void notifyBotDeleted(String host, String channel, String bot, boolean listEnabled);

	/**
	 * A pack has been deleted. Note: Also called due to bot deletion for every
	 * pack deleted.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @param number
	 * @param file
	 * @param size
	 */
	public void notifyPackDeleted(String host, String channel, String bot, int number, String file, String size);

	/**
	 * All changes have been written to the database.
	 */
	public void notifyFlush();

	/**
	 * The database controller has been closed.
	 */
	public void notifyClose();
}
