package io.committed.speedy.format;

import com.diffplug.spotless.maven.SpotlessCheckMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;

@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class SpeedyCheckMojo extends SpotlessCheckMojo {}
