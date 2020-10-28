#!/usr/bin/env bash

mvn -B install -DskipTests

BASE_DIR=$(pwd)
ROOT_DIR=$(mktemp -d -t speedy-XXXXXXXXXX)
SPEEDY_VERSION=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`

pushd $ROOT_DIR

mvn archetype:generate -DgroupId=test -DartifactId=partial -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
cd partial
 
mkdir -p src/main/java/io/committed/speedy
cp $BASE_DIR/src/main/java/io/committed/speedy/Example.java src/main/java/io/committed/speedy

echo '<project xmlns="http://maven.apache.org/POM/4.0.0" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
  xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/maven-v4_0_0.xsd">
  <modelVersion>4.0.0</modelVersion>
  <groupId>test</groupId>
  <artifactId>partial</artifactId>
  <packaging>jar</packaging>
  <version>1.0-SNAPSHOT</version>
  <name>partial</name>
  <url>http://maven.apache.org</url>
  <dependencies>
    <dependency>
      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>
      <scope>test</scope>
    </dependency>
  </dependencies>

  <build>
    <plugins>
      <plugin>
        <groupId>io.committed</groupId>
        <artifactId>speedy-spotless-maven-plugin</artifactId>
        <version>0.1.1</version>
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

</project>' >pom.xml

JAVA_FILE="src/main/java/io/committed/speedy/Example.java"
FORMATTED_JAVA_FILE=$ROOT_DIR/partial/$JAVA_FILE
BASE_JAVA_FILE=$BASE_DIR/$JAVA_FILE

git init
git config user.email "you@example.com"
git config user.name "Your Name"
git add .
git commit -m initial
# insert incorrect formatting
tmpfile=$(mktemp)
sed 's/;/  ;/g' $FORMATTED_JAVA_FILE >"$tmpfile" && mv "$tmpfile" $FORMATTED_JAVA_FILE

# ensure the formatting is now incorrect
diff $FORMATTED_JAVA_FILE $BASE_JAVA_FILE
error=$?
if [ $error -eq 0 ]
then
  echo "Formatting search and replace failed"
  exit 1
fi

git add .
mvn -X "io.committed:speedy-spotless-maven-plugin:${SPEEDY_VERSION}:staged"
echo $ROOT_DIR

popd


diff $FORMATTED_JAVA_FILE $BASE_JAVA_FILE
error=$?
if [ $error -eq 0 ]
then
  echo "Pass: $FORMATTED_JAVA_FILE and $BASE_JAVA_FILE are the same"
  exit 0
else
  echo "Fail: $FORMATTED_JAVA_FILE and $BASE_JAVA_FILE are not the same!"
  echo "speedy-spotless:staged did not apply formatting on Example.java correctly. Check the diff above for more information."
  exit 1
fi
