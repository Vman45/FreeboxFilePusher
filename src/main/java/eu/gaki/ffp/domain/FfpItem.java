package eu.gaki.ffp.domain;

import java.util.ArrayList;
import java.util.List;

// TODO: Auto-generated Javadoc
/**
 * Represent a set of ffpFiles to be sended in the same time.
 */
public class FfpItem {

    /** List of files. */
    private List<FfpFile> ffpFiles = new ArrayList<>();

    /**
     * Status of this pack of ffpFile.
     */
    private StatusEnum status;

    /**
     * Sets the ffp files.
     *
     * @param ffpFiles
     *            the new ffp files
     */
    public void setffpFiles(List<FfpFile> ffpFiles) {
	this.ffpFiles = ffpFiles;
    }

    /**
     * Gets the ffp files.
     *
     * @return the ffp files
     */
    public List<FfpFile> getFfpFiles() {
	return this.ffpFiles;
    }

    /**
     * Adds the file.
     *
     * @param file
     *            the file
     */
    public void addFile(FfpFile file) {
	ffpFiles.add(file);
    }

    /**
     * Sets the status.
     *
     * @param status
     *            the new status
     */
    public void setStatus(StatusEnum status) {
	this.status = status;
    }

    /**
     * Gets the status.
     *
     * @return the status
     */
    public StatusEnum getStatus() {
	return this.status;
    }

}
