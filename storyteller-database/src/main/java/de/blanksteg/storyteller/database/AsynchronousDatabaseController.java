package de.blanksteg.storyteller.database;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import de.blanksteg.storyteller.common.Authentication;
import de.blanksteg.storyteller.common.StorytellerBot;
import de.blanksteg.storyteller.common.StorytellerChannel;
import de.blanksteg.storyteller.common.StorytellerPack;
import de.blanksteg.storyteller.common.StorytellerServer;
import static de.blanksteg.storyteller.database.DatabaseController.checkHostSanity;
import static de.blanksteg.storyteller.database.DatabaseController.checkChannelSanity;
import static de.blanksteg.storyteller.database.DatabaseController.checkNicknameSanity;
import static de.blanksteg.storyteller.database.DatabaseController.checkFilenameSanity;
import static de.blanksteg.storyteller.database.DatabaseController.checkPackSearchSanity;
import static de.blanksteg.storyteller.database.DatabaseController.checkAuthentificationSantiy;

/**
 * This database controller offers all of the functionality the
 * {@link DatabaseController} does, but the methods used don't block and return
 * a {@link Future} instead of the return value. The actual database interaction
 * is performed in a separate thread. However, it is guaranteed that the
 * interactions are performed in the order the respective methods were called.
 * Additionally, all queued jobs will be finished before the
 * {@link AsynchronousDatabaseController#close(long, TimeUnit)} returns.
 * 
 * Note: While the {@link DatabaseController} performs appropriate sanity
 * checking and thus any {@link Future} will contain exceptions for invalid
 * arguments or unsupported operations, the methods in this class perform the
 * very same sanity checking before a job that is bound to fail is even
 * submitted. Thus, for illegal arguments or operations the methods immediately
 * fail.
 * 
 * @see DatabaseController
 * @see Future
 * @author Marc Müller
 */
public class AsynchronousDatabaseController {

	private static final Logger l = Logger.getLogger("de.blanksteg.storyteller.database.async");

	/** The service used to run jobs. */
	private final ExecutorService exec = Executors.newSingleThreadScheduledExecutor();
	/** The underlying controller. */
	private final DatabaseController controller;

	public AsynchronousDatabaseController(DatabaseController controller) {
		super();
		this.controller = controller;
		l.debug("Created a new asynchronous database controller.");
	}

	/**
	 * Registers an observer to the underlying controller. Note: The addition is
	 * also scheduled as a job, meaning the observer will only receive
	 * notifications about jobs scheduled after him.
	 * 
	 * @param obs
	 */

	public synchronized void addObserver(final DatabaseObserver obs) {
		this.exec.submit(new Runnable() {

			@Override
			public void run() {
				controller.addObserver(obs);
			}
		});
	}

	/**
	 * Unregisters an observer from the underlying controller. Note: The removal
	 * is also scheduled as a job, meaning the observer will continue to receive
	 * updates for all jobs scheduled before his removal.
	 * 
	 * @param obs
	 */

	public synchronized void removeObserver(final DatabaseObserver obs) {
		this.exec.submit(new Runnable() {

			@Override
			public void run() {
				controller.removeObserver(obs);
			}
		});
	}

	public synchronized Future<Void> addServer(final String host, final int port, final String nick, final String user, final String real, final Authentication auth, final String userPassword, final String password) {
		checkHostSanity(host);
		checkNicknameSanity(nick);
		checkAuthentificationSantiy(auth, userPassword);
		l.info("Submitting addServer(" + host + ", " + port + ", " + nick + ", " + user + ", " + real + ", " + auth + ", " + userPassword + ", " + password);
		return this.exec.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				controller.addServer(host, port, nick, user, real, auth, userPassword, password);
				return null;
			}
		});
	}

	public synchronized Future<Void> addChannel(final String host, final String name, final String password) {
		checkHostSanity(host);
		checkChannelSanity(name);
		l.info("Submitting addChannel(" + host + ", " + name + ", " + password + ")");
		return this.exec.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				controller.addChannel(host, name, password);
				return null;
			}
		});
	}

	public synchronized Future<Void> addBot(final String host, final String channelName, final String botName, final boolean listEnabled) {
		checkHostSanity(host);
		checkChannelSanity(channelName);
		checkNicknameSanity(botName);
		l.info("Submitting addBot(" + host + ", " + channelName + ", " + botName + ", " + listEnabled + ")");
		return this.exec.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				controller.addBot(host, channelName, botName, listEnabled);
				return null;
			}
		});
	}

	public synchronized Future<Void> updateOrAddPack(final String host, final String channel, final String botName, final int number, final String fileName, final String fileSize, final boolean introduceBot) {
		checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(botName);
		checkFilenameSanity(fileName);
		l.info("Submitting updateOrAddPack(" + host + ", " + channel + ", " + botName + ", " + number + ", " + fileName + ", " + fileSize + ", " + introduceBot + ")");
		return this.exec.submit(new Callable<Void>() {

			@Override
			public Void call() throws Exception {
				controller.updateOrAddPack(host, channel, botName, number, fileName, fileSize, introduceBot);
				return null;
			}
		});
	}

	public synchronized Future<List<? extends StorytellerPack>> findPack(final String name) {
		checkPackSearchSanity(name);
		l.info("Submitting findPack(" + name + ")");
		return this.exec.submit(new Callable<List<? extends StorytellerPack>>() {

			@Override
			public List<? extends StorytellerPack> call() throws Exception {
				return controller.findPack(name);
			}
		});
	}

	public synchronized Future<List<? extends StorytellerPack>> findPackOnServer(final String host, final String name) {
		checkHostSanity(host);
		checkPackSearchSanity(name);
		l.info("Submitting findPackOnServer(" + host + ", " + name + ")");
		return this.exec.submit(new Callable<List<? extends StorytellerPack>>() {

			@Override
			public List<? extends StorytellerPack> call() throws Exception {
				return controller.findPackOnServer(host, name);
			}
		});
	}

	public synchronized Future<List<? extends StorytellerPack>> findPackInChannel(final String host, final String channel, final String name) {
		checkHostSanity(host);
		checkChannelSanity(channel);
		checkPackSearchSanity(name);
		l.info("Submitting findPackInChannel(" + host + ", " + channel + ", " + name + ")");
		return this.exec.submit(new Callable<List<? extends StorytellerPack>>() {

			@Override
			public List<? extends StorytellerPack> call() throws Exception {
				return controller.findPackInChannel(host, channel, name);
			}
		});
	}

	public synchronized Future<List<? extends StorytellerPack>> findPackByBot(final String host, final String channel, final String bot, final String name) {
		checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(bot);
		checkPackSearchSanity(name);
		l.info("Submitting findPackByBot(" + host + ", " + channel + ", " + bot + ", " + name + ")");
		return this.exec.submit(new Callable<List<? extends StorytellerPack>>() {

			@Override
			public List<? extends StorytellerPack> call() throws Exception {
				return controller.findPackByBot(host, channel, bot, name);
			}
		});
	}

	public synchronized Future<StorytellerServer> getServer(final String host) {
		checkHostSanity(host);
		l.info("Submitting getServer(" + host + ")");
		return this.exec.submit(new Callable<StorytellerServer>() {

			@Override
			public StorytellerServer call() throws Exception {
				return controller.getServer(host);
			}
		});
	}

	public synchronized Future<StorytellerChannel> getChannel(final String host, final String channel) {
		checkHostSanity(host);
		checkChannelSanity(channel);
		l.info("Submitting getChannel(" + host + ", " + channel + ")");
		return this.exec.submit(new Callable<StorytellerChannel>() {

			@Override
			public StorytellerChannel call() throws Exception {
				return controller.getChannel(host, channel);
			}
		});
	}

	public synchronized Future<StorytellerBot> getBot(final String host, final String channel, final String bot) {
		checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(bot);
		l.info("Submitting getBot(" + host + ", " + channel + ", " + bot + ")");
		return this.exec.submit(new Callable<StorytellerBot>() {

			@Override
			public StorytellerBot call() throws Exception {
				return controller.getBot(host, channel, bot);
			}
		});
	}

	public synchronized Future<StorytellerPack> getPack(final String host, final String channel, final String bot, final int number) {
		checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(bot);
		l.info("Submitting getPack(" + host + ", " + channel + ", " + bot + ", " + number + ")");
		return this.exec.submit(new Callable<StorytellerPack>() {

			@Override
			public StorytellerPack call() throws Exception {
				return controller.getPack(host, channel, bot, number);
			}
		});
	}

	public synchronized Future<Boolean> setServerIdentity(final String host, final String nick, final String user, final String real, final Authentication auth, final String userPassword) {
		checkHostSanity(host);
		checkNicknameSanity(nick);
		checkAuthentificationSantiy(auth, userPassword);
		l.info("Submitting setServerIdentity(" + host + ", " + nick + ", " + user + ", " + real + ", " + auth + ", " + userPassword + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.setServerIdentity(host, nick, user, real, auth, userPassword);
			}
		});
	}

	public synchronized Future<Boolean> setServerPort(final String host, final int port) {
		checkHostSanity(host);
		l.info("Submitting setServerPort(" + host + ", " + port + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.setServerPort(host, port);
			}
		});
	}

	public synchronized Future<Boolean> setServerPassword(final String host, final String password) {
		checkHostSanity(host);
		l.info("Submitting setServerPassword(" + host + ", " + password + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.setServerPassword(host, password);
			}
		});
	}

	public synchronized Future<Boolean> setChannelPassword(final String host, final String channelName, final String password) {
		checkHostSanity(host);
		checkChannelSanity(channelName);
		l.info("Submitting setChannelPassword(" + host + ", " + channelName + ", " + password + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.setChannelPassword(host, channelName, password);
			}
		});
	}

	public synchronized Future<Boolean> setBotListEnabled(final String host, final String channel, final String botName, final boolean listEnabled) {
		checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(botName);
		l.info("Submitting setBotListEnabled(" + host + ", " + channel + ", " + botName + ", " + listEnabled + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.setBotListEnabled(host, channel, botName, listEnabled);
			}
		});
	}

	public synchronized Future<Boolean> setBotChannel(final String host, final String oldChannel, final String newChannel, final String botName) {
		checkHostSanity(host);
		checkChannelSanity(oldChannel);
		checkChannelSanity(newChannel);
		checkNicknameSanity(botName);
		l.info("Submitting setBotChannel(" + host + ", " + oldChannel + ", " + newChannel + ", " + botName + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.setBotChannel(host, oldChannel, newChannel, botName);
			}
		});
	}

	public synchronized Future<Boolean> deleteServer(final String host) {
		checkHostSanity(host);
		l.info("Submitting deleteServer(" + host + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.deleteServer(host);
			}
		});
	}

	public synchronized Future<Boolean> deleteChannel(final String host, final String channelName) {
		checkHostSanity(host);
		checkChannelSanity(channelName);
		l.info("Submitting deleteChannel(" + host + ", " + channelName + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.deleteChannel(host, channelName);
			}
		});
	}

	public synchronized Future<Boolean> deleteBot(final String host, final String channel, final String botName) {
		checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(botName);
		l.info("Submitting deleteBot(" + host + ", " + channel + ", " + botName + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.deleteBot(host, channel, botName);
			}
		});
	}

	public synchronized Future<Boolean> deletePack(final String host, final String channel, final String bot, final int number) {
		checkHostSanity(host);
		checkChannelSanity(channel);
		checkNicknameSanity(bot);
		l.info("Submitting deletePack(" + host + ", " + channel + ", " + bot + ", " + number + ")");
		return this.exec.submit(new Callable<Boolean>() {

			@Override
			public Boolean call() throws Exception {
				return controller.deletePack(host, channel, bot, number);
			}
		});
	}

	public synchronized Future<List<? extends StorytellerServer>> getServerList() {
		l.info("Submitting getServerList()");
		return this.exec.submit(new Callable<List<? extends StorytellerServer>>() {

			@Override
			public List<? extends StorytellerServer> call() throws Exception {
				return controller.getServerList();
			}
		});
	}

	public synchronized void close(long timeout, TimeUnit unit) throws InterruptedException {
		l.info("Closing this asynchronous database controller.");
		this.exec.shutdown();
		l.info("Waiting for current jobs to finish...");
		try {
			this.exec.awaitTermination(timeout, unit);
		} catch (InterruptedException e) {
			l.debug("Got interrupted while waiting.");
			throw e;
		}
		this.controller.close();
		l.info("Done with all jobs. Closed the underlying database controller.");
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((controller == null) ? 0 : controller.hashCode());
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
		AsynchronousDatabaseController other = (AsynchronousDatabaseController) obj;
		if (controller == null) {
			if (other.controller != null) {
				return false;
			}
		} else if (!controller.equals(other.controller)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return "AsynchronousDatabaseController [controller=" + controller + "]";
	}
}
