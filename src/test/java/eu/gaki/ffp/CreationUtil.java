package eu.gaki.ffp;

import static java.security.AccessController.doPrivileged;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.junit.Assert;

import sun.security.action.GetPropertyAction;
import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;

/**
 * The Class CreationUtil.
 */
public class CreationUtil {
	
	/**
	 * Creates the ffp item.
	 *
	 * @return the ffp item
	 */
	public static FfpItem createFfpItem() {
		FfpItem item = new FfpItem();

		int nbFile = ThreadLocalRandom.current().nextInt(2, 2 + 1);
		for (int i = 0; i < nbFile; i++) {
			item.addFile(createFfpFile());
		}

		return item;
	}
	
	/**
	 * Create a random FfpFile.
	 *
	 * @return the ffp file
	 */
	public static FfpFile createFfpFolder() {
		FfpFile file = null;
		try {
			final Path tempFile = Files.createTempDirectory("test-");
			tempFile.toFile().deleteOnExit();
			file = new FfpFile();
			file.setPathUri(tempFile.toUri());
		} catch (final IOException e) {
			System.err.format("IOException: %s%n", e);
			Assert.assertTrue(false);
		}
		return file;
	}
	
	/**
	 * Create a random FfpFile.
	 * 
	 * @return the ffp file
	 */
	@SuppressWarnings("restriction")
	public static FfpFile createFfpFile() {
		return createFfpFile(Paths.get(doPrivileged(new GetPropertyAction("java.io.tmpdir"))));
	}

	/**
	 * Create a random FfpFile.
	 *
	 * @param dir the dir
	 * @return the ffp file
	 */
	public static FfpFile createFfpFile(Path dir) {
		FfpFile file = null;
		try {
			final Path tempFile = Files.createTempFile(dir, "test-", ".tmp");
			tempFile.toFile().deleteOnExit();
			try (FileChannel fc = FileChannel.open(tempFile,
					StandardOpenOption.WRITE)) {
				final Random rdm = new Random();
				final byte[] bytes = new byte[4096];
				final ByteBuffer buff = ByteBuffer.allocate(bytes.length);
				for (int i = 0; i < ThreadLocalRandom.current().nextInt(1,
						20 + 1); i++) {
					rdm.nextBytes(bytes);
					buff.put(bytes);
					buff.flip();
					fc.write(buff);
					buff.clear();
				}

			} catch (final IOException e) {
				System.err.format("IOException: %s%n", e);
				Assert.assertTrue(false);
			}
			file = new FfpFile();
			file.setPathUri(tempFile.toUri());
		} catch (final IOException e) {
			System.err.format("IOException: %s%n", e);
			Assert.assertTrue(false);
		}

		return file;
	}
	
}
