package eu.gaki.ffp.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Objet principal.
 */
public class FilePusher {
    
    /**
     * List of filesList to watch by the system.
     */
    private List<FfpList> ffpListWatched = new ArrayList<>();
   
    public void setFfpListWatched (List<FfpList> ffpListWatched) {
        this.ffpListWatched = ffpListWatched;
    }    
    public List<FfpList> getFfpListWatched() {
        return this.ffpListWatched;
    }    
    
}
