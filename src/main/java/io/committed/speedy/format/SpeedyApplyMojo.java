package io.committed.speedy.format;

import org.apache.maven.plugins.annotations.Mojo;
import com.diffplug.spotless.maven.SpotlessApplyMojo;

@Mojo(name = "apply", threadSafe = true)
public class SpeedyApplyMojo extends SpotlessApplyMojo {

}
