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
	private static final Logger LOGGER = LoggerFactory.getLogger(ItemService.class);

	/** The file service. */
	private final FileService fileService;

	/**
	 * Instantiates a new item service.
	 *
	 * @param fileService
	 *            the file service
	 */
	public ItemService(final FileService fileService) {
		this.fileService = fileService;
	}

	/**
	 * Update the item.
	 *
	 * @param item
	 *            the FfpItem
	 * @return true if the item have changed (ex: new files)
	 */
	public boolean update(final FfpItem item) {
		final AtomicBoolean result = new AtomicBoolean(false);

		final List<FfpFile> ffpFiles = new ArrayList<>(item.getFfpFiles());
		// Delete no more existing file
		ffpFiles.forEach((file) -> {
			final Path path = file.getPath();
			final boolean exist = Files.exists(path);
			if (!exist) {
				item.removeFile(file);
				result.set(true);
			}
		});
		// Add new file/directory
		final List<Path> newDirectories = new ArrayList<Path>();
		ffpFiles.forEach((file) -> {
			final Path path = file.getPath();
			if (Files.isDirectory(path)) {
				try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
					for (final Path pathInDirectory : directoryStream) {
						final List<FfpFile> contains = item.contains(pathInDirectory);
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
				} catch (final IOException e) {
					LOGGER.error("Cannot list folder: " + path, e);
				}
			}
		});
		// Scan new directory
		newDirectories.forEach((directory) -> recursiveScanPath(directory, item));
		final boolean b = result.get();
		LOGGER.trace("Update the item {}. Result have change: {}", item, b);
		return b;
	}

	/**
	 * Creates the FfpItem.
	 *
	 * @param path
	 *            the path
	 * @return the ffp item
	 */
	public FfpItem create(final Path path) {
		FfpItem result;
		result = new FfpItem();
		result.setStatus(StatusEnum.WATCH);
		recursiveScanPath(path, result);
		LOGGER.info("Create the item {}", result);
		return result;
	}

	/**
	 * Scan a path and add all path to the {@link FfpItem}.
	 *
	 * @param path
	 *            The directory or the file to scan.
	 * @param item
	 *            The {@link FfpItem} to fill.
	 */
	private void recursiveScanPath(final Path path, final FfpItem item) {
		try {
			Files.walkFileTree(path, new FileVisitor<Path>() {

				@Override
				public FileVisitResult preVisitDirectory(final Path dir, final BasicFileAttributes attrs)
						throws IOException {
					item.addFile(fileService.create(dir));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
					item.addFile(fileService.create(file));
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult visitFileFailed(final Path file, final IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (final IOException e) {
			LOGGER.error("Cannot create Item for folder: " + path, e);
		}
	}

	/**
	 * Delete all the files of the item.
	 *
	 * @param item
	 *            the item to delete.
	 */
	public void delete(final FfpItem item) {
		// Start the deletion
		final List<FfpFile> folders = new ArrayList<>();
		item.getFfpFiles().parallelStream().forEach((ffpFile) -> {
			try {
				if (Files.isRegularFile(ffpFile.getPath())) {
					Files.delete(ffpFile.getPath());
				} else {
					// We need to delete folder when they are empty
					folders.add(ffpFile);
					/**/}
				/**/} catch (final IOException e) {
				/**/LOGGER.error("Cannot delete item.", e);
				/**/}
			/**/});
		folders.forEach((ffpFile) -> {
			try {
				Files.delete(ffpFile.getPath());
			} catch (final IOException e) {
				LOGGER.error("Cannot delete item.", e);
			}
		});
	}

}
