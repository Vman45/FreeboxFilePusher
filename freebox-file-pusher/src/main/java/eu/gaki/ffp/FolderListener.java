package eu.gaki.ffp;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * The listener interface for receiving folder events. The class that is
 * interested in processing a folder event implements this interface, and the
 * object created with that class is registered with a component using the
 * component's <code>addFolderListener<code> method. When
 * the folder event occurs, that object's appropriate
 * method is invoked.
 *
 * @see FolderEvent
 */
public interface FolderListener {

	/**
	 * Folder file.
	 *
	 * @param file
	 *            the file
	 * @param folder
	 *            the folder
	 */
	List<RssFileItem> folderFile(Path dataFile, Path folder) throws IOException;

}
