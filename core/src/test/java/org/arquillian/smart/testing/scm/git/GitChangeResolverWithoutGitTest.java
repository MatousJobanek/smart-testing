package org.arquillian.smart.testing.scm.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Set;
import org.arquillian.smart.testing.scm.Change;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

public class GitChangeResolverWithoutGitTest {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private File gitFolder;

    @Before
    public void unpack_repo() throws IOException {
        gitFolder = Files.createTempDirectory("junit-").toFile();
    }

    @Test
    public void should_not_applicable_when_git_repository_is_not_initialized() throws IOException {
        // given
        GitChangeResolver gitChangeResolver = new GitChangeResolver(gitFolder, "HEAD", "HEAD~0");

        // when
        final boolean applicable = gitChangeResolver.isApplicable();

        // then
        assertThat(applicable).isFalse();
    }

    @Test
    public void should_throw_exception_for_fetching_changes_when_git_repository_is_not_initialized()
        throws IOException {
        // given
        GitChangeResolver gitChangeResolver = new GitChangeResolver(gitFolder, "HEAD", "HEAD~0");

        // then
        thrown.expect(IllegalStateException.class);

        // when
        gitChangeResolver.diff(GitChangeResolverTest.CUSTOM);
    }

    @Test
    public void should_return_empty_changes_when_git_repository_is_not_initialized_and_strategy_is_not_specified()
        throws IOException {
        // given
        GitChangeResolver gitChangeResolver = new GitChangeResolver(gitFolder, "HEAD", "HEAD~0");

        // when
        Set<Change> changes = gitChangeResolver.diff();

        // then
        assertThat(changes).isEmpty();
    }
}
