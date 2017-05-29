package org.arquillian.smart.testing.surefire.provider;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
import java.util.Collection;
import org.jboss.shrinkwrap.resolver.api.maven.Maven;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(Parameterized.class)
public class LoaderVersionExtractorSurefireBooterTest {

    private String surefireBooterVersion;

    public LoaderVersionExtractorSurefireBooterTest(String surefireBooterVersion) {
        this.surefireBooterVersion = surefireBooterVersion;
    }

    @Parameterized.Parameters
    public static Collection<String> data() {
        return Arrays.asList(new String[] {"2.8", "2.13", "2.19.1", "2.20"});
    }

    @Test
    public void test_should_load_surefire_booter_version() throws MalformedURLException {
        // given
        File junitFile = Maven
            .resolver()
            .resolve("org.apache.maven.surefire:surefire-booter:" + surefireBooterVersion)
            .withoutTransitivity()
            .asSingleFile();

        URL[] junitUrl = {junitFile.toURI().toURL()};
        URLClassLoader urlClassLoader = new URLClassLoader(junitUrl, (ClassLoader) null);

        // when
        String junitVersion =
            LoaderVersionExtractor.getVersionFromClassLoader(LoaderVersionExtractor.LIBRARY_SUREFIRE_BOOTER,
                urlClassLoader);

        // then
        assertThat(junitVersion).isEqualTo(surefireBooterVersion);
    }
}
