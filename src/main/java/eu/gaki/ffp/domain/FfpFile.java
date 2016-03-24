package eu.gaki.ffp.domain;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * File
 */
public class FfpFile {
    
    /**
     * The file path
     */
    private Path path = null;
    
    /**
     * The date when we compute the checksome adler32 for this file.
     */
    private LocalDateTime adler32Date = null;
   
    /**
     * Map for store checksome : Bytes number => Checksome value.<br>
     * 20000 => Checksome for byte 0 to byte 20000<br>
     * 40000 => Checksome for byte 20001 to byte 40000<br>
     */
    private Map<Long ,Long> adler32 = new HashMap<>();
    
    /**
     * Size of file.
     */
    private Long size;
    
    public void setAdler32Date (LocalDateTime adler32Date) {
        this.adler32Date = adler32Date;
    }    
    public LocalDateTime getAdler32Date() {
        return this.adler32Date;
    }    
        
    public void setPath (Path path) {
        this.path = path;
    }    
    public Path getPath() {
        return this.path;
    }
       
    public Map<Long ,Long> getAdler32() {
        return this.adler32;
    }
    public void setAdler32( Map<Long ,Long> adler32) {
        this.adler32 = adler32;
    }
    
    public void setSize (Long size) {
        this.size = size;
    }    
    public Long getSize() {
        return this.size;
    }
    
}
