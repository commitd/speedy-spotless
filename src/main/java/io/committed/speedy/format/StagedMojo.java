package io.committed.speedy.format;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.maven.SpotlessApplyMojo;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

@Mojo(name = "staged", threadSafe = true)
public class StagedMojo extends SpotlessApplyMojo {

  private static final List<ChangeType> CHANGE_TYPES =
      asList(ChangeType.ADD, ChangeType.COPY, ChangeType.MODIFY, ChangeType.RENAME);

  @Override
  protected void process(List<File> files, Formatter formatter) throws MojoExecutionException {
    // TODO parameterise this (or filter against SpotlessApplyMojo.process(allFiles, ...)
    Predicate<? super String> pathMatcher = path -> path.matches(".*\\.java");
    Repository repository = getRepo();

    getLog().info("Formatting staged files");

    try (Git git = new Git(repository)) {
      List<String> stagedChangedFiles = getChangedFiles(git, true, pathMatcher);
      if (stagedChangedFiles.isEmpty()) {
        getLog().info("No files were formatted");
        return;
      }
      List<String> unstagedChangedFiles = getChangedFiles(git, false, pathMatcher);

      Set<String> partiallyStagedFiles =
          getPartiallyStagedFiles(stagedChangedFiles, unstagedChangedFiles);

      List<String> fullyStagedFiles =
          stagedChangedFiles.stream()
              .distinct()
              .filter(f -> !unstagedChangedFiles.contains(f))
              .collect(Collectors.toList());

      List<File> stagedFiles =
          stagedChangedFiles.stream()
              .map(filePath -> gitBaseDir().resolve(filePath).toFile())
              .collect(toList());
      super.process(stagedFiles, formatter);
      getLog().info("Formatted " + stagedFiles.size() + " staged files");

      for (String f : fullyStagedFiles) {
        stage(git, f);
      }
      if (!partiallyStagedFiles.isEmpty()) {
        throwPartialUnstaged(partiallyStagedFiles);
      }
    }
  }

  private void throwPartialUnstaged(Set<String> partiallyStagedFiles)
      throws MojoExecutionException {
    throw new MojoExecutionException(
        format(
            "Partially staged files were formatted but not re-staged:%n%s",
            partiallyStagedFiles.stream().collect(Collectors.joining("\\n"))));
  }

  private void stage(Git git, String f) throws MojoExecutionException {
    try {
      git.add().addFilepattern(f).call();
    } catch (GitAPIException e) {
      throw new MojoExecutionException("Failed to stage", e);
    }
  }

  private Set<String> getPartiallyStagedFiles(
      List<String> stagedChangedFiles, List<String> unstagedChangedFiles) {
    return stagedChangedFiles.stream()
        .distinct()
        .filter(unstagedChangedFiles::contains)
        .collect(Collectors.toSet());
  }

  private List<String> getChangedFiles(Git git, boolean staged, Predicate<? super String> predicate)
      throws MojoExecutionException {
    try {
      // do we need to include untracked files also? e.g. ls-files --others --exclude-standard
      return git.diff().setShowNameAndStatusOnly(true).setCached(staged).call().stream()
          .filter(e -> CHANGE_TYPES.contains(e.getChangeType()))
          .map(DiffEntry::getNewPath)
          .filter(predicate)
          .collect(toList());
    } catch (GitAPIException e) {
      throw new MojoExecutionException("Failed to list changed files", e);
    }
  }

  protected Repository getRepo() {
    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    Repository repository;
    try {
      repository =
          builder
              .readEnvironment() // scan environment GIT_* variables
              .findGitDir() // scan up the file system tree
              .build();
    } catch (IOException e) {
      throw new RuntimeException("Failed to find Git repository", e);
    }
    return repository;
  }

  protected final Path gitBaseDir() {
    return getRepo().getDirectory().getParentFile().toPath();
  }
}
