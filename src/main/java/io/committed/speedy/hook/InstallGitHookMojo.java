package io.committed.speedy.hook;

import io.committed.speedy.hook.executable.Executable;
import io.committed.speedy.hook.executable.ExecutableManager;
import io.committed.speedy.hook.util.MavenUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

@Mojo(name = "install-hooks", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class InstallGitHookMojo extends AbstractMojo {

  private static final String HOOKS_DIR = "hooks";
  private static final String BASE_PLUGIN_PRE_COMMIT_HOOK = "maven-git-code-format.pre-commit.sh";
  private static final String PRE_COMMIT_HOOK_BASE_SCRIPT = "pre-commit";

  private final ExecutableManager executableManager = new ExecutableManager(this::getLog);
  private final MavenUtils mavenUtils = new MavenUtils();

  @Parameter(readonly = true, defaultValue = "${project}")
  private MavenProject currentProject;

  /**
   * True to truncate hooks base scripts before each install. <br>
   * Do not use this option if any other system or human manipulate the hooks
   */
  @Parameter(property = "truncateHooksBaseScripts", defaultValue = "false")
  private boolean truncateHooksBaseScripts;

  /** The list of properties to propagate to the hooks */
  @Parameter(property = "propertiesToPropagate")
  private String[] propertiesToPropagate;

  @Override
  public void execute() throws MojoExecutionException {
    if (!isExecutionRoot()) {
      getLog().debug("Not in execution root. Do not execute.");
      return;
    }

    try {
      getLog().info("Installing git hooks");
      doExecute();
      getLog().info("Installed git hooks");
    } catch (Exception e) {
      throw new MojoExecutionException(e.getMessage(), e);
    }
  }

  private void doExecute() throws IOException {
    Path hooksDirectory = prepareHooksDirectory();

    writePluginHooks(hooksDirectory);

    configureHookBaseScripts(hooksDirectory);
  }

  protected final Path baseDir() {
    return currentProject.getBasedir().toPath();
  }

  protected final boolean isExecutionRoot() {
    return currentProject.isExecutionRoot();
  }

  protected final String artifactId() {
    return currentProject.getArtifactId();
  }

  protected final Path pomFile() {
    return currentProject.getFile().toPath();
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

  private void writePluginHooks(Path hooksDirectory) throws IOException {
    getLog().debug("Writing plugin pre commit hook file");
    executableManager
        .getOrCreateExecutableScript(hooksDirectory.resolve(pluginPreCommitHookFileName()))
        .truncateWithTemplate(
            () -> getClass().getResourceAsStream(BASE_PLUGIN_PRE_COMMIT_HOOK),
            StandardCharsets.UTF_8.toString(),
            mavenUtils.getMavenExecutable().toAbsolutePath(),
            pomFile().toAbsolutePath(),
            mavenCliArguments());
    getLog().debug("Written plugin pre commit hook file");
  }

  private void configureHookBaseScripts(Path hooksDirectory) throws IOException {
    Executable basePreCommitHook =
        executableManager.getOrCreateExecutableScript(
            hooksDirectory.resolve(PRE_COMMIT_HOOK_BASE_SCRIPT));
    getLog().debug("Configuring '" + basePreCommitHook + "'");
    if (truncateHooksBaseScripts) {
      basePreCommitHook.truncate();
    }
    basePreCommitHook.appendCommandCall(preCommitHookBaseScriptCall());
  }

  private String mavenCliArguments() {

    return Optional.ofNullable(propertiesToPropagate)
        .map(Arrays::asList)
        .orElse(Collections.emptyList())
        .stream()
        .filter(prop -> System.getProperty(prop) != null)
        .map(prop -> "-D" + prop + "=" + System.getProperty(prop))
        .collect(Collectors.joining(" "));
  }

  private Path prepareHooksDirectory() {
    getLog().debug("Preparing git hook directory");
    Path hooksDirectory;
    hooksDirectory = getOrCreateHooksDirectory();
    getLog().debug("Prepared git hook directory");
    return hooksDirectory;
  }

  private String preCommitHookBaseScriptCall() {
    return "./"
        + gitBaseDir().relativize(getOrCreateHooksDirectory())
        + "/"
        + pluginPreCommitHookFileName();
  }

  private String pluginPreCommitHookFileName() {
    return artifactId() + "." + BASE_PLUGIN_PRE_COMMIT_HOOK;
  }

  protected Path getOrCreateHooksDirectory() {
    Path hooksDirectory = getRepo().getDirectory().toPath().resolve(HOOKS_DIR);
    if (!Files.exists(hooksDirectory)) {
      getLog().debug("Creating directory " + hooksDirectory);
      try {
        Files.createDirectories(hooksDirectory);
      } catch (IOException e) {
        throw new RuntimeException("Failed to create hooks directory", e);
      }
    } else {
      getLog().debug(hooksDirectory + " already exists");
    }
    return hooksDirectory;
  }
}
