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

## Gradle Plugins

Kör

```sh
read -s KEY
gradle publishPlugins -Psigning.password=$KEY
```

så dyker den direkt upp på [Gradle - Plugin: com.github.onslip.gradle-one-jar](https://plugins.gradle.org/plugin/com.github.onslip.gradle-one-jar).
