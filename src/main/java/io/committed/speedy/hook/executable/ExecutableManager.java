package io.committed.speedy.hook.executable;

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.function.Supplier;
import org.apache.maven.plugin.logging.Log;

/**
 * Created on 02/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public class ExecutableManager {

  private final Supplier<Log> log;

  public ExecutableManager(Supplier<Log> log) {
    requireNonNull(log);
    this.log = log;
  }

  /**
   * Get or creates a file then mark it as executable.
   *
   * @param file The file
   * @return the script
   * @throws IOException when the script file could not be accessed
   */
  public Executable getOrCreateExecutableScript(Path file) throws IOException {
    return new DefaultExecutable(log, file);
  }
}
