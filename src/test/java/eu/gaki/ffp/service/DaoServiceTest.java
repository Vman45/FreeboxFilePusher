/*
 *
 */
package eu.gaki.ffp.service;

import java.io.IOException;
import java.nio.file.Paths;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;
import eu.gaki.ffp.domain.FilePusher;
import eu.gaki.ffp.domain.StatusEnum;

/**
 * The Class DaoServiceTest.
 */
public class DaoServiceTest {
	
	/** The cs. */
	private ConfigService cs;
    
	/** The dao. */
    private DaoService dao;

    public DaoServiceTest() throws IOException {
    	cs = new ConfigService(Paths.get("src/test/resources/eu/gaki/ffp/freeboxFilePusher.properties"));
    	dao = new DaoService(cs);
	}
    
    /**
     * Before.
     */
    @Before
    public void before() {
	dao.clear();
    }

    /**
     * Save and get.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void saveAndGet() throws IOException {

	final FilePusher filePusher = dao.get();

	// Create Item 1
	final FfpItem ffpItem1 = new FfpItem();
	ffpItem1.setStatus(StatusEnum.ARCHIVED);
	final FfpFile file = new FfpFile();
	file.setPathUri(Paths.get("toto/docteur01").toUri());
//	file.setSize(42L);
	ffpItem1.addFile(file);

	// Create Item 2
	final FfpItem ffpItem2 = new FfpItem();
	ffpItem2.setStatus(StatusEnum.ARCHIVED);

	// Create item 3
	final FfpItem ffpItem3 = new FfpItem();
	ffpItem3.setStatus(StatusEnum.TO_SEND);

	filePusher.addFfpItem(ffpItem1);
	filePusher.addFfpItem(ffpItem2);
	filePusher.addFfpItem(ffpItem3);
	dao.save();

	final DaoService dao2 = new DaoService(cs);

	Assert.assertEquals(3, dao2.getByStatus(null).size());
	Assert.assertEquals(2, dao2.getByStatus(StatusEnum.ARCHIVED).size());
	Assert.assertEquals(1, dao2.getByStatus(StatusEnum.TO_SEND).size());
	Assert.assertEquals(0, dao2.getByStatus(StatusEnum.SENDING).size());

    }

    /**
     * Save and get.
     *
     * @throws IOException
     *             Signals that an I/O exception has occurred.
     */
    @Test
    public void contains() throws IOException {

	final FilePusher filePusher = dao.get();

	// Create Item 1
	final FfpItem ffpItem1 = new FfpItem();
	ffpItem1.setStatus(StatusEnum.ARCHIVED);
	final FfpFile file = new FfpFile();
	file.setPathUri(Paths.get("toto/docteur01").toUri());
//	file.setSize(42L);
	ffpItem1.addFile(file);

	// Create Item 2
	final FfpItem ffpItem2 = new FfpItem();
	ffpItem2.setStatus(StatusEnum.ARCHIVED);

	// Create item 3
	final FfpItem ffpItem3 = new FfpItem();
	ffpItem3.setStatus(StatusEnum.TO_SEND);
	final FfpFile file3 = new FfpFile();
	file3.setPathUri(Paths.get("toto/docteur02").toUri());
//	file3.setSize(42L);
	ffpItem3.addFile(file3);

	filePusher.addFfpItem(ffpItem1);
	filePusher.addFfpItem(ffpItem2);
	filePusher.addFfpItem(ffpItem3);

	Assert.assertEquals(1, dao.contains(Paths.get("toto/docteur02").toUri()).size());
	Assert.assertEquals(1, dao.contains(Paths.get("toto/docteur01").toUri()).size());
    }
}
