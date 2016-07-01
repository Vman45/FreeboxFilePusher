/*
 * 
 */
package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;

public class FileService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(FileService.class);

	public FfpFile create(Path path) {
		FfpFile result;
		
		result = new FfpFile();
		result.setPath(path);
		
		return result;
	}
	
	/**
	 * Delete all the files of the item.
	 * 
	 * @param item
	 *            the item to delete.
	 */
	public void delete(FfpItem item) {
		// We cannot delete when a MappedByteBuffer is still in memory (event if
		// nobody reference/use it anymore)
		System.gc();
		// Start the deletion
		List<FfpFile> folders = new ArrayList<>();
		item.getFfpFiles().parallelStream().forEach((ffpFile) -> {
			try {
				if (Files.isRegularFile(ffpFile.getPath())) {
					Files.delete(ffpFile.getPath());
				} else {
					// We need to delete folder when they are empty
				folders.add(ffpFile);
			}
		} catch (IOException e) {
			LOGGER.error("Cannot delete item.", e);
		}
		/**/});
		folders.forEach((ffpFile) -> {
			try {
				Files.delete(ffpFile.getPath());
			} catch (IOException e) {
				LOGGER.error("Cannot delete item.", e);
			}
		});
	}

}
