package io.committed.speedy;

import static java.util.stream.Collectors.toList;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import com.diffplug.spotless.Formatter;
import com.diffplug.spotless.maven.SpotlessApplyMojo;


@Mojo(name = "format-staged", threadSafe = true)
public class SpeedyMojo extends SpotlessApplyMojo {

  @Override
  protected void process(List<File> allFiles, Formatter formatter) throws MojoExecutionException {
    Predicate<? super String> predicate = path -> path.matches(".*\\.java");
    Repository repository = createRepo();

    try {
      BufferedWriter fileWriter = new BufferedWriter(new FileWriter(new File("testing")));
      fileWriter.write("testing");
      fileWriter.close();
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    }

    getLog().info("Starting");

    try (Git git = new Git(repository)) {
      List<String> stagedChangedFiles = getChangedFiles(git, true, predicate);
      List<String> unstagedChangedFiles = getChangedFiles(git, true, predicate);

      Set<String> partiallyStagedFiles =
          getPartiallyStagedFiles(stagedChangedFiles, unstagedChangedFiles);

      List<String> fullyStagedFiles = stagedChangedFiles.stream()
          .distinct()
          .filter(f -> !unstagedChangedFiles.contains(f))
          .collect(Collectors.toList());


      super.process(stagedChangedFiles.stream().map(File::new).collect(toList()), formatter);
      getLog().info("Formatted " + fullyStagedFiles.size() + " fully staged files");

      for (String f : fullyStagedFiles) {
        stage(git, f);
      }
      if (!partiallyStagedFiles.isEmpty()) {
        throwPartialUnstaged(partiallyStagedFiles);
      }
    } catch (GitAPIException e) {
      throw new MojoExecutionException("Failed to stage");
    }
  }

  private void throwPartialUnstaged(Set<String> partiallyStagedFiles)
      throws MojoExecutionException {
    throw new MojoExecutionException(partiallyStagedFiles.size()
        + " partially staged files were formatted but not re-staged");
  }

  private void stage(Git git, String f) throws GitAPIException {
    git.add().addFilepattern(f).call();
  }

  private Set<String> getPartiallyStagedFiles(List<String> stagedChangedFiles,
      List<String> unstagedChangedFiles) {
    return stagedChangedFiles.stream()
        .distinct()
        .filter(unstagedChangedFiles::contains)
        .collect(Collectors.toSet());
  }

  private List<String> getChangedFiles(Git git, boolean staged, Predicate<? super String> predicate)
      throws GitAPIException {
    return git.diff().setCached(staged).setShowNameAndStatusOnly(true).call().stream()
        .map(DiffEntry::getNewPath)
        .filter(predicate)
        .collect(toList());
  }

  private Repository createRepo() throws MojoExecutionException {
    FileRepositoryBuilder builder = new FileRepositoryBuilder();
    Repository repository;
    try {
      repository = builder.setGitDir(new File("."))
          .readEnvironment() // scan environment GIT_* variables
          .findGitDir() // scan up the file system tree
          .build();
    } catch (IOException e) {
      throw new MojoExecutionException("Failed to find Git repository", e);
    }
    return repository;
  }
}
