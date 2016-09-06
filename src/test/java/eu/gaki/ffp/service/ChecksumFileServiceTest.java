/*
 *
 */
package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import eu.gaki.ffp.CreationUtil;
import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;

public class ChecksumFileServiceTest {

	private final FileService fs = new FileService();
	private final ChecksumService cfs = new ChecksumService();
	private final ItemService is = new ItemService(fs);

	@Test
	public void computeChecksumFfpFile() {

		// Creation d'un fichier de test
		// Compute it checksum
		final FfpFile file = CreationUtil.createFfpFile();

		final boolean result1 = cfs.computeChecksum(file, 4096);
		final boolean result2 = cfs.computeChecksum(file, 4096);
		final boolean result3 = cfs.computeChecksum(file, 1024);

		Assert.assertTrue(result1);
		Assert.assertFalse(result2);
		Assert.assertTrue(result3);

		// Modification du fichier de test
		try (FileChannel fc = FileChannel.open(file.getPath(), StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			final Random rdm = new Random();
			final byte[] bytes = new byte[4096];
			final ByteBuffer buff = ByteBuffer.allocate(bytes.length);
			for (int i = 0; i < 10; i++) {
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

		final boolean result4 = cfs.computeChecksum(file, 1024);
		Assert.assertTrue(result4);

		Assert.assertTrue("No adler32 computed", file.getAdler32().size() > 0);
	}

	@Test
	public void computeChecksumFfpItem() {
		final FfpItem item = CreationUtil.createFfpSingleItem();
		final boolean result1 = cfs.computeChecksum(item);
		final boolean result2 = cfs.computeChecksum(item);
		Assert.assertTrue(result1);
		Assert.assertFalse(result2);

		// Modification du fichier de test
		final FfpFile next = item.getFfpFiles().iterator().next();
		try (FileChannel fc = FileChannel.open(next.getPath(), StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
			final Random rdm = new Random();
			final byte[] bytes = new byte[4096];
			final ByteBuffer buff = ByteBuffer.allocate(bytes.length);
			for (int i = 0; i < 10; i++) {
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

		final boolean result4 = cfs.computeChecksum(item);
		Assert.assertTrue(result4);

		final LocalDateTime adler32Date = item.getAdler32Date();
		Assert.assertNotSame(LocalDateTime.MIN, adler32Date);

		is.delete(item);
	}

}
