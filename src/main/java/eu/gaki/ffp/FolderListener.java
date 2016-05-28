package eu.gaki.ffp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Set;

import eu.gaki.ffp.domain.RssFileItem;

/**
 * The listener interface for receiving folder events. The class that is
 * interested in processing a folder event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addFolderListener<code> method. When the folder event
 * occurs, that object's appropriate method is invoked.
 */
public interface FolderListener {

	/**
	 * Checks if is already pushed.
	 *
	 * @param path
	 *            the path
	 * @return true, if is already pushed
	 */
	boolean isAlreadyPushed(Path path);

	/**
	 * Folder file.
	 *
	 * @param folderScanned
	 *            the folder scanned (parent folder of 'path' attribute)
	 * @param path
	 *            the file/folder to deal with
	 * @throws IOException
	 *             Signals that an I/O exception has occurred.
	 */
	void scanPath(Path folderScanned, Path path) throws IOException;

	/**
	 * Launch after scanning of all watcher folder (for clean, star/stop
	 * purpose).
	 */
	void afterScans();

	/**
	 * Gets the rss item list.
	 *
	 * @return the rss item list or an empty list
	 */
	Set<RssFileItem> getRssItemList();

}
