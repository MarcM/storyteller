package de.blanksteg.storyteller.common;

/**
 * Defines methods common to classes representing an XDCC pack available for
 * download inside the Storyteller project. A pack represents a certain file
 * offered by a {@link StorytellerBot} for download. Files are characterized by
 * their file name and size.
 * 
 * @author Marc Müller
 */
public interface StorytellerPack {

	/**
	 * Returns this bot that offers this pack.
	 * 
	 * @return
	 */
	public StorytellerBot getOwningBot();

	/**
	 * Returns the pack number used to identify this pack.
	 * 
	 * @return
	 */
	public int getPackNumber();

	/**
	 * Returns the name of the file offered in this pack.
	 * 
	 * @return
	 */
	public String getFileName();

	/**
	 * Returns the size of the file offered in this pack.
	 * 
	 * Note: The file size is a string because it's generally an inaccurate,
	 * verbal description of the size. Not the exact byte count.
	 * 
	 * @return
	 */
	public String getFileSize();
}
