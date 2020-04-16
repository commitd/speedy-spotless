BASE_DIR=$(pwd)
ROOT_DIR=$(mktemp -d -t speedy-XXXXXXXXXX)

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

git init
git add .
git commit -m initial
sed -i "" 's:\/\/ modline1:String s="";:g' "src/main/java/io/committed/speedy/Example.java"
git add .

mvn -X io.committed:speedy-spotless-maven-plugin:0.1.1:staged
cat src/main/java/io/committed/speedy/Example.java
cat pom.xml

popd

rm -rf
