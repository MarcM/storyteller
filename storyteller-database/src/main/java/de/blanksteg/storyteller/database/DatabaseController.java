package de.blanksteg.storyteller.database;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.apache.log4j.Logger;

import de.blanksteg.storyteller.common.Authentication;
import de.blanksteg.storyteller.common.StorytellerBot;
import de.blanksteg.storyteller.common.StorytellerChannel;
import de.blanksteg.storyteller.common.StorytellerPack;
import de.blanksteg.storyteller.common.StorytellerServer;

/**
 * The database controller forms the main tool for all Storyteller database
 * interactions by providing methods for storage, modification, retrieval,
 * search and removal of entities common in the Storyteller project to and from
 * a persistent storage.
 * 
 * This class is thread-safe and can be shared among multiple threads. However,
 * beware that entity instances returned by database controller instances thus
 * acquire the lock on their respective controller, since lazy fetching might
 * retrieve entity relations bypassing the controller's synchronization
 * otherwise. It follows users should avoid prolonged usage of entity instances
 * returned by this class but rather copy relevant data.
 * 
 * Classes implementing the {@link DatabaseObserver} interface can register
 * themselves to this class using the
 * {@link DatabaseController#addObserver(DatabaseObserver)} method. Any event an
 * observer gets notified about is guaranteed to have been completed when the
 * notification method is called, meaning the notification about the deletion of
 * an entity is called after the entity has been deleted. Users should be aware
 * of the synchronization as notification methods are always called from within
 * synchronized methods in the respective database controller.
 * 
 * @see AsynchronousDatabaseController
 * @see DatabaseObserver
 * @author Marc Müller
 */
public class DatabaseController {
	private static final Logger l = Logger.getLogger("de.blanksteg.storyteller.database");

	/** The pattern channel names are checked against. */
	public static final String CHANNEL_MATCH = "[#&][a-zA-Z_-]{1,32}";
	/**
	 * The pattern nick names are checked against. Both server identity and bot
	 * names are verified with this.
	 */
	public static final String NICK_MATCH = "[a-zA-Z_|-]{1,32}";

	/**
	 * Helper method used for sanity checking a given host name. Host names might
	 * not be null or empty. Additionally, this method returns the lower case
	 * trimmed host name since hosts are case-insensitive.
	 * 
	 * @throws IllegalArgumentExcption
	 *           If null or pure whitespace is given.
	 * @param host
	 * @return The host name in lower case and trimmed
	 */
	protected static final String checkHostSanity(String host) {
		if (host == null || host.trim().isEmpty())
			throw new IllegalArgumentException("Can't have a null or empty host: " + host);
		return host.trim().toLowerCase();
	}

	/**
	 * Helper method used for sanity checking a given channel name. Channel names
	 * might not be null or empty and must match
	 * {@link DatabaseController#CHANNEL_MATCH}.
	 * 
	 * @throws IllegalArgumentException
	 *           For null, pure whitespace or non-matching channel name.
	 * @param channel
	 */
	protected static final void checkChannelSanity(String channel) {
		if (channel == null || channel.trim().isEmpty())
			throw new IllegalArgumentException("Can't have a null or empty channel name: " + channel);
		if (!channel.matches(CHANNEL_MATCH))
			throw new IllegalArgumentException("Channel name must match " + CHANNEL_MATCH + " but " + channel + " was given.");
	}

	/**
	 * Helper method used for sanity checking a nickname. Nicknames might not be
	 * null or empty and must match {@link DatabaseController#NICK_MATCH}.
	 * 
	 * @throws IllegalArgumentException
	 *           For null, pure whitespace or non-matching nickname.
	 * @param name
	 */
	protected static final void checkNicknameSanity(String name) {
		if (name == null || name.trim().isEmpty())
			throw new IllegalArgumentException("Can't have a null or empty nick name: " + name);
		if (!name.matches(NICK_MATCH))
			throw new IllegalArgumentException("Nickname must match " + NICK_MATCH + " but " + name + " was given.");
	}

	/**
	 * Helper method used for sanity checking a filename. Filenames might not be
	 * null or empty.
	 * 
	 * @throws IllegalArgumentException
	 *           For null or pure whitespace.
	 * @param name
	 */
	protected static final void checkFilenameSanity(String name) {
		if (name == null || name.trim().isEmpty())
			throw new IllegalArgumentException("Can't have a null or empty file name: " + name);
	}

	/**
	 * Helper method used for sanity checking pack queries. Queries might not be
	 * null or pure whitespace.
	 * 
	 * @throws IllegalArgumentException
	 *           For null, pure whitespace or a query containing '%'.
	 * @param name
	 */
	protected static final void checkPackSearchSanity(String name) {
		if (name == null || name.trim().isEmpty())
			throw new IllegalArgumentException("Can't search for a null or empty file name:" + name);
		if (name.contains("%"))
			throw new IllegalArgumentException("Pack name search must not contain '%': " + name);
	}

	/**
	 * Helper method used for sanity checking {@link Authentication} options
	 * along with a password. If the authentification type requires a password,
	 * the given password might not be empty.
	 * 
	 * @throws IllegalArgumentException
	 *           If the authentification requires a password but the given one is
	 *           null or pure whitespace.
	 * @see Authentication
	 * @param auth
	 * @param userPassword
	 */
	protected static final void checkAuthentificationSantiy(Authentication auth, String userPassword) {
		if (auth.hasPassRequired() && (userPassword == null || userPassword.isEmpty()))
			throw new IllegalArgumentException("Password required for the given authentification method: " + auth + " but \"" + userPassword + "\" was given.");
	}

	/**
	 * The {@link EntityManager instance used for all of this controller's JPA
	 * interactions.
	 */
	private final EntityManager eman;
	/** A set of observers that get notified about database interactions. */
	private final Set<DatabaseObserver> observers = new HashSet<DatabaseObserver>();

	/**
	 * Constructs a new controller interacting with the given
	 * {@link EntityManager}.
	 * 
	 * @param eman
	 */
	protected DatabaseController(EntityManager eman) {
		this.eman = eman;
		this.eman.getTransaction().begin();
		l.debug("Created a new database controller and started its transaction.");
	}

	/**
	 * Helper method used to check if the underlying {@link EntityManager} is
	 * actually open.
	 */
	private final void checkClosed() {
		if (!this.eman.isOpen())
			throw new IllegalStateException("Can't perform entity operations on a closed database controller.");
	}

	/**
	 * Register an observer to this controller. An observer can only be registered
	 * once and observers are called in arbitrary order, not order of
	 * registration.
	 * 
	 * @param obs
	 */
	public void addObserver(DatabaseObserver obs) {
		if (obs == null)
			throw new IllegalArgumentException("Observer is not allowed to be null.");
		if (this.observers.contains(obs))
			throw new UnsupportedOperationException("Can't add the same observer twice: " + obs);
		this.observers.add(obs);
		l.debug("Added new database observer: " + obs);
	}

	/**
	 * Removes a registered observer from this controller.
	 * 
	 * @param obs
	 */
	public void removeObserver(DatabaseObserver obs) {
		if (obs == null)
			throw new IllegalArgumentException("Observer is not allowed to be null.");
		if (!this.observers.contains(obs))
			throw new UnsupportedOperationException("Can't remove unregistered observers: " + obs);
		this.observers.remove(obs);
		l.debug("Removed database observer: " + obs);
	}

	/**
	 * Retrieves a server using its primary key.
	 * 
	 * @param host
	 * @return The server with the given host or null if none is found.
	 */
	private StorytellerServerEntity findServer(String host) {
		StorytellerServerEntity ret = this.eman.find(StorytellerServerEntity.class, host);
		if (ret != null)
			ret.source = this;
		return ret;
	}

	/**
	 * Retrieves a channel using its parent host and name.
	 * 
	 * @param host
	 * @param channel
	 * @return The channel with the given name on the given host or null if none
	 *         is found.
	 */
	private StorytellerChannelEntity findChannel(String host, String channel) {
		TypedQuery<StorytellerChannelEntity> query = this.eman.createNamedQuery("StorytellerChannelEntity.get", StorytellerChannelEntity.class);
		query.setParameter("host", host);
		query.setParameter("channel", channel);
		List<StorytellerChannelEntity> results = query.getResultList();
		assert results.size() < 2;
		StorytellerChannelEntity ret = results.isEmpty() ? null : results.get(0);
		if (ret != null)
			ret.source = this;
		return ret;
	}

	/**
	 * Retrieves a bot using its parent host and channel and its name.
	 * 
	 * @param host
	 * @param channel
	 * @param name
	 * @return The bot in the given channel on the given host or null if none is
	 *         found.
	 */
	private StorytellerBotEntity findBot(String host, String channel, String name) {
		TypedQuery<StorytellerBotEntity> query = this.eman.createNamedQuery("StorytellerBotEntity.get", StorytellerBotEntity.class);
		query.setParameter("host", host);
		query.setParameter("channel", channel);
		query.setParameter("name", name);
		List<StorytellerBotEntity> results = query.getResultList();
		assert results.size() < 2;
		StorytellerBotEntity ret = results.isEmpty() ? null : results.get(0);
		if (ret != null)
			ret.source = this;
		return ret;
	}

	/**
	 * Retrieves a pack using its parent bot, channel and host and pack number.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @param number
	 * @return The pack in from the given bot in the given channel on the given
	 *         host or null if none is found.
	 */
	private StorytellerPackEntity findPack(String host, String channel, String bot, int number) {
		TypedQuery<StorytellerPackEntity> query = this.eman.createNamedQuery("StorytellerPackEntity.get", StorytellerPackEntity.class);
		query.setParameter("host", host);
		query.setParameter("channel", channel);
		query.setParameter("bot", bot);
		query.setParameter("number", number);
		List<StorytellerPackEntity> results = query.getResultList();
		assert results.size() < 2;
		StorytellerPackEntity ret = results.isEmpty() ? null : results.get(0);
		if (ret != null)
			ret.source = this;
		return ret;
	}

	/**
	 * Adds a server using the given host and identity data. A server can only be
	 * added once. If the user- and/or real name is null, the nick name is used
	 * instead of them. The user password can be null if the
	 * {@link Authentication#hasPassRequired()} is false. The server password
	 * might be null.
	 * 
	 * @param host
	 * @param port
	 * @param nick
	 * @param user
	 * @param real
	 * @param auth
	 * @param userPassword
	 * @param password
	 * @see DatabaseController#checkHostSanity(String)
	 * @see DatabaseController#checkNicknameSanity(String)
	 * @see DatabaseController#checkAuthentificationSantiy(Authentication,
	 *      String)
	 */
	public synchronized void addServer(String host, int port, String nick, String user, String real, Authentication auth, String userPassword, String password) {
		l.trace("Attempting to add a new server.");
		this.checkClosed();
		host = checkHostSanity(host);
		checkNicknameSanity(nick);
		if (user == null)
			user = nick;
		if (real == null)
			real = nick;
		checkAuthentificationSantiy(auth, userPassword);
		if (this.findServer(host) != null) {
			throw new UnsupportedOperationException("The server " + host + " is already known.");
		}

		l.info("Adding a new server: (" + host + ", " + port + ", " + nick + "," + user + ", " + real + ", " + auth + ", " + userPassword + ", " + password + ")");
		StorytellerServerEntity persist = new StorytellerServerEntity(host, port, nick, user, real, auth, userPassword, password);
		this.eman.persist(persist);
		for (DatabaseObserver obs : this.observers)
			obs.notifyServerAdded(host, port, nick, user, real, auth, userPassword, password);
		this.flush();
		l.debug("Persisted newly added server: " + persist);
	}

	/**
	 * Adds a channel to the database and the server identified by the given host.
	 * The host server has to be added before the channel. The password might be
	 * null if the channel has no password. A channel can only be added once.
	 * 
	 * @param host
	 * @param name
	 * @param password
	 * @see DatabaseController#checkHostSanity(String)
	 * @see DatabaseController#checkChannelSanity(String)
	 */
	public synchronized void addChannel(String host, String name, String password) {
		l.trace("Attempting to add a new channel.");
		this.checkClosed();
		host = checkHostSanity(host);
		checkChannelSanity(name);

		StorytellerServerEntity server = this.findServer(host);
		if (server == null) {
			throw new UnsupportedOperationException("The server " + host + " must be added before the channel " + name);
		}
		l.trace("Found supposed server to add the channel to " + server);
		l.info("Adding a new channel: (" + host + ", " + name + ", " + password + ")");
		StorytellerChannelEntity channel = new StorytellerChannelEntity(server, name, password);
		if (server.getChannels().contains(channel)) {
			throw new UnsupportedOperationException("The channel " + name + " is already contained in the server " + host);
		}
		this.eman.persist(channel);
		for (DatabaseObserver obs : this.observers)
			obs.notifyChannelAdded(host, name, password);
		this.flush();
		this.eman.refresh(server);
		l.debug("Persisted newly added channel: " + channel);
	}

	/**
	 * Adds a bot to the database and the channel identified by the given host and
	 * channel name. The channel has to be added before the bot. A bot can only be
	 * added once.
	 * 
	 * @param host
	 * @param channelName
	 * @param botName
	 * @param listEnabled
	 * @see DatabaseController#checkHostSanity(String)
	 * @see DatabaseController#checkChannelSanity(String)
	 * @see DatabaseController#checkNicknameSanity(String)
	 */
	public synchronized void addBot(String host, String channelName, String botName, boolean listEnabled) {
		l.trace("Attempting to add a new bot.");
		this.checkClosed();
		host = checkHostSanity(host);
		checkChannelSanity(channelName);
		checkNicknameSanity(botName);

		StorytellerChannelEntity channel = this.findChannel(host, channelName);
		if (channel == null) {
			throw new UnsupportedOperationException("The server " + host + " and the channel " + channelName + " need to be added before the bot " + botName);
		}
		l.trace("Found supposed channel to add the bot to " + channel);
		l.info("Adding a new bot: (" + host + ", " + channelName + ", " + botName + ", " + listEnabled + ")");
		StorytellerBotEntity bot = new StorytellerBotEntity(channel, botName, listEnabled);
		if (channel.getBots().contains(bot)) {
			throw new UnsupportedOperationException("The bot " + botName + " is already contained in the channel " + channelName);
		}
		this.eman.persist(bot);
		for (DatabaseObserver obs : this.observers)
			obs.notifyBotAdded(host, channelName, botName, listEnabled);
		this.flush();
		this.eman.refresh(channel);
		l.debug("Persisted newly added bot: " + bot);
	}

	/**
	 * This method either updates an existing pack instance or adds a new one. A
	 * pack is updated if a pack of the given number is already found from the
	 * given bot. The new file name and size will then be stored for the existing
	 * pack. Otherwise, a new pack is created with them and added to the given
	 * bot. If the last boolean flag is set, the presence of the bot prior to the
	 * addition of this pack is not necessary and a new bot will be created in
	 * case he is absent from the given channel. Otherwise the bot has to exist
	 * before the pack can be added or updated.
	 * 
	 * @param host
	 * @param channel
	 * @param botName
	 * @param number
	 * @param fileName
	 * @param fileSize
	 * @param introduceBot
	 * @see DatabaseController#checkHostSanity(String)
	 * @see DatabaseController#checkChannelSanity(String)
	 * @see DatabaseController#checkNicknameSanity(String)
	 * @see DatabaseController#checkFilenameSanity(String)
	 */
	public synchronized void updateOrAddPack(String host, String channel, String botName, int number, String fileName, String fileSize, boolean introduceBot) {
		l.trace("Attempting to update or add a pack.");
		this.checkClosed();
		host = checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(botName);
		checkFilenameSanity(fileName);

		StorytellerPackEntity pack = this.findPack(host, channel, botName, number);
		if (pack == null) {
			l.trace("Creating a new pack because no pack was found for (" + host + ", " + channel + ", " + botName + ", " + number + ")");
			StorytellerBotEntity bot = this.findBot(host, channel, botName);
			if (bot == null && introduceBot) {
				this.addBot(host, channel, botName, false);
				bot = this.findBot(host, channel, botName);
				assert bot != null;
			}
			if (bot != null) {
				l.trace("Found supposed bot to add the pack to " + bot);
				l.info("Creating a new pack (" + host + ", " + channel + ", " + botName + ", " + number + ", " + fileName + ", " + fileSize + ")");
				pack = new StorytellerPackEntity(bot, number, fileName, fileSize);
				this.eman.persist(pack);
				for (DatabaseObserver obs : this.observers)
					obs.notifyPackAdded(host, channel, botName, number, fileName, fileSize);
				this.flush();
				this.eman.refresh(bot);
				l.debug("Persisted newly created pack: " + pack);
			} else {
				throw new UnsupportedOperationException("Can't update or add a pack to a bot that doesn't exist: " + botName);
			}
		} else {
			String oldFile = pack.getFile();
			String oldSize = pack.getSize();
			pack.setFile(fileName);
			pack.setSize(fileSize);
			l.info("Updating pack (" + number + ", " + oldFile + " -> " + fileName + ", " + oldSize + " -> " + fileSize + ")");
			this.eman.merge(pack);
			l.debug("Merged updated pack: " + pack);
			for (DatabaseObserver obs : this.observers)
				obs.notifyPackUpdated(host, channel, botName, number, oldFile, fileName, oldSize, fileSize);
		}
	}

	/**
	 * Helper method used to search packs with a query and associate them with
	 * this controller. The name must not be null, pure whitespace.
	 * 
	 * @param q
	 * @param name
	 * @return The result list with each entry aware of its source.
	 * @see DatabaseController#checkPackSearchSanity(String)
	 */
	private List<? extends StorytellerPack> findPack(TypedQuery<StorytellerPackEntity> q, String name) {
		checkPackSearchSanity(name);
		q.setParameter("name", "%" + name + "%");
		this.flush();
		List<StorytellerPackEntity> ret = q.getResultList();
		for (StorytellerPackEntity pack : ret) {
			pack.source = this;
		}
		return ret;
	}

	/**
	 * Searches for a pack containing the given string in its filename.
	 * 
	 * @param name
	 * @return List of results. Empty if nothing is found.
	 * @see DatabaseController#checkPackSearchSanity(String)
	 */
	public synchronized List<? extends StorytellerPack> findPack(String name) {
		l.trace("Attempting to search a pack by name alone");
		this.checkClosed();
		TypedQuery<StorytellerPackEntity> q = this.eman.createNamedQuery("StorytellerPackEntity.find.name", StorytellerPackEntity.class);
		List<? extends StorytellerPack> ret = this.findPack(q, name);
		l.info("Found " + ret.size() + " packs for search: " + name);
		return ret;
	}

	/**
	 * Searches for a pack containing the given string in its filename restricted
	 * to the current host. The host must identify a server known in this database
	 * and might not be null or pure whitespace.
	 * 
	 * @param host
	 * @param name
	 * @return List of results. Empty if nothing is found.
	 * @see DatabaseController#checkHostSanity(String)
	 * @see DatabaseController#checkPackSearchSanity(String)
	 */
	public synchronized List<? extends StorytellerPack> findPackOnServer(String host, String name) {
		l.trace("Attempting to search a pack by host and name.");
		this.checkClosed();
		host = checkHostSanity(host);
		TypedQuery<StorytellerPackEntity> q = this.eman.createNamedQuery("StorytellerPackEntity.find.server", StorytellerPackEntity.class);
		q.setParameter("host", host);
		List<? extends StorytellerPack> ret = this.findPack(q, name);
		l.info("Found " + ret.size() + " packs on " + host + " for search: " + name);
		return ret;
	}

	/**
	 * Searches for a pack containing the given string in its filename restricted
	 * to the given channel on the given host. The host and channel might not be
	 * null or empty and must identify entities known in this database.
	 * 
	 * @param host
	 * @param channel
	 * @param name
	 * @return List of results. Empty if nothing is found.
	 * @see DatabaseController#checkHostSanity(String)
	 * @see DatabaseController#checkChannelSanity(String)
	 * @see DatabaseController#checkPackSearchSanity(String)
	 */
	public synchronized List<? extends StorytellerPack> findPackInChannel(String host, String channel, String name) {
		l.trace("Attempting to search a pack by host, channel and name.");
		this.checkClosed();
		host = checkHostSanity(host);
		checkChannelSanity(channel);
		TypedQuery<StorytellerPackEntity> q = this.eman.createNamedQuery("StorytellerPackEntity.find.channel", StorytellerPackEntity.class);
		q.setParameter("host", host);
		q.setParameter("channel", channel);
		List<? extends StorytellerPack> ret = this.findPack(q, name);
		l.info("Found " + ret.size() + " packs on " + host + " in  " + channel + " for search: " + name);
		return ret;
	}

	/**
	 * Searches for a pack containing the given string in its filename restricted
	 * to the given bot in the given channel on the given host. The bot-, channel-
	 * and host names may not be null or empty and must identify entities known by
	 * this database.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @param name
	 * @return List of results. Empty if nothing is found.
	 * @throws IllegalArgumentException
	 *           For illegal name queries.
	 * @see DatabaseController#checkHostSanity(String)
	 * @see DatabaseController#checkChannelSanity(String)
	 * @see DatabaseController#checkNicknameSanity(String)
	 * @see DatabaseController#checkPackSearchSanity(String)
	 */
	public synchronized List<? extends StorytellerPack> findPackByBot(String host, String channel, String bot, String name) {
		l.trace("Attempting to search a pack by host, channel, bot and name.");
		this.checkClosed();
		host = checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(bot);
		TypedQuery<StorytellerPackEntity> q = this.eman.createNamedQuery("StorytellerPackEntity.find.bot", StorytellerPackEntity.class);
		q.setParameter("host", host);
		q.setParameter("channel", channel);
		q.setParameter("bot", bot);
		List<? extends StorytellerPack> ret = this.findPack(q, name);
		l.info("Found " + ret.size() + " packs on " + host + " in " + channel + " from " + bot + " for search: " + name);
		return ret;
	}

	/**
	 * Returns the server instance identified by the given host. The host name
	 * might not be null or empty.
	 * 
	 * @param host
	 * @see DatabaseController#checkHostSanity(String)
	 * @return The respective host or null if none is found.
	 */
	public synchronized StorytellerServer getServer(String host) {
		l.trace("Attempting to retrieve a server entity.");
		host = checkHostSanity(host);
		StorytellerServer ret = this.findServer(host);
		l.info("Found server: " + ret);
		return ret;
	}

	/**
	 * Returns the channel instance identified by the given host and channel. The
	 * given names might not be null or empty.
	 * 
	 * @param host
	 * @param channel
	 * @return The respective channel or null if none is found.
	 */
	public synchronized StorytellerChannel getChannel(String host, String channel) {
		l.trace("Attempting to retrieve a channel entity.");
		host = checkHostSanity(host);
		checkChannelSanity(channel);
		StorytellerChannel ret = this.findChannel(host, channel);
		l.info("Found channel: " + ret);
		return ret;
	}

	/**
	 * Returns the bot instance identified by the given host, channel and name.
	 * The given names might not be null or empty.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @return The respective bot or null if none is found.
	 */
	public synchronized StorytellerBot getBot(String host, String channel, String bot) {
		l.trace("Attempting to retrieve a bot entity.");
		host = checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(bot);
		StorytellerBot ret = this.findBot(host, channel, bot);
		l.info("Found bot: " + ret);
		return ret;
	}

	/**
	 * Returns the pack instance identified by the given host, channel, bot and
	 * pack number. The given names might not be null or empty.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @param number
	 * @return The respective pack or null if none is found.
	 */
	public synchronized StorytellerPack getPack(String host, String channel, String bot, int number) {
		l.trace("Attempting to retrieve a pack entity.");
		host = checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(bot);
		StorytellerPack ret = this.findPack(host, channel, bot, number);
		l.info("Found pack: " + ret);
		return ret;
	}

	/**
	 * Updates an existing server with new user data. Given user data is rechecked
	 * like in the
	 * {@link DatabaseController#addServer(String, int, String, String, String, Authentication, String, String)}
	 * method and the given host might not be null or empty and must identify a
	 * known server.
	 * 
	 * @param host
	 * @param nick
	 * @param user
	 * @param real
	 * @param auth
	 * @param userPassword
	 * @return True iff the data was changed.
	 */
	public synchronized boolean setServerIdentity(String host, String nick, String user, String real, Authentication auth, String userPassword) {
		l.trace("Attempting to set user data related fields in a server.");
		host = checkHostSanity(host);
		checkNicknameSanity(nick);
		if (user == null)
			user = nick;
		if (real == null)
			real = nick;
		checkAuthentificationSantiy(auth, userPassword);
		StorytellerServerEntity server = this.findServer(host);
		if (server != null) {
			String oldNick = server.getNickName();
			String oldUser = server.getUserName();
			String oldReal = server.getRealName();
			Authentication oldAuth = server.getAuth();
			String oldUserPassword = server.getUserPassword();
			l.info("Updating server identity for host " + host + " (" + oldNick + " -> " + nick + ", " + oldUser + " -> " + user + ", " + oldReal + " -> " + real + ", " + oldAuth + " -> " + auth + ", " + oldUserPassword + " -> " + userPassword + ")");
			server.setNick(nick);
			server.setUser(user);
			server.setReal(real);
			server.setAuth(auth);
			server.setUserPassword(userPassword);
			this.eman.merge(server);
			for (DatabaseObserver obs : this.observers)
				obs.notifyServerIdentityChanged(host, oldNick, nick, oldUser, user, oldReal, real, oldAuth, auth, oldUserPassword, userPassword);
			return true;
		} else {
			l.info("Could not find entity to update for host " + host);
			return false;
		}
	}

	/**
	 * Updates an existing server with a new port. The given host name might not
	 * be null or empty and must identify a known server.
	 * 
	 * @param host
	 * @param port
	 * @return True iff the port was changed.
	 */
	public synchronized boolean setServerPort(String host, int port) {
		l.trace("Attempting to set the port of a server.");
		host = checkHostSanity(host);
		StorytellerServerEntity server = this.findServer(host);
		if (server != null) {
			int oldPort = server.getPort();
			l.info("Updating server port for host " + host + " (" + oldPort + " -> " + port + ")");
			server.setPort(port);
			this.eman.merge(server);
			for (DatabaseObserver obs : this.observers)
				obs.notifyServerPortChanged(host, oldPort, port);
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Updates an existing server with a new password. The given host name might
	 * not be null or empty and must identify a known server. Pass 'null' for no
	 * password.
	 * 
	 * @param host
	 * @param password
	 *          Might be null.
	 * @return True iff the password was changed.
	 */
	public synchronized boolean setServerPassword(String host, String password) {
		l.trace("Attempting to set the password of a server.");
		host = checkHostSanity(host);
		StorytellerServerEntity server = this.findServer(host);
		if (server != null) {
			String oldPassword = server.getPassword();
			l.info("Updating server passsword for host " + host + " (" + oldPassword + " -> " + password + ")");
			server.setPassword(password);
			this.eman.merge(server);
			for (DatabaseObserver obs : this.observers)
				obs.notifyServerPasswordChanged(host, oldPassword, password);
			return true;
		} else {
			l.info("Could not find entity to update for host " + host);
			return false;
		}
	}

	/**
	 * Updates an existing channel with a new password. The given host and channel
	 * names must not be null or empty and identify known entities. Pass 'null'
	 * for no password.
	 * 
	 * @param host
	 * @param channelName
	 * @param password
	 *          Might be null.
	 * @return True iff the password was changed.
	 */
	public synchronized boolean setChannelPassword(String host, String channelName, String password) {
		l.trace("Attempting to set the password of a channel.");
		host = checkHostSanity(host);
		checkChannelSanity(channelName);
		StorytellerChannelEntity channel = this.findChannel(host, channelName);
		if (channel != null) {
			String oldPassword = channel.getPassword();
			l.info("Updating channel password for channel " + channel + " (" + oldPassword + " -> " + password + ")");
			channel.setPassword(password);
			this.eman.merge(channel);
			for (DatabaseObserver obs : this.observers)
				obs.notifyChannelPasswordChanged(host, channelName, oldPassword, password);
			return true;
		} else {
			l.info("Could not find entity to update for channel " + host + "." + channelName);
			return false;
		}
	}

	/**
	 * Change the {@link StorytellerBot#isListEnabled()} flag of an existing bot.
	 * The given host-, channel and bot names might not be null or empty and must
	 * identify known entities.
	 * 
	 * @param host
	 * @param channel
	 * @param botName
	 * @param listEnabled
	 * @return True iff the flag was changed.
	 */
	public synchronized boolean setBotListEnabled(String host, String channel, String botName, boolean listEnabled) {
		l.trace("Attempting to set the listing enabled flag of a bot.");
		host = checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(botName);
		StorytellerBotEntity bot = this.findBot(host, channel, botName);
		if (bot != null) {
			boolean oldFlag = bot.isListEnabled();
			l.info("Updating list enabled flag for bot " + bot + " (" + oldFlag + " -> " + listEnabled + ")");
			bot.setListEnabled(listEnabled);
			this.eman.merge(bot);
			for (DatabaseObserver obs : this.observers)
				obs.notifyBotListFlagChanged(host, channel, oldFlag, listEnabled);
			return true;
		} else {
			l.info("Could not find entity to update for bot " + host + "." + channel + "." + botName);
			return false;
		}
	}

	/**
	 * Moves an existing bot to another channel. All known packs are migrated
	 * alongside the bot. The host, old channel, new channel and bot names must
	 * not be null or empty and must identify known entities. The old and new
	 * channel are not allowed to be the same.
	 * 
	 * @param host
	 * @param oldChannel
	 * @param newChannel
	 * @param botName
	 * @return True iff the bot was moved.
	 */
	public synchronized boolean setBotChannel(String host, String oldChannel, String newChannel, String botName) {
		l.trace("Attempting to move a bot into a another channel.");
		host = checkHostSanity(host);
		checkChannelSanity(oldChannel);
		checkChannelSanity(newChannel);
		checkNicknameSanity(botName);
		if (oldChannel.equals(newChannel))
			throw new UnsupportedOperationException("Can't move a bot into the same channel.");

		StorytellerBotEntity bot = this.findBot(host, oldChannel, botName);
		if (bot != null) {
			StorytellerChannelEntity target = this.findChannel(host, newChannel);
			if (target != null) {
				StorytellerChannelEntity previous = bot.getChannelEntity();
				l.info("Moving bot " + bot + " (" + oldChannel + " -> " + newChannel + ")");
				bot.setChannel(target);
				this.eman.merge(bot);
				this.flush();
				for (DatabaseObserver obs : this.observers)
					obs.notifyBotMoved(host, oldChannel, newChannel, botName);
				this.eman.refresh(previous);
				this.eman.refresh(target);
				return true;
			} else {
				l.info("Could not find target channel " + newChannel + " to move " + bot + " into.");
				return false;
			}
		} else {
			l.info("Could not find bot to move " + host + "." + oldChannel + "." + botName);
			return false;
		}
	}

	/**
	 * Removes the server identified by the given host alongside all of its
	 * channels. The given host might not be null or empty and must identify a
	 * known server.
	 * 
	 * @param host
	 * @return True iff a server was deleted.
	 */
	public synchronized boolean deleteServer(String host) {
		l.trace("Attempting to delete a server.");
		this.checkClosed();
		host = checkHostSanity(host);
		StorytellerServerEntity server = this.findServer(host);
		if (server != null) {
			this.eman.remove(server);
			// TODO: Call observers for all channel, bot and pack deletions due to
			// cascade
			for (DatabaseObserver obs : this.observers)
				obs.notifyServerDeleted(server.getHost(), server.getPort(), server.getNickName(), server.getUserName(), server.getRealName(), server.getAuthentification(), server.getUserPassword(), server.getPassword());
			l.info("Deleted server: " + server);
			return true;
		} else {
			l.info("Could not find server to delete: " + host);
			return false;
		}
	}

	/**
	 * Removes the channel identified by the given host and channel name. Both
	 * names may not be null and must identify known entities. The related bots
	 * are removed with the channel.
	 * 
	 * @param host
	 * @param channelName
	 * @return True iff a channel was deleted.
	 */
	public synchronized boolean deleteChannel(String host, String channelName) {
		l.trace("Attempting to delete a channel.");
		this.checkClosed();
		host = checkHostSanity(host);
		checkChannelSanity(channelName);
		StorytellerChannelEntity channel = this.findChannel(host, channelName);
		if (channel != null) {
			channel.getParentServer().getRelatedChannels().remove(channel);
			this.eman.merge(channel.getParentServer());
			this.flush();
			// TODO: Call observers for all bot and pack deletions due to cascade
			for (DatabaseObserver obs : this.observers)
				obs.notifyChannelDeleted(host, channelName, channel.getPassword());
			l.info("Deleted channel: " + channel);
			return true;
		} else {
			l.info("Could not find server to delete: (" + host + ", " + channelName + ")");
			return false;
		}
	}

	/**
	 * Removes the bot identified by the given host, channel and bot name. All
	 * names may not be null or empty and must identify known entities. The
	 * related packs are removed with the bot.
	 * 
	 * @param host
	 * @param channel
	 * @param botName
	 * @return True iff a bot was deleted.
	 */
	public synchronized boolean deleteBot(String host, String channel, String botName) {
		this.checkClosed();
		host = checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(botName);
		StorytellerBotEntity bot = this.findBot(host, channel, botName);
		if (bot != null) {
			bot.getChannelEntity().getBots().remove(bot);
			this.eman.merge(bot.getChannel());
			this.flush();
			// TODO: Call observers about all pack deletions due to cascade.
			for (DatabaseObserver obs : this.observers)
				obs.notifyBotDeleted(host, channel, botName, bot.isListEnabled());
			l.info("Deleted bot: " + bot);
			return true;
		} else {
			l.info("Could not find bot to delete: (" + host + ", " + channel + ", " + botName + ")");
			return false;
		}
	}

	/**
	 * Removes the pack identified by the given host, chanel, bot name and pack
	 * number. The names might not be null or empty and must identify known
	 * entities.
	 * 
	 * @param host
	 * @param channel
	 * @param bot
	 * @param number
	 * @return True iff a pack was deleted.
	 */
	public synchronized boolean deletePack(String host, String channel, String bot, int number) {
		this.checkClosed();
		host = checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(bot);
		StorytellerPackEntity pack = this.findPack(host, channel, bot, number);
		if (pack != null) {
			pack.getBotEntity().getPacks().remove(pack);
			this.eman.merge(pack.getBotEntity());
			this.flush();
			this.eman.refresh(pack);
			for (DatabaseObserver obs : this.observers)
				obs.notifyPackDeleted(host, channel, bot, number, pack.getFileName(), pack.getFileSize());
			l.info("Deleted pack: " + pack);
			return true;
		} else {
			l.info("Could not find pack to delete: (" + host + ", " + channel + ", " + bot + ", " + number + ")");
			return false;
		}
	}

	/**
	 * Writes the current state to the underlying database.
	 */
	private synchronized void flush() {
		l.trace("Attempting to commit.");
		this.checkClosed();
		this.eman.flush();
		l.debug("Flushed current database state.");
		for (DatabaseObserver obs : this.observers)
			obs.notifyFlush();
	}

	/**
	 * Returns all servers tracked in this database.
	 * 
	 * @return List of servers.
	 */
	public synchronized List<? extends StorytellerServer> getServerList() {
		l.trace("Attempting to retrieve server list.");
		this.checkClosed();
		TypedQuery<StorytellerServerEntity> q = this.eman.createQuery("SELECT t FROM StorytellerServerEntity t", StorytellerServerEntity.class);
		List<? extends StorytellerServer> ret = q.getResultList();
		l.info("Found " + ret.size() + " servers.");
		return ret;
	}

	/**
	 * Closes this database and its underlying {@link EntityManager}. Note that
	 * any future access to this instance will result in an exception. Most
	 * importantly, this also includes access to elements returned from this
	 * controller.
	 */
	public synchronized void close() {
		l.trace("Attempting to close this controller.");
		if (!this.eman.isOpen())
			throw new IllegalStateException("Already closed.");
		this.eman.getTransaction().commit();
		this.eman.close();
		for (DatabaseObserver obs : this.observers)
			obs.notifyClose();
		l.info("Closed the database controller.");
	}

	/**
	 * Checks if this controller has been closed.
	 * 
	 * @return True iff the controller is closed and can't be used anymore.
	 */
	public synchronized boolean isClosed() {
		return !this.eman.isOpen();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((eman == null) ? 0 : eman.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		DatabaseController other = (DatabaseController) obj;
		if (eman == null) {
			if (other.eman != null) {
				return false;
			}
		} else if (!eman.equals(other.eman)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "DatabaseController [eman=" + eman + "]";
	}
}
