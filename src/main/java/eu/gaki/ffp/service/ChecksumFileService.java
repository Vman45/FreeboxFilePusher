package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.Adler32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.gaki.ffp.domain.FfpFile;

/**
 * CheckedInputStream
 */
public class ChecksumFileService {
    
    private static final int CHUNK_SIZE = 100000;
    
    /** The Constant LOGGER. */
	private static final Logger LOGGER = LoggerFactory.getLogger(ChecksumFileService.class);

    /**
     * Compute the checksun of the file.
     * 
     * @param file The file.
     * @return true if the checksum have changed
     */
    public boolean computeChecksum(FfpFile file) {
        return this.computeChecksum(file, CHUNK_SIZE);
    }
    
    /**
     * Compute the checksun of the file.
     * 
     * @param file The file.
     * @param chunkSize The size of each chunk
     * @return true if the checksum have changed
     */
    public boolean computeChecksum(FfpFile file, int chunkSize) {
        boolean result = false;
        
        if (file != null && file.getPath() != null) {
            Path path = file.getPath();
            if(Files.isRegularFile(path)) {
                try {
                //RandomAccessFile aFile = new RandomAccessFile("test.txt", "r");
                //FileChannel inChannel = aFile.getChannel();
                FileChannel inChannel = FileChannel.open(path, StandardOpenOption.READ);                               
                MappedByteBuffer buffer = inChannel.map(FileChannel.MapMode.READ_ONLY, 0, inChannel.size());
                buffer.load();  
                while (buffer.remaining() > 0)
                {
                    byte[] readed;
                    if (buffer.remaining() >= CHUNK_SIZE) {
                        readed = new byte[CHUNK_SIZE];
                    } else {
                        readed = new byte[buffer.remaining()];
                    }
                    buffer.get(readed);
                    Adler32 adler32 = new Adler32();
                    adler32.update(readed);
                    Long position = Long.valueOf(buffer.position());
                    Long newChecksum = adler32.getValue();
                    Long oldChecksum = file.getAdler32().get(position);
                    if (oldChecksum == null || oldChecksum != newChecksum) {
                        result = true;
                        LOGGER.info("Le checksum de {"+path+"} a changé.");
                    }    
                    file.addAdler32(position, newChecksum);
                }
                buffer.clear(); // do something with the data and clear/compact it.
                inChannel.close();
                //aFile.close();
                } catch (IOException e) {
                    LOGGER.error("Impossible de calculer le checksum de {"+path+"}", e);
                }
            }    
        }
        return result;
    }
    
}
