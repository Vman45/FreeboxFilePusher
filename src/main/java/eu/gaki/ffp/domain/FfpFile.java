package eu.gaki.ffp.domain;

import java.nio.file.Path;
import java.util.Date;
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
    private Date adler32Date = null;
   
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
    
    public void setAdler32Date (Date adler32Date) {
        this.adler32Date = adler32Date;
    }    
    public Date getAdler32Date() {
        return this.adler32Date;
    }    
        
    public void setPath (Path path) {
        this.path = path;
    }    
    public Path getPath() {
        return this.path;
    }
    
    public void addAdler32 (Long byteNumber, Long checksome) {
        this.adler32.put(byteNumber, checksome);
    }    
    public Map<Long ,Long> getAdler32() {
        return this.adler32;
    }
    
    public void setSize (Long size) {
        this.size = size;
    }    
    public Long getSize() {
        return this.size;
    }
    
}
