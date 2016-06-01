/*
 * 
 */
package eu.gaki.ffp.service;

import org.junit.Test;

import eu.gaki.ffp.CreationUtil;
import eu.gaki.ffp.domain.FfpFile;
import eu.gaki.ffp.domain.FfpItem;

public class ItemServiceTest {

	private final ItemService is = new ItemService();

	@Test
	public void delete() {
		FfpItem item = CreationUtil.createFfpItem();

		{
			FfpFile folder = CreationUtil.createFfpFolder();
			FfpFile file01 = CreationUtil.createFfpFile(folder.getPath());
			FfpFile file02 = CreationUtil.createFfpFile(folder.getPath());
			FfpFile file03 = CreationUtil.createFfpFile(folder.getPath());
			item.addFile(folder);
			item.addFile(file01);
			item.addFile(file02);
			item.addFile(file03);
		}

		{
			FfpFile folder = CreationUtil.createFfpFolder();
			FfpFile file01 = CreationUtil.createFfpFile(folder.getPath());
			FfpFile file02 = CreationUtil.createFfpFile(folder.getPath());
			item.addFile(folder);
			item.addFile(file01);
			item.addFile(file02);
		}

		{
			FfpFile folder = CreationUtil.createFfpFolder();
			item.addFile(folder);
		}

		is.delete(item);
	}

}
