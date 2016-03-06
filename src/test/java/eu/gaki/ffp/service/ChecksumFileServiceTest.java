package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import eu.gaki.ffp.domain.FfpFile;

public class ChecksumFileServiceTest {

    private ChecksumFileService cfs = new ChecksumFileService();

    @Test
    public void computeChecksum() {

        try {
            // Create a ramdom file
            Random rdm = new Random();
            byte[] bytes = new byte[4096];
            Path tempFile = Files.createTempFile("test-", ".tmp");
            tempFile.toFile().deleteOnExit();
            // Put randow content on file
            try (SeekableByteChannel writer =
                                              Files.newByteChannel(tempFile, StandardOpenOption.WRITE, StandardOpenOption.APPEND)) {
                ByteBuffer buff = ByteBuffer.allocate(bytes.length);
                for(int i = 0; i<200; i++){
                    rdm.nextBytes(bytes);
                    buff.put(bytes);
                    int writed = writer.write(buff);
                    buff.clear();
                }
            } catch (IOException e) {
                System.err.format("IOException: %s%n", e);
                Assert.assertTrue(false);
            }
            
            // Compute it checksum
            FfpFile file = new FfpFile();
            file.setPath(tempFile);
            
            cfs.computeChecksum(file, 25);
            
             Map<Long ,Long> adler32 = file.getAdler32();
             
             Assert.assertTrue("No adler32 computed", adler32.size() > 0);             
        } catch (IOException e) {
            System.err.format("IOException: %s%n", e);
            Assert.assertTrue(false);
        }
    }

}
