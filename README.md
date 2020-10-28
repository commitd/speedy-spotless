# Speedy Spotless

For easy formatting of staged changes. Inspired by [pretty-quick](https://github.com/azz/pretty-quick).

It includes `apply` and `check` goals from Spotless Maven Plugin but also includes the new goal `staged` to trigger the formatting of files staged in Git.

Works with Java 8+.

## Installation

Speedy Spotless supports the exact same configuration options as Spotless Maven Plugin.

Additionally the `install-hooks` goal may be used to install a pre-commit Git hook to format staged files when committing.

```xml

<build>
    <plugins>
      <plugin>
        <groupId>io.committed</groupId>
        <artifactId>speedy-spotless-maven-plugin</artifactId>
        <version>0.1.1</version>
        <executions>
          <execution>
            <id>install-formatter-hook</id>
            <goals>
              <goal>install-hooks</goal>
            </goals>
          </execution>
        </executions>
        <configuration>
          <java>
            <googleJavaFormat>
              <style>GOOGLE</style>
            </googleJavaFormat>
            <removeUnusedImports />
          </java>
        </configuration>
      </plugin>
    </plugins>
</build>

```

Ensure the `install-hooks` goal is declared in your root POM.

## Configuration

See [Spotless Maven Plugin](https://github.com/diffplug/spotless/tree/master/plugin-maven#applying-to-java-source) for code formatting options.

## Caveats

- Currently only Java files are formatted. Spotless's `spotlessFiles` option is ignored.

## Building

```
# Building the maven plugin
mvn clean package

# Installing the maven plugin
mvn clean install -DskipTests
```

## Deploying to Maven Central

```
# Required on macOS
GPG_TTY=$(tty)
export GPG_TTY

# Setup GPG, maven settings.xml

mvn clean deploy -P release
```