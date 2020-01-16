curl -o jpackage.tar.gz https://download.java.net/java/early_access/jdk14/31/GPL/openjdk-14-ea+31_osx-x64_bin.tar.gz
curl -sL https://github.com/shyiko/jabba/raw/master/install.sh | bash && . ~/.jabba/jabba.sh
jabba install adopt@1.12.0-2
jabba use adopt@1.12.0-2
tar -xzf jpackage.tar.gz
BADASS_RUNTIME_JPACKAGE_HOME=jdk-14.jdk/Contents/Home ./gradlew jpackageImage
cd build/jpackage
tar czf lockbook-macos.tar.gz Lockbook.app
