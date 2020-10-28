package io.committed.speedy.hook.executable;

import java.io.IOException;
import java.io.InputStream;
import java.util.function.Supplier;

/**
 * Created on 08/11/17.
 *
 * @author Reda.Housni-Alaoui
 */
public interface Executable {

  /**
   * Erase the executable content
   *
   * @return The executable
   * @throws IOException when the content could not be erased
   */
  Executable truncate() throws IOException;

  /**
   * @param template The template to truncate with
   * @param sourceEncoding The source encoding
   * @param values The values to use for the template interpolations
   * @return The executable
   * @throws IOException when the content could not be erased
   */
  Executable truncateWithTemplate(
      Supplier<InputStream> template, String sourceEncoding, Object... values) throws IOException;

  /**
   * Appends a command call to the executable
   *
   * @param commandCall The command call to append to the executable
   * @return The executable
   * @throws IOException when the call could not be appended
   */
  Executable appendCommandCall(String commandCall) throws IOException;
}
