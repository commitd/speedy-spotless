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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffEntry.ChangeType;
import org.eclipse.jgit.lib.Repository;
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

    try (Git git = Git.open(new File("."))) {

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

      Set<String> partiallyStagedFiles =
          getPartiallyStagedFiles(stagedChangedFiles, unstagedChangedFiles);

      List<String> fullyStagedFiles =
          stagedChangedFiles.stream()
              .distinct()
              .filter(f -> !unstagedChangedFiles.contains(f))
              .collect(Collectors.toList());

      List<File> stagedFiles =
          stagedChangedFiles.stream()
              .map(
                  filePath ->
                      repository.getDirectory().getParentFile().toPath().resolve(filePath).toFile())
              .collect(toList());
      super.process(stagedFiles, formatter);
      getLog().info("Formatted " + stagedFiles.size() + " staged files");

      for (String f : fullyStagedFiles) {
        stage(git, f);
      }
      if (!partiallyStagedFiles.isEmpty()) {
        throwPartialUnstaged(partiallyStagedFiles);
      }
    } catch (IOException e) {
      throw new MojoExecutionException("Could not open Git repository", e);
    }
  }

  private void throwPartialUnstaged(Set<String> partiallyStagedFiles)
      throws MojoExecutionException {
    throw new MojoExecutionException(
        format(
            "Partially staged files were formatted but not re-staged:%n%s",
            String.join("\\n", partiallyStagedFiles)));
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

  private List<String> getChangedFiles(Git git, boolean staged, TreeFilter pathFilter)
      throws MojoExecutionException {
    try {
      // do we need to include untracked files also? e.g. ls-files --others --exclude-standard
      return git.diff().setPathFilter(pathFilter).setShowNameAndStatusOnly(true).setCached(staged)
          .call().stream()
          .filter(e -> CHANGE_TYPES.contains(e.getChangeType()))
          .map(DiffEntry::getNewPath)
          .collect(toList());
    } catch (GitAPIException e) {
      throw new MojoExecutionException("Failed to list changed files", e);
    }
  }
}
