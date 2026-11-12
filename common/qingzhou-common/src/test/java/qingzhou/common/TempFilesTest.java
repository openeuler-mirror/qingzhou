package qingzhou.common;

import java.io.File;
import java.nio.file.Files;

import org.testng.Assert;
import org.testng.annotations.Test;

public class TempFilesTest {
    @Test
    public void cleanExpired_removesOnlyStaleChildren() throws Exception {
        File dir = Files.createTempDirectory("tempfiles-expire").toFile();
        File stale = new File(dir, "stale.txt");
        File fresh = new File(dir, "fresh.txt");
        Assert.assertTrue(stale.createNewFile());
        Assert.assertTrue(fresh.createNewFile());
        Assert.assertTrue(stale.setLastModified(System.currentTimeMillis() - 60_000)); // 早于 TTL

        TempFiles.cleanExpired(dir, 30_000);

        Assert.assertFalse(stale.exists());
        Assert.assertTrue(fresh.exists());
    }

    @Test
    public void clean_missingDirectory_isSilent() {
        TempFiles.cleanExpired(new File("tempfiles-not-exists-" + System.nanoTime()), 1_000);
    }

    @Test
    public void clean_nullDirectory_isSilent() {
        TempFiles.cleanExpired(null, 1_000);
    }
}
