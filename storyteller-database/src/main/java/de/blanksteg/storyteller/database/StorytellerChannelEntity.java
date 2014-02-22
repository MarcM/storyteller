package de.blanksteg.storyteller.database;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;

import de.blanksteg.storyteller.common.StorytellerBot;
import de.blanksteg.storyteller.common.StorytellerChannel;
import de.blanksteg.storyteller.common.StorytellerServer;

@Entity
@NamedQuery(name = "StorytellerChannelEntity.get", query = "SELECT e FROM StorytellerChannelEntity e WHERE e.parent.host = :host AND e.name = :channel")
class StorytellerChannelEntity extends StorytellerEntity implements StorytellerChannel {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private int channelId;
	private String name;
	@ManyToOne
	private StorytellerServerEntity parent;
	private String password;
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
	private Set<StorytellerBotEntity> bots = new HashSet<StorytellerBotEntity>();

	public StorytellerChannelEntity() {

	}

	public StorytellerChannelEntity(StorytellerServerEntity parent, String channelName, String password) {
		super();
		this.parent = parent;
		this.name = channelName;
		this.password = password;
	}

	@Override
	public StorytellerServer getParentServer() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return this.parent;
		}
	}

	@Override
	public String getName() {
		return this.name;
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
	public Collection<? extends StorytellerBot> getRelatedBots() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return this.bots;
		}
	}

	public Set<StorytellerBotEntity> getBots() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return bots;
		}
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + channelId;
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
		StorytellerChannelEntity other = (StorytellerChannelEntity) obj;
		if (channelId != other.channelId) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.name + "/" + this.parent.getHost();
	}
}
