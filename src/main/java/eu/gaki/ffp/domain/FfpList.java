package eu.gaki.ffp.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Represent a set of ffpFiles to be sended in the same time.
 */
public class FfpList {
    
    /**
     * List of files
     */
    private List<FfpFile> ffpFiles = new ArrayList<>();
    
    /**
     * Status of this pack of ffpFile.
     */
    private StatusEnum status;
    
    public void setffpFiles (List<FfpFile> ffpFiles) {
        this.ffpFiles = ffpFiles;
    }    
    public List<FfpFile> getFfpFiles() {
        return this.ffpFiles;
    }
    
    public void setStatus (StatusEnum status) {
        this.status = status;
    }    
    public StatusEnum getStatus() {
        return this.status;
    }    
    
}
