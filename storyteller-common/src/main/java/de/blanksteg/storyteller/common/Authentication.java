package de.blanksteg.storyteller.common;

/**
 * This enum defines simple options for identifying a user within an IRC
 * network. Options might require a password which can be checked using
 * {@link Authentication#hasPassRequired()}.
 * 
 * @author Marc Müller
 */
public enum Authentication {
	NONE(false), NICKSERV(true);

	Authentication(boolean passRequired) {
		this.passRequired = passRequired;
	}

	private final boolean passRequired;

	/**
	 * Check if this authentication option requires a password.
	 * 
	 * @return
	 */
	public boolean hasPassRequired() {
		return passRequired;
	}
}
