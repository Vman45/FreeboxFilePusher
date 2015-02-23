package eu.gaki.ffp.compress;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.xz.XZCompressorOutputStream;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.http.HttpFolderListener;

/**
 * The Class FilesCompresor.
 */
public class FilesCompresor {

	/** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(HttpFolderListener.class);

	/** The Constant TXZ. */
	private static final String TXZ = ".txz";

	/** The configuration. */
	private final Properties configuration;

	/**
	 * Instantiates a new files compresor.
	 *
	 * @param configuration
	 *            the configuration
	 */
	public FilesCompresor(final Properties configuration) {
		this.configuration = configuration;
	}

	/**
	 * Compress.
	 *
	 * @param pathToCompress
	 *            the path or file to compress
	 * @return the path the Tar XZ file
	 */
	public Path compress(final Path pathToCompress) {
		final Collection<String> excludeExtention = getExcludeExtensions();

		final Instant startDate = Instant.now();
		LOGGER.info("Start compress: " + pathToCompress);

		// Tar
		Stream<Path> walkFiltered = null;
		final Path tarXzFile = computeTarXzName(pathToCompress);
		try (final OutputStream tarXzOutputStream = Files.newOutputStream(tarXzFile, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
				final OutputStream tarXzBufferedOutputStream = new BufferedOutputStream(tarXzOutputStream);
				final OutputStream xzOutputStream = new XZCompressorOutputStream(tarXzBufferedOutputStream, 0);
				final TarArchiveOutputStream tarOutputStream = new TarArchiveOutputStream(xzOutputStream);
				Stream<Path> walk = Files.walk(pathToCompress, FileVisitOption.FOLLOW_LINKS);) {

			tarOutputStream.setLongFileMode(TarArchiveOutputStream.LONGFILE_POSIX);
			tarOutputStream.setBigNumberMode(TarArchiveOutputStream.BIGNUMBER_POSIX);

			// final String rootPath = pathToCompress.toString() +
			// File.pathSeparator;
			walkFiltered = walk.filter(p -> {
				final String name = p.getFileName().toString();
				final String extension = FilenameUtils.getExtension(name);
				boolean include = false;

				if (Files.isDirectory(p)) {
					// Exclude the root folder to compress (we tar sub path of
					// the root folder)
					include = !p.equals(pathToCompress);
				} else {
					// Exclude some file extention
					include = !excludeExtention.contains(extension);
				}

				return include;
			});

			walkFiltered.forEach(t -> {
				try {
					// Add the file to the TAR
					// final String relativePath =
					// t.toString().replace(rootPath, "");
					final Path relativePath = pathToCompress.relativize(t);
					final TarArchiveEntry entry = new TarArchiveEntry(t.toFile(), relativePath.normalize().toString());
					tarOutputStream.putArchiveEntry(entry);

					if (!Files.isDirectory(t)) {
						// Copy the file
						Files.copy(t, tarOutputStream);
					}

					tarOutputStream.closeArchiveEntry();
				} catch (final IOException e) {
					LOGGER.error(e.getMessage(), e);
				}
			});

		} catch (final IOException e) {
			LOGGER.error(e.getMessage(), e);
		} finally {
			if (walkFiltered != null) {
				walkFiltered.close();
			}
		}

		final Instant endDate = Instant.now();
		final long between = ChronoUnit.SECONDS.between(startDate, endDate);

		LOGGER.info("Stop compress: " + pathToCompress + " took " + between + " secondes");

		// XZ the tar
		return tarXzFile;
	}

	/**
	 * Gets the extentions to exclude.
	 *
	 * @return the extentions to exclude
	 */
	private Collection<String> getExcludeExtensions() {
		final Collection<String> excludeExtensions = new HashSet<>();
		final String excludeExtensionString = configuration.getProperty("fft.exclude.extension", ".html,.exe,.txt,.readme,.nfo,.link");
		final String[] split = excludeExtensionString.split(",");
		excludeExtensions.addAll(Arrays.asList(split));
		return excludeExtensions;
	}

	/**
	 * Compute tar xz name.
	 *
	 * @param pathToCompress
	 *            the path to compress
	 * @return the path
	 */
	private Path computeTarXzName(final Path pathToCompress) {
		final Path tarXzFile = pathToCompress.getParent().resolve(pathToCompress.getFileName() + TXZ);
		return tarXzFile;
	}

}
