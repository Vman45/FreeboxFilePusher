package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import eu.gaki.ffp.domain.FfpFile;

public class ChecksumFileServiceTest {

    private ChecksumFileService cfs = new ChecksumFileService();

    @Test
    public void computeChecksum() {

        // Creation d'un fichier de test
        try {
            Path tempFile = Files.createTempFile("test-", ".tmp");
            tempFile.toFile().deleteOnExit();

            try (FileChannel fc = FileChannel.open(tempFile, StandardOpenOption.WRITE)) {
                Random rdm = new Random();
                byte[] bytes = new byte[4096];
                ByteBuffer buff = ByteBuffer.allocate(bytes.length);
                for (int i = 0; i < 10; i++) {
                    rdm.nextBytes(bytes);
                    buff.put(bytes);
                    buff.flip();
                    fc.write(buff);
                    buff.clear();
                }

            } catch (IOException e) {
                System.err.format("IOException: %s%n", e);
                Assert.assertTrue(false);
            }

            // Compute it checksum
            FfpFile file = new FfpFile();
            file.setPath(tempFile);

            boolean result1 = cfs.computeChecksum(file, 4096);
            boolean result2 = cfs.computeChecksum(file, 4096);
            boolean result3 = cfs.computeChecksum(file, 1024);

            Assert.assertTrue(result1);
            Assert.assertFalse(result2);
            Assert.assertTrue(result3);

            // Modification du fichier de test
            try (FileChannel fc = FileChannel.open(tempFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                Random rdm = new Random();
                byte[] bytes = new byte[4096];
                ByteBuffer buff = ByteBuffer.allocate(bytes.length);
                for (int i = 0; i < 10; i++) {
                    rdm.nextBytes(bytes);
                    buff.put(bytes);
                    buff.flip();
                    fc.write(buff);
                    buff.clear();
                }

            } catch (IOException e) {
                System.err.format("IOException: %s%n", e);
                Assert.assertTrue(false);
            }

            boolean result4 = cfs.computeChecksum(file, 1024);
            Assert.assertTrue(result4);

            Assert.assertTrue("No adler32 computed", file.getAdler32().size() > 0);
            
            // toto
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
            Assert.assertTrue(false);
        }
    }

}
