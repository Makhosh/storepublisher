import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.*;

class UploadTestTest {

    @TempDir
    File testProjectDir;
    private File buildFile;

    @BeforeEach
    public void setup() throws IOException {
        buildFile = new File(testProjectDir, "build.gradle");
        String text = "plugins {\n" +
                "                id 'io.github.makhosh.storepublisher'\n" +
                "            }\n  storePublisher {\n" +
                "                 apkFile = file('/Users/umutatacan/Desktop/APK/Bonus-prd-release.apk')\n" +
                "\n" +
                "            }";
        Files.write(buildFile.toPath(), Collections.singletonList(text));
    }

    @Test
    void uploadTest() {
        BuildResult result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("storePublisher")
                .withPluginClasspath()
                .withDebug(true)
                .build();
        assertEquals(SUCCESS, result.task(":storePublisher").getOutcome());
    }
}