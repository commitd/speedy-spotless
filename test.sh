#!/usr/bin/env bash

mvn -B install -DskipTests

BASE_DIR=$(pwd)
ROOT_DIR=$(mktemp -d -t speedy-XXXXXXXXXX)
SPEEDY_VERSION=`mvn help:evaluate -Dexpression=project.version -q -DforceStdout`

cp -r examples/multi-module $ROOT_DIR/partial

pushd $ROOT_DIR

cd partial

JAVA_FILE="src/main/java/io/committed/multimodule/Example.java"
FORMATTED_JAVA_FILE=$ROOT_DIR/partial/child/$JAVA_FILE
BASE_JAVA_FILE=$BASE_DIR/examples/multi-module/child/$JAVA_FILE

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

# run formatter from child module
pushd child
mvn -X "io.committed:speedy-spotless-maven-plugin:${SPEEDY_VERSION}:staged"
popd

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
