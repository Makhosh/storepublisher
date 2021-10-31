

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class StorePublisherPluginTest extends Specification {
    @TempDir
    File testProjectDir
    File buildFile

    def setup() {
        buildFile = new File(testProjectDir, 'build.gradle')
        buildFile << """
            plugins {
                id 'com.umutata.StorePublisherPlugin'
            }
        """
    }

    def "store upload"() {
        given:
        buildFile << """
            storePublisher {
                 apkFile = file('/Users/umutatacan/Desktop/APK/Bonus-prd-release.apk')
   huaweiAppGallery{
        appId = '103589977'
        clientId = '719131324236973120'
        clientSecret = '73E5D39D6250DB160676BC17A25866E92E1C4A9D495EF395243812B5C4A57876'
    }

            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments('storePublisher')
                .withPluginClasspath()
                .withDebug(true)
                .build()

        then:
        result.task(":storePublisher").outcome == SUCCESS
    }
}