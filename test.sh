BASE_DIR=$(pwd)
ROOT_DIR=$(mktemp -d -t speedy-XXXXXXXXXX)

pushd $ROOT_DIR

mvn archetype:generate -DgroupId=test -DartifactId=partial -DarchetypeArtifactId=maven-archetype-quickstart -DinteractiveMode=false
cd partial
 
mkdir -p src/main/java/io/committed/speedy
cp $BASE_DIR/src/main/java/io/committed/speedy/Example.java src/main/java/io/committed/speedy
git init
git add .
git commit -m initial
sed -i "" 's:\/\/ modline1:String s="";:g' "src/main/java/io/committed/speedy/Example.java"
git add .

mvn -X io.committed:spotless-speedy-maven-plugin:0.0.1-SNAPSHOT:format-staged
cat src/main/java/io/committed/speedy/Example.java

find .

popd
rm -rf $tmp_dir