package de.blanksteg.storyteller.database;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

import de.blanksteg.storyteller.common.StorytellerBot;
import de.blanksteg.storyteller.common.StorytellerPack;

@Entity
// F OFF
@NamedQueries({
	@NamedQuery(name = "StorytellerPackEntity.get", query = "SELECT p FROM StorytellerPackEntity p WHERE p.parent.parent.parent.host = :host AND p.parent.parent.name = :channel AND p.parent.name = :bot AND p.number = :number"),
	@NamedQuery(name = "StorytellerPackEntity.find.name", query = "SELECT p FROM StorytellerPackEntity p WHERE p.file LIKE :name"),
	@NamedQuery(name = "StorytellerPackEntity.find.server", query = "SELECT p FROM StorytellerPackEntity p WHERE p.parent.parent.parent.host = :host AND p.file LIKE :name"),
	@NamedQuery(name = "StorytellerPackEntity.find.channel", query = "SELECT p FROM StorytellerPackEntity p WHERE p.parent.parent.parent.host = :host AND p.parent.parent.name = :channel AND p.file LIKE :name"),
	@NamedQuery(name = "StorytellerPackEntity.find.bot", query = "SELECT p FROM StorytellerPackEntity p WHERE p.parent.parent.parent.host = :host AND p.parent.parent.name = :channel AND p.parent.name = :bot AND p.file LIKE :name")
})
// F ON
class StorytellerPackEntity extends StorytellerEntity implements StorytellerPack {

	@Id
	@GeneratedValue(strategy = GenerationType.TABLE)
	private int packId;
	private int number;
	@ManyToOne
	private StorytellerBotEntity parent;
	private String file;
	private String size;

	public StorytellerPackEntity() {

	}

	public StorytellerPackEntity(StorytellerBotEntity bot, int number, String file, String size) {
		super();
		this.number = number;
		this.parent = bot;
		this.file = file;
		this.size = size;
	}

	@Override
	public StorytellerBot getOwningBot() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return this.parent;
		}
	}

	@Override
	public int getPackNumber() {
		return this.number;
	}

	@Override
	public String getFileName() {
		return this.file;
	}

	@Override
	public String getFileSize() {
		return this.size;
	}

	public String getFile() {
		return file;
	}

	public void setFile(String file) {
		this.file = file;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public StorytellerBotEntity getBotEntity() {
		synchronized (this.source) {
			if (this.source.isClosed())
				throw new IllegalStateException("Can't retrieve relations from an entity whose corresponding database controller has been closed.");
			return this.parent;
		}
	}

	@Override
	public String toString() {
		return this.parent.getChannel().getParentServer().getHost() + "/" + this.parent.getChannel().getName() + "/" + this.parent.getName() + "/" + this.number + " - [" + this.size + "]: " + this.file;
	}
}
