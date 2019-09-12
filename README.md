# speedy-spotless

For easy formatting of staged changes. Inspired by [pretty-quick](https://github.com/azz/pretty-quick).

It includes all goals from Spotless Maven Plugin but includes a new goal *apply-staged*.

`speedy-spotless:apply-staged` will only format staged files. Partially staged files will still be formatted but will trigger an error (cancelling say a Git commit).

Speedy Spotless can easily be added as part of a pre-commit hook, e.g.:

```xml

<build>
    <plugins>
      <plugin>
        <groupId>io.committed</groupId>
        <artifactId>speedy-spotless-maven-plugin</artifactId>
        <version>0.0.1-SNAPSHOT</version>
      </plugin>
      <plugin>
        <groupId>io.github.phillipuniverse</groupId>
        <artifactId>githook-maven-plugin</artifactId>
        <version>1.0.4</version>
        <executions>
          <execution>
            <goals>
              <goal>install</goal>
            </goals>
            <configuration>
              <hooks>
                <pre-commit>
                  mvn speedy-spotless:apply-staged
                </pre-commit>
              </hooks>
            </configuration>
          </execution>
        </executions>
      </plugin>
    </plugins>
</build>

```
