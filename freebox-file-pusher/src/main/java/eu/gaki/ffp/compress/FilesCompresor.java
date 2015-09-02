package eu.gaki.ffp.compress;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Stream;

import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.commons.io.FileUtils;
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

	/** The Constant ZIP. */
	private static final String ZIP_EXTENSION = ".zip";

	/** The configuration. */
	private final Properties configuration;
	
	/** The executor: not thread safe. */
	private Executor executor = Executors.newSingleThreadExecutor();

	/** The in progress. */
	private final Set<Path> inProgress = new HashSet<Path>();
	
	/**
	 * Instantiates a new files compressor.
	 *
	 * @param configuration
	 *            the configuration
	 */
	public FilesCompresor(final Properties configuration) {
		this.configuration = configuration;
	}
	
	public synchronized boolean isInProgress(final Path pathToCompress) {
		return inProgress.contains(pathToCompress);
	}

	/**
	 * Compress.
	 *
	 * @param pathToCompress
	 *            the path or file to compress
	 */
	public synchronized void compress(final Path pathToCompress) {

		if (!isInProgress(pathToCompress)) {
			inProgress.add(pathToCompress);
			executor.execute(() -> doCompress(pathToCompress));
		} 
		
	}
	
	/**
	 * Do the compression.
	 *
	 * @param pathToCompress
	 *            the path to compress
	 * @return the path
	 */
	private void doCompress(final Path pathToCompress) {
			LOGGER.info("Start compress: " + pathToCompress);
			final Instant startDate = Instant.now();
			String compressionMethodString = configuration.getProperty("ffp.compress.method", "8");
			int compressionMethod = Integer.valueOf(compressionMethodString);
			String compressionLevelString = configuration.getProperty("ffp.compress.level", "0");
			int compressionLevel = Integer.valueOf(compressionLevelString);
			Stream<Path> walkFiltered = null;
			final Path zipFile = computeCompressFileName(pathToCompress);
			try (ZipArchiveOutputStream zipOutputStream = new ZipArchiveOutputStream(
					zipFile.toFile());
					Stream<Path> walk = Files.walk(pathToCompress,
							FileVisitOption.FOLLOW_LINKS);) {

				zipOutputStream.setMethod(compressionMethod);
				zipOutputStream.setLevel(compressionLevel);
				zipOutputStream.setEncoding("UTF-8");
				zipOutputStream
						.setCreateUnicodeExtraFields(ZipArchiveOutputStream.UnicodeExtraFieldPolicy.ALWAYS);

				final Collection<String> excludeExtention = getExcludeExtensions();

				walkFiltered = walk.filter(p -> {
					boolean include = false;
					
					
					if (Files.isDirectory(p)) {
						// Exclude the root folder to compress (we tar sub path of
						// the root folder)
						include = !p.equals(pathToCompress);
					} else {
						// Exclude some file extension
						final String name = p.getFileName().toString();
						final String extension = FilenameUtils.getExtension(name);
						include = !excludeExtention.contains(extension);
					}

					return include;
				});

				walkFiltered
						.forEach(t -> {
							try {
								// Write the Zip header
								final Path relativePath = pathToCompress.relativize(t);
								final ZipArchiveEntry entry = new ZipArchiveEntry(t.toFile(), relativePath.normalize().toString());
								entry.setMethod(compressionMethod);
								zipOutputStream.putArchiveEntry(entry);
								
								// Write the stream
								if (!Files.isDirectory(t)) {
									try (InputStream fileStream = Files.newInputStream(t);) {
										 IOUtils.copy(fileStream, zipOutputStream);
									} catch (final IOException e) {
										LOGGER.error(e.getMessage(), e);
									}
								}
								
								zipOutputStream.closeArchiveEntry();
								
							} catch (IOException e) {
								LOGGER.error(e.getMessage(), e);
							}
							
						});

				// We have finish
				FileUtils.deleteDirectory(pathToCompress.toFile());
				
			} catch (IOException e) {
				LOGGER.error(e.getMessage(), e);
			} finally {
				if (walkFiltered != null) {
					walkFiltered.close();
				}
			}
			
			synchronized (FilesCompresor.this) {
				inProgress.remove(pathToCompress);
			}
			
			final Instant endDate = Instant.now();
			final long between = ChronoUnit.SECONDS.between(startDate, endDate);
			LOGGER.info("Stop compress: " + pathToCompress + " took " + between
					+ " secondes");
	}
	
	/**
	 * Gets the extensions to exclude.
	 *
	 * @return the extensions to exclude
	 */
	private Collection<String> getExcludeExtensions() {
		final Collection<String> excludeExtensions = new HashSet<>();
		final String excludeExtensionString = configuration.getProperty("fft.exclude.extension", ".html,.exe,.txt,.readme,.nfo,.link");
		final String[] split = excludeExtensionString.split(",");
		excludeExtensions.addAll(Arrays.asList(split));
		return excludeExtensions;
	}

	/**
	 * Compute zip name.
	 *
	 * @param pathToCompress
	 *            the path to compress
	 * @return the path
	 */
	public Path computeCompressFileName(final Path pathToCompress) {
		Path compressFileName = null;
		Path parent = pathToCompress.getParent();
		if (pathToCompress != null && parent != null) {
			compressFileName = parent.resolve(""+pathToCompress.getFileName() + ZIP_EXTENSION);
		}
		return compressFileName;
	}

}
