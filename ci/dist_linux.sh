curl -O https://download.java.net/java/early_access/jpackage/1/openjdk-14-jpackage+1-49_linux-x64_bin.tar.gz
tar -xzf openjdk-14-jpackage+1-49_linux-x64_bin.tar.gz
BADASS_RUNTIME_JPACKAGE_HOME=jdk-14 ./gradlew jpackageImage
cd build/jpackage
tar czf lockbook-linux.tar.gz Lockbook
