curl -O https://download.java.net/java/early_access/jpackage/1/openjdk-14-jpackage+1-49_osx-x64_bin.tar.gz
curl -sL https://github.com/shyiko/jabba/raw/master/install.sh | bash && . ~/.jabba/jabba.sh
jabba install adopt@1.12.0-2
jabba use adopt@1.12.0-2
tar -xzf openjdk-14-jpackage+1-49_osx-x64_bin.tar.gz
BADASS_RUNTIME_JPACKAGE_HOME=jdk-14.jdk/Contents/Home ./gradlew jpackageImage
cd build/jpackage
tar czf lockbook-macos.tar.gz Lockbook.app
