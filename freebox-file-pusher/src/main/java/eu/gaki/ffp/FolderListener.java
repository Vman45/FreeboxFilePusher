package eu.gaki.ffp;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;

/**
 * The listener interface for receiving folder events. The class that is
 * interested in processing a folder event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addFolderListener<code> method. When
 * the folder event occurs, that object's appropriate
 * method is invoked.
 */
public interface FolderListener {

	/**
	 * Folder file.
	 *
	 * @param folderScanned the folder scanned
	 * @param path the path
	 * @return the list
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	Collection<RssFileItem> folderFile(Path folderScanned, Path path) throws IOException;

	/**
	 * Ending.
	 *
	 * @param folderScanned the folder scanned
	 */
	void ending(Path folderScanned);

	/**
	 * Beginning.
	 *
	 * @param folderScanned the folder scanned
	 * @return the object
	 */
	void beginning(Path folderScanned);

}
