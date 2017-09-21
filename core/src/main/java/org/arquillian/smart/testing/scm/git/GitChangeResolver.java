package org.arquillian.smart.testing.scm.git;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.arquillian.smart.testing.Logger;
import org.arquillian.smart.testing.configuration.Configuration;
import org.arquillian.smart.testing.configuration.Scm;
import org.arquillian.smart.testing.scm.Change;
import org.arquillian.smart.testing.scm.ChangeType;
import org.arquillian.smart.testing.scm.spi.ChangeResolver;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.RenameDetector;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import static org.arquillian.smart.testing.scm.Change.add;
import static org.arquillian.smart.testing.scm.Change.modify;

public class GitChangeResolver implements ChangeResolver {

    private static final String ENSURE_TREE = "^{tree}";

    private static final Logger logger = Logger.getLogger();

    private final String previous;
    private final String head;
    private final File repoRoot;
    private final Git git;

    public GitChangeResolver() {
        this(Paths.get("").toAbsolutePath().toFile(), Configuration.loadPrecalculated().getScm());
    }

    public GitChangeResolver(File projectDir) {
        this(projectDir, Configuration.loadPrecalculated().getScm());
    }

    public GitChangeResolver(File projectDir, Scm scm) {
        this(projectDir, scm.getRange().getTail(), scm.getRange().getHead());
    }

    public GitChangeResolver(File dir, String previous, String head) {
        this.previous = previous;
        this.head = head;
        final FileRepositoryBuilder builder = new FileRepositoryBuilder();
        try {
            this.git = new Git(builder.readEnvironment().findGitDir(dir).build());
        } catch (IOException e) {
            throw new IllegalArgumentException("Unable to find git repository for path " + dir.getAbsolutePath(), e);
        }
        this.repoRoot = git.getRepository().getDirectory().getParentFile();
    }

    @Override
    public void close() throws Exception {
        git.close();
    }

    @Override
    public Set<Change> diff() {
        final Set<Change> allChanges= new HashSet<>();

        allChanges.addAll(retrieveCommitsChanges());
        allChanges.addAll(retrieveUncommittedChanges());

        return allChanges;
    }

    @Override
    public boolean isApplicable() {
        try {
            final FileRepositoryBuilder builder = new FileRepositoryBuilder();
            builder.readEnvironment().findGitDir().build();
        } catch (IOException e) {
            logger.warn("Working directory is not git directory. Cause: %s", e.getMessage());
            return false;
        }
        return true;
    }

    private Set<Change> retrieveCommitsChanges() {
        final Repository repository = git.getRepository();
        try (ObjectReader reader = repository.newObjectReader()) {
            final ObjectId oldHead = repository.resolve(previous + ENSURE_TREE);
            final ObjectId newHead = repository.resolve(head + ENSURE_TREE);

            final CanonicalTreeParser oldTree = new CanonicalTreeParser();
            oldTree.reset(reader, oldHead);
            final CanonicalTreeParser newTree = new CanonicalTreeParser();
            newTree.reset(reader, newHead);

            final List<DiffEntry> commitDiffs = git.diff()
                    .setNewTree(newTree)
                    .setOldTree(oldTree)
                .call();
            return transformToChangeSet(reduceToRenames(commitDiffs), repoRoot);
        } catch (IOException | GitAPIException e) {
            throw new IllegalStateException(e);
        }
    }

    private Set<Change> retrieveUncommittedChanges() {
        final Set<Change> allChanges = new HashSet<>();

        final Status status;
        try {
            status = git.status().call();
        } catch (GitAPIException e) {
            throw new IllegalArgumentException(e);
        }

        allChanges.addAll(status.getModified()
            .stream()
            .map(location -> modify(repoRoot.getAbsolutePath(), location))
            .collect(Collectors.toSet()));

        allChanges.addAll(status.getChanged()
            .stream()
            .map(location -> modify(repoRoot.getAbsolutePath(), location))
            .collect(Collectors.toSet()));

        allChanges.addAll(status.getUntracked()
            .stream()
            .map(location -> add(repoRoot.getAbsolutePath(), location))
            .collect(Collectors.toSet()));

        allChanges.addAll(status.getAdded()
            .stream()
            .map(location -> add(repoRoot.getAbsolutePath(), location))
            .collect(Collectors.toSet()));

        return allChanges;
    }

    /**
     * By default jgit sees renames as ADDs and DELETEs. For finding renames we use {@link RenameDetector} which tracks changes
     * and if they much similarity score (default 60%) treats them as renames
     *
     * @param commitDiffs diffs to analyze
     * @return original list reduced by ADDs/DELETEs which are in fact RENAMEs
     * @throws IOException
     */
    private List<DiffEntry> reduceToRenames(final Collection<DiffEntry> commitDiffs) throws IOException {
        final RenameDetector renameDetector = new RenameDetector(git.getRepository());
        renameDetector.addAll(commitDiffs);
        return renameDetector.compute();
    }

    private Set<Change> transformToChangeSet(List<DiffEntry> diffs, File repoRoot) {
        return diffs.stream()
            .map(diffEntry -> {
                final Path classLocation = Paths.get(repoRoot.getAbsolutePath(), diffEntry.getNewPath());
                final ChangeType changeType = ChangeType.valueOf(diffEntry.getChangeType().name());

                return new Change(classLocation, changeType);
            })
            .collect(Collectors.toSet());
    }

}
