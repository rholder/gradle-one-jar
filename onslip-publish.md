# Hur man bygger

## Utveckling

Peka ut JAR-filen manuellt:

```groovy
buildscript {
    dependencies {
        classpath files('/Users/mb/source/Libs/gradle-one-jar/build/libs/gradle-one-jar-1.0.6.jar')
    }
}

plugins {
    ...
    //    id 'com.github.onslip.gradle-one-jar' version '1.0.5'
}

apply plugin: 'gradle-one-jar'
```

## Publicering

I `~/.gradle/gradle.properties` ska man ha:

```sh
gradle.publish.key=...
gradle.publish.secret=...
sonatypeUsername=lcs
signing.keyId=9C541F6C
signing.secretKeyRingFile=/Users/mb/.gnupg/secring.gpg
```

Om vi nu antar att man vill signera med GPG-nyckeln

```txt
/Users/mb/.gnupg/pubring.gpg
----------------------------
pub   rsa4096 2012-01-26 [SC]
      D430F02A482E66DCB18465C468EB539B9C541F6C
uid           [förbehållslös] Martin Blom <martin.blom@onslip.com>
sub   rsa4096 2012-01-26 [E]
```

### Maven

Skicka upp filerna.

```sh
read -s PASSWORD
read -s KEY
./gradlew uploadArchives -PsonatypePassword=$PASSWORD -Psigning.password=$KEY
```

Logga sedan in på [Nexus Repository Manager](https://oss.sonatype.org/#stagingRepositories), hitta repot, välj *Close*
och sen *Release*. Förr eller senare kommer det då att dyka upp på [Maven Central](https://search.maven.org/artifact/com.github.onslip/gradle-one-jar).

Se [How to publish your open source library to Maven central](https://medium.com/@scottyab/how-to-publish-your-open-source-library-to-maven-central-5178d9579c5)
för mer info.

## Gradle Plugins

Kör

```sh
read -s KEY
gradle publishPlugins -PsonatypePassword= -Psigning.password=$KEY
```

så dyker den direkt upp på [Gradle - Plugin: com.github.onslip.gradle-one-jar](https://plugins.gradle.org/plugin/com.github.onslip.gradle-one-jar).
