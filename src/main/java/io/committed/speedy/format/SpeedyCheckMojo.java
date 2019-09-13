package io.committed.speedy.format;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import com.diffplug.spotless.maven.SpotlessCheckMojo;

@Mojo(name = "check", defaultPhase = LifecyclePhase.VERIFY, threadSafe = true)
public class SpeedyCheckMojo extends SpotlessCheckMojo {

}
