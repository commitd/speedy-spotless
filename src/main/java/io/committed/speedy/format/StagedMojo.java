package io.committed.speedy.format;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.PaddedCell;
import com.diffplug.spotless.maven.SpotlessApplyMojo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;
import org.eclipse.jgit.treewalk.filter.TreeFilter;

@Mojo(name = "staged", threadSafe = true)
public class StagedMojo extends SpotlessApplyMojo {

  private static final List<ChangeType> CHANGE_TYPES =
      asList(ChangeType.ADD, ChangeType.COPY, ChangeType.MODIFY, ChangeType.RENAME);

  @Override
  protected void process(Iterable<File> files, Formatter formatter) throws MojoExecutionException {
    if (!files.iterator().hasNext()) {
      return;
    }

    try (Git git = openGitRepo()) {

      Repository repository = git.getRepository();
      Path workTreePath = repository.getWorkTree().toPath();

      TreeFilter treeFilter =
          PathFilterGroup.createFromStrings(
              StreamSupport.stream(files.spliterator(), false)
                  .map(f -> workTreePath.relativize(f.toPath()))
                  .map(f -> f.toString().replace('\\', '/'))
                  .collect(Collectors.toList()));

      List<String> stagedChangedFiles = getChangedFiles(git, true, treeFilter);
      if (stagedChangedFiles.isEmpty()) {
        getLog().debug("No files were formatted for this formatter");
        return;
      }
      List<String> unstagedChangedFiles = getChangedFiles(git, false, treeFilter);

      Set<File> partiallyStagedFiles =
          getPartiallyStagedFiles(stagedChangedFiles, unstagedChangedFiles, repository);

      List<String> fullyStagedFiles =
          stagedChangedFiles.stream()
              .distinct()
              .filter(f -> !unstagedChangedFiles.contains(f))
              .collect(Collectors.toList());

      List<File> stagedFiles =
          stagedChangedFiles.stream()
              .map(filePath -> this.resolveGitPath(filePath, repository))
              .collect(toList());
      super.process(stagedFiles, formatter);
      getLog().info("Formatted " + stagedFiles.size() + " staged files");

      for (String f : fullyStagedFiles) {
        stage(git, f);
      }
      if (!partiallyStagedFiles.isEmpty()) {
        Set<File> unformatted = findUnformatted(partiallyStagedFiles, formatter);
        if (!unformatted.isEmpty()) {
          throwPartialUnstaged(unformatted);
        }
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Could not open Git repository", e);
    }
  }

  private File resolveGitPath(String filePath, Repository repository) {
    return repository.getDirectory().getParentFile().toPath().resolve(filePath).toFile();
  }

  private Set<File> findUnformatted(Set<File> files, Formatter formatter) {
    List<File> problemFiles = new ArrayList<>();
    for (File file : files) {
      try {
        PaddedCell.DirtyState dirtyState = PaddedCell.calculateDirtyState(formatter, file);
        if (!dirtyState.isClean() && !dirtyState.didNotConverge()) {
          problemFiles.add(file);
        }
      } catch (IOException e) {
        throw new RuntimeException("Unable to format file " + file, e);
      }
    }
    return new TreeSet(problemFiles);
  }

  private Git openGitRepo() throws IOException {
    File cwd = Paths.get("").toFile().getAbsoluteFile();
    try {
      return Git.open(cwd);
    } catch (IOException e) {
      FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder();
      repositoryBuilder.findGitDir(cwd);
      File gitDir = repositoryBuilder.getGitDir();
      if (gitDir != null) {
        return Git.open(gitDir);
      } else {
        throw new IOException(
            "Could not find git directory scanning upwards from " + cwd.getPath());
      }
    }
  }

  private void throwPartialUnstaged(Set<File> partiallyStagedFiles) throws MojoExecutionException {
    throw new MojoExecutionException(
        format(
            "There are partially staged :%n%s",
            String.join(
                "\\n",
                partiallyStagedFiles.stream()
                    .map(File::getAbsolutePath)
                    .collect(Collectors.toList()))));
  }

  private void stage(Git git, String f) throws MojoExecutionException {
    try {
      git.add().addFilepattern(f).call();
    } catch (GitAPIException e) {
      throw new MojoExecutionException("Failed to stage", e);
    }
  }

  private Set<File> getPartiallyStagedFiles(
      List<String> stagedChangedFiles, List<String> unstagedChangedFiles, Repository repository) {
    return stagedChangedFiles.stream()
        .distinct()
        .filter(unstagedChangedFiles::contains)
        .map(filePath -> this.resolveGitPath(filePath, repository))
        .collect(Collectors.toSet());
  }

  private List<String> getChangedFiles(Git git, boolean staged, TreeFilter pathFilter)
      throws MojoExecutionException {
    try {
      // do we need to include untracked files also? e.g. ls-files --others --exclude-standard
      return git
          .diff()
          .setPathFilter(pathFilter)
          .setShowNameAndStatusOnly(true)
          .setCached(staged)
          .call()
          .stream()
          .filter(e -> CHANGE_TYPES.contains(e.getChangeType()))
          .map(DiffEntry::getNewPath)
          .collect(toList());
    } catch (GitAPIException e) {
      throw new MojoExecutionException("Failed to list changed files", e);
    }
  }
}
