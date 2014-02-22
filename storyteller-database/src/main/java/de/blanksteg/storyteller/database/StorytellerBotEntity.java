package de.blanksteg.storyteller.database;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

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
import de.blanksteg.storyteller.common.StorytellerPack;

@Entity
@NamedQuery(name = "StorytellerBotEntity.get", query = "SELECT b FROM StorytellerBotEntity b WHERE b.parent.parent.host = :host AND b.parent.name = :channel AND b.name = :name")
class StorytellerBotEntity extends StorytellerEntity implements StorytellerBot {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private int botId;
	private String name;
	@ManyToOne
	private StorytellerChannelEntity parent;
	@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL)
	private List<StorytellerPackEntity> packs = new LinkedList<StorytellerPackEntity>();
	private boolean listEnabled;

	public StorytellerBotEntity() {

	}

	public StorytellerBotEntity(StorytellerChannelEntity channel, String name, boolean listEnabled) {
		this.name = name;
		this.parent = channel;
		this.listEnabled = listEnabled;
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public Collection<? extends StorytellerPack> getRelatedPacks() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return this.packs;
		}
	}

	public Collection<StorytellerPackEntity> getPacks() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return this.packs;
		}
	}

	@Override
	public StorytellerChannel getChannel() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return this.parent;
		}
	}

	public void setChannel(StorytellerChannelEntity channelEntity) {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			this.parent = channelEntity;
		}
	}

	public StorytellerChannelEntity getChannelEntity() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return this.parent;
		}
	}

	public boolean isListEnabled() {
		return listEnabled;
	}

	public void setListEnabled(boolean listEnabled) {
		this.listEnabled = listEnabled;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + botId;
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
		StorytellerBotEntity other = (StorytellerBotEntity) obj;
		if (botId != other.botId) {
			return false;
		}
		return true;
	}

	@Override
	public String toString() {
		return this.parent.getParentServer().getHost() + "/" + this.parent.getName() + "/" + this.name;
	}
}
