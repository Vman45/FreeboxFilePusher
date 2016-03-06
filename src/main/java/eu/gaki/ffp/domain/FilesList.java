package eu.gaki.ffp.domain;

import java.util.List;

/**
 * Represent a set of files to be sended in the same time.
 */
public class FilesList {
    
    /**
     * List of files
     */
    private List<FfpFile> files;
    
    public void setFiles (List<FfpFile> files) {
        this.files = files;
    }    
    public List<FfpFile> getFiles() {
        return this.files;
    }
}
