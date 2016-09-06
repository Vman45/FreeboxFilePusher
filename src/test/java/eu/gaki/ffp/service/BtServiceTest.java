/*
 *
 */
package eu.gaki.ffp.service;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import eu.gaki.ffp.CreationUtil;
import eu.gaki.ffp.domain.FfpItem;

public class BtServiceTest {

	private final ConfigService cs;
	private final BtService btService;
	private final DaoService dao;

	public BtServiceTest() throws IOException {
		cs = new ConfigService(Paths.get("src/test/resources/eu/gaki/ffp/freeboxFilePusher.properties"));
		dao = new DaoService(cs);
		btService = new BtService(cs, dao);
	}

	@Test
	public void shareTorrent()
			throws FileNotFoundException, NoSuchAlgorithmException, IOException, InterruptedException {
		final FfpItem item = CreationUtil.createFfpFolderItem();
		btService.startSharing(item);
		Thread.currentThread().wait();
	}

}
