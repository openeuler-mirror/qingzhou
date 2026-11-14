package qingzhou.common;

import java.io.File;
import java.nio.file.Files;

import org.testng.Assert;
import org.testng.annotations.Test;

public class ExpiredFileCleanerTest {
    @Test
    public void cleanExpired_removesOnlyStaleChildren() throws Exception {
        File dir = Files.createTempDirectory("tempfiles-expire").toFile();
        File stale = new File(dir, "stale.txt");
        Assert.assertTrue(stale.createNewFile());

        ExpiredFileCleaner.cleanExpired(dir, 30_000);

        Assert.assertFalse(stale.exists());
    }

    @Test
    public void clean_missingDirectory_isSilent() {
        ExpiredFileCleaner.cleanExpired(new File("tempfiles-not-exists-" + System.nanoTime()), 1_000);
    }

    @Test
    public void clean_nullDirectory_isSilent() {
        ExpiredFileCleaner.cleanExpired(null, 1_000);
    }
}
