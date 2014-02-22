package de.blanksteg.storyteller.database;

import javax.persistence.Transient;

class StorytellerEntity {
	@Transient
	protected DatabaseController source;
}
