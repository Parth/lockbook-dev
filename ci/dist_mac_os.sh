curl -o jpackage.tar.gz $(curl -v --silent https://jdk.java.net/jpackage/ 2>&1 | sed -n 's/.*href="\([^"]*\).*/\1/p' | grep "jpackage+1-.*osx-x64_bin.tar.gz$")
curl -sL https://github.com/shyiko/jabba/raw/master/install.sh | bash && . ~/.jabba/jabba.sh
jabba install adopt@1.12.0-2
jabba use adopt@1.12.0-2
tar -xzf jpackage.tar.gz
BADASS_RUNTIME_JPACKAGE_HOME=jdk-14.jdk/Contents/Home ./gradlew jpackageImage
cd build/jpackage
tar czf lockbook-macos.tar.gz Lockbook.app
