curl -o jpackage.tar.gz https://download.java.net/java/early_access/jdk14/31/GPL/openjdk-14-ea+31_linux-x64_bin.tar.gz
tar -xzf jpackage.tar.gz
BADASS_RUNTIME_JPACKAGE_HOME=jdk-14 ./gradlew jpackageImage
cd build/jpackage
tar czf lockbook-linux.tar.gz Lockbook
