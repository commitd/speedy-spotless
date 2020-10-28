package io.committed.speedy.format;

import com.diffplug.spotless.maven.SpotlessApplyMojo;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "apply", threadSafe = true)
public class SpeedyApplyMojo extends SpotlessApplyMojo {}
