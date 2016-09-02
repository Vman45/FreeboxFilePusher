/*
 *
 */
package eu.gaki.ffp.runnable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.Assert;
import org.junit.Test;

import eu.gaki.ffp.domain.FfpItem;
import eu.gaki.ffp.domain.FilePusher;
import eu.gaki.ffp.domain.StatusEnum;
import eu.gaki.ffp.service.ServiceProvider;

public class FolderWatcherRunnableTest {

	@Test
	public void createConfigService() throws IOException, InterruptedException {

		final ServiceProvider sp = new ServiceProvider("src/test/resources/eu/gaki/ffp/freeboxFilePusher.properties");
		sp.getDaoService().clear();
		sp.getDaoService().save();

		final Path watchedFolder = Paths.get("target/to-send");
		final FolderWatcherRunnable folderWatcherRunnable = new FolderWatcherRunnable(watchedFolder, sp);

		// Run with an not existing folder
		{
			FileUtils.deleteDirectory(watchedFolder.toFile());
			folderWatcherRunnable.run();
			final FilePusher filePusher = sp.getDaoService().get();
			Assert.assertEquals(0, filePusher.getItems().size());
		}

		// Run with an empty folder
		{
			Files.createDirectories(watchedFolder);
			folderWatcherRunnable.run();
			final FilePusher filePusher = sp.getDaoService().get();
			Assert.assertEquals(0, filePusher.getItems().size());
		}

		// Run with one file ine the folder
		{
			Files.copy(Paths.get("src/test/resources/to-send/to-send-File01.log"),
					watchedFolder.resolve("to-send-File01.log"));
			folderWatcherRunnable.run();
			final FilePusher filePusher = sp.getDaoService().get();
			Assert.assertEquals(1, filePusher.getItems().size());
		}

		// Run with two file in the folder
		{
			Files.copy(Paths.get("src/test/resources/to-send/to-send-File02.log"),
					watchedFolder.resolve("to-send-File02.log"));
			folderWatcherRunnable.run();
			final FilePusher filePusher = sp.getDaoService().get();
			Assert.assertEquals(2, filePusher.getItems().size());
		}

		// Run with two file and one folder in the folder
		{
			FileUtils.copyDirectory(Paths.get("src/test/resources/to-send/to-send-Folder01").toFile(),
					watchedFolder.resolve("to-send-Folder01").toFile());
			folderWatcherRunnable.run();
			final FilePusher filePusher = sp.getDaoService().get();
			Assert.assertEquals(3, filePusher.getItems().size());
		}

		// Run with adding a file to the folder in the folder
		{
			Files.copy(Paths.get("src/test/resources/to-send/to-send-File02.log"),
					watchedFolder.resolve("to-send-Folder01").resolve("to-send-Folder01File03.log"));
			folderWatcherRunnable.run();
			final FilePusher filePusher = sp.getDaoService().get();
			Assert.assertEquals(3, filePusher.getItems().size());
		}

		// Run without file modification during the cooldown time
		{
			Thread.sleep(sp.getConfigService().getFileChangeCooldown() * 1000 + 10);
			folderWatcherRunnable.run();
			final FilePusher filePusher = sp.getDaoService().get();
			Assert.assertEquals(3, filePusher.getItems().size());
			final List<FfpItem> byStatus = sp.getDaoService().getByStatus(StatusEnum.TO_SEND);
			Assert.assertEquals(3, byStatus.size());
		}
	}
}
