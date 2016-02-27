package eu.gaki.ffp.domain;

import java.util.List;

/**
 * Represent a set of files to be sended in the same time.
 */
public class FilesList {
    
    /**
     * List of files
     */
    private List<File> files;
    
    public void setFiles (List<File> files) {
        this.files = files;
    }    
    public List<File> getFiles() {
        return this.files;
    }
}
