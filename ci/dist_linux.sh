curl -o jpackage.tar.gz $(curl -v --silent https://jdk.java.net/jpackage/ 2>&1 | sed -n 's/.*href="\([^"]*\).*/\1/p' | grep "jpackage+1-.*linux-x64_bin.tar.gz$")
tar -xzf jpackage.tar.gz
BADASS_RUNTIME_JPACKAGE_HOME=jdk-14 ./gradlew jpackageImage
cd build/jpackage
tar czf lockbook-linux.tar.gz Lockbook
