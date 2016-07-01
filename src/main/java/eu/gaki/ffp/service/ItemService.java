/*
 * 
 */
package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;
import eu.gaki.ffp.domain.StatusEnum;

/**
 * The Class ItemService.
 */
public class ItemService {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ItemService.class);

	/** The file service. */
	private FileService fileService;

	/**
	 * Instantiates a new item service.
	 *
	 * @param fileService
	 *            the file service
	 */
	public ItemService(FileService fileService) {
		this.fileService = fileService;
	}

	/**
	 * Update the item.
	 *
	 * @param item
	 *            the FfpItem
	 * @return true if the item have changed (ex: new files)
	 */
	public boolean update(FfpItem item) {
		final AtomicBoolean result = new AtomicBoolean(false);

		List<FfpFile> ffpFiles = new ArrayList<>(item.getFfpFiles());
		// Delete no more existing file
		ffpFiles.forEach((file) -> {
			Path path = file.getPath();
			boolean exist = Files.exists(path);
			if (!exist) {
				item.removeFile(file);
				result.set(true);
			}
		});
		// Add new file/directory
		List<Path> newDirectories = new ArrayList<Path>();
		ffpFiles.forEach((file) -> {
			Path path = file.getPath();
			if (Files.isDirectory(path)) {
				try (DirectoryStream<Path> directoryStream = Files
						.newDirectoryStream(path)) {
					for (Path pathInDirectory : directoryStream) {
						List<FfpFile> contains = item.contains(pathInDirectory);
						if (contains.isEmpty()) {
							// New file or directory
							item.addFile(fileService.create(pathInDirectory));
							// Track new directory
							if (Files.isDirectory(pathInDirectory)) {
								newDirectories.add(pathInDirectory);
							}
							result.set(true);
						}
					}
				} catch (IOException e) {
					LOGGER.error("Cannot list folder: " + path, e);
				}
			}
		});
		// Scan new directory
		newDirectories.forEach((directory) -> scanDirectory(directory, item));

		return result.get();
	}

	/**
	 * Creates the.
	 *
	 * @param path
	 *            the path
	 * @return the ffp item
	 */
	public FfpItem create(Path path) {
		FfpItem result;

		result = new FfpItem();
		result.setStatus(StatusEnum.WATCH);

		result.addFile(fileService.create(path));

		// It's a folder, we include all contain
		scanDirectory(path, result);

		return result;
	}

	/**
	 * Scan directory and add all path to the {@link FfpItem}.
	 * 
	 * @param path
	 *            The directory to scan.
	 * @param item
	 *            The {@link FfpItem} to fill.
	 */
	private void scanDirectory(Path path, FfpItem item) {
		if (Files.isDirectory(path)) {
			try {
				Files.walkFileTree(path, new FileVisitor<Path>() {

					@Override
					public FileVisitResult preVisitDirectory(Path dir,
							BasicFileAttributes attrs) throws IOException {
						item.addFile(fileService.create(dir));
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file,
							BasicFileAttributes attrs) throws IOException {
						item.addFile(fileService.create(file));
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file,
							IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult postVisitDirectory(Path dir,
							IOException exc) throws IOException {
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				LOGGER.error("Cannot create Item for folder: " + path, e);
			}
		}
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
				/**/}
			/**/} catch (IOException e) {
			/**/LOGGER.error("Cannot delete item.", e);
			/**/}
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
