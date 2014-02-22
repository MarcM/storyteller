package de.blanksteg.storyteller.database;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import de.blanksteg.storyteller.common.Authentication;
import de.blanksteg.storyteller.common.StorytellerChannel;
import de.blanksteg.storyteller.common.StorytellerServer;

@Entity
class StorytellerServerEntity extends StorytellerEntity implements StorytellerServer {
	@Id
	private String host;
	private int port;
	private String nickName;
	private String userName;
	private String realName;
	private Authentication auth;
	private String userPassword;
	private String password;
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
	private Set<StorytellerChannelEntity> channels = new HashSet<StorytellerChannelEntity>();

	public StorytellerServerEntity() {

	}

	public StorytellerServerEntity(String host, int port, String nick, String user, String real, Authentication auth, String userPassword, String password) {
		super();
		this.host = host;
		this.port = port;
		this.nickName = nick;
		this.userName = user;
		this.realName = real;
		this.auth = auth;
		this.userPassword = userPassword;
		this.password = password;
	}

	@Override
	public String getHost() {
		return this.host;
	}

	@Override
	public int getPort() {
		return this.port;
	}

	@Override
	public String getNickName() {
		return this.nickName;
	}

	@Override
	public String getUserName() {
		return this.userName;
	}

	@Override
	public String getRealName() {
		return this.realName;
	}

	@Override
	public Authentication getAuthentification() {
		return this.auth;
	}

	@Override
	public boolean hasUserPassword() {
		return this.userPassword != null;
	}

	@Override
	public String getUserPassword() {
		return this.userPassword;
	}

	@Override
	public boolean hasPassword() {
		return this.password != null;
	}

	@Override
	public String getPassword() {
		return this.password;
	}

	@Override
	public Collection<? extends StorytellerChannel> getRelatedChannels() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return this.channels;
		}
	}

	public String getNick() {
		return nickName;
	}

	public void setNick(String nick) {
		this.nickName = nick;
	}

	public String getUser() {
		return userName;
	}

	public void setUser(String user) {
		this.userName = user;
	}

	public String getReal() {
		return realName;
	}

	public void setReal(String real) {
		this.realName = real;
	}

	public Authentication getAuth() {
		return auth;
	}

	public void setAuth(Authentication auth) {
		this.auth = auth;
	}

	public Set<StorytellerChannelEntity> getChannels() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return channels;
		}
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUserPassword(String userPassword) {
		this.userPassword = userPassword;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((host == null) ? 0 : host.hashCode());
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
		StorytellerServerEntity other = (StorytellerServerEntity) obj;
		if (host == null) {
			if (other.host != null) {
				return false;
			}
		} else if (!host.equals(other.host)) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.nickName + "@" + this.host;
	}
}
