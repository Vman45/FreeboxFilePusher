package eu.gaki.ffp.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Objet principal.
 */
public class FilePusher {
    
    /**
     * List of filesList managed by the system.
     */
    private List<FilesList> fileManaged = new ArrayList<>();
    
    public void setFileManaged (List<FilesList> fileManaged) {
        this.fileManaged = fileManaged;
    }    
    public List<FilesList> getFileManaged() {
        return this.fileManaged;
    }    
    
}
