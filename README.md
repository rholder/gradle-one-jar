## What is this?
This plugin rolls up your current project's jar and all of its dependencies
into the the layout expected by One-JAR, producing a single runnable
fat-jar, similar to the following:

    my-awesome-thing-1.2.3-standalone.jar
    |
    +---- com
    |   +---- simontuffs
    |       +---- onejar
    |           +---- Boot.class
    |           +---- (etc., etc.)
    |           +---- OneJarURLConnection.class
    +---- doc
    |   +---- one-jar-license.txt
    +---- lib
    |   +---- other-cool-lib-1.7.jar
    |   +---- some-cool-lib-2.5.jar
    +---- main
    |   +-- main.jar
    +---- META-INF
    |   +---- MANIFEST.MF
    +---- OneJar.class
    +---- .version

You can read more about the layout of a One-JAR archive from the official site
[here](http://one-jar.sourceforge.net/).

##Quick Start
First, you'll want to add the plugin to your build, as in:

    apply plugin: 'gradle-one-jar'

    buildscript {
        repositories {
            mavenCentral()
        }
        dependencies {
            classpath 'com.github.rholder:gradle-one-jar:1.0.1'
        }
    }

Then, at a minimum, the configuration expects to find a custom 'mainClass' when
adding your own task, as in:

    task awesomeFunJar(type: OneJar) {
        mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
    }

Then you can run the task with:

    gradle awesomeFunJar

The end result will be a new build artifact with the `standalone` classifier
that should be suitable for publishing to a repository, etc. via:

    artifacts {
        archives awesomeFunJar
    }

##Advanced Features
The current incarnation of the `gradle-one-jar` plugin exists as a highly
configurable Gradle task implementation built as an extension of the built-in
`Jar` task. The following is a non-exhaustive list of some of the more advanced
features that the plugin can perform to meet the varying needs of deploying
standardized artifacts.

###Selectable One-JAR version
By default, the `one-jar-boot` version used is the stable
`one-jar-boot-0.97.jar` which is available from the One-JAR homepage. However,
if you'd prefer to use the latest development version `one-jar-boot-0.98.jar`
(last updated 2010-08-25) then you can do so with the following:

    task awesomeFunJar(type: OneJar) {
        mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
        useStable = false
    }

###Use custom configuration for dependencies
By default, the plugin uses the current project's `runtime` configuration to
resolve which dependencies are to be included in the final One-JAR archive. If
you would rather use your own custom configuration, you can set it as follows in
the task:

    // add your own configuration
    configurations {
        fatJarBuild
    }

    // declare dependencies for this configuration
    dependencies {
        // only for compile
        compile 'org.slf4j:slf4j-api:1.7.2'

        // dependencies in fat jar
        fatJarBuild 'org.slf4j:slf4j-api:1.7.2'
        fatJarBuild 'org.slf4j:slf4j-simple:1.7.2'
    }

    // override target configuration
    task awesomeFunJar(type: OneJar) {
        mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
        targetConfiguration = configurations.fatJarBuild
    }

###Custom MANIFEST.MF entries
By default, the MANIFEST.MF added to the final One-JAR archive contains only the
bare minimum number of attributes expected for `one-jar-boot` to behave
correctly.  You can add your own custom attributes to the `manifest` property of
a `OneJar` task just like a `Jar` task, such as in:

    task awesomeFunJar(type: OneJar) {
        mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
        manifest {
            attributes 'Timestamp': String.valueOf(System.currentTimeMillis())
            attributes 'ContainsXML': 'No'
        }
    }

###Merge base `Jar` task MANIFEST.MF entries
If you just want all of the MANIFEST.MF entries that are present in your
project's `Jar` task to be merged with the default entries needed for
`one-jar-boot` in the final archive, then you can do so with:

    task awesomeFunJar(type: OneJar) {
        mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
        mergeManifestFromJar = true
    }

###Add your own custom root MANIFEST.MF
If you just want total control over the MANIFEST.MF being used in the final
One-JAR archive, you can override the MANIFEST.MF entry and instead provide your
own custom manifest file with the following:

    task awesomeFunJar(type: OneJar) {
        mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
        manifestFile = file('custom/MY-CUSTOM-MANIFEST.MF')
    }

You should note however, that if you decide to do this, you'll need to provide
the entries that are expected by `one-jar-boot` yourself:

    Main-Class: com.simontuffs.onejar.Boot
    One-Jar-Main-Class: com.github.rholder.awesome.MyAwesomeMain
    One-Jar-Show-Expand: false
    One-Jar-Confirm-Expand: false
    Created-By: rholder

###Add native libraries
Files added to the `/binlib` directory within an archive get expanded to a
temporary directory on startup, and the One-JAR JarClassLoader loads them
automatically. To get your own native library files included in your archive,
try something like this:

    task awesomeFunJar(type: OneJar) {
        mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
        binLib = files('libFoo.so')
    }

###Add any files to the root archive
If you just want to be able to drop arbitrary files into the root of the
generated archive, then you can specify a directory (which will also include its
children) to be copied over the top of the the existing files with:

    task awesomeFunJar(type: OneJar) {
        mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
        additionalDir = file('someDirFilledWithGoodies')
    }

###Override the base Jar task
By default, the current project's `Jar` task (which is made available when
applying the `java` plugin and exposed as `jar`) is where a `OneJar` task pulls
its raw compiled class and resource information to create the `main/main.jar`
entry in the final One-JAR archive. However, it is possible to override this
default with:

    task awesomeFunJar(type: OneJar) {
        mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
        baseJar = someOtherJarTask
    }

I'd consider this experimental functionality. If you find yourself needing to do
this for some reason, you might also consider just setting up a multi-module
Gradle project with a nearly empty One-JAR creator project (which would in turn
create a nearly empty `main/main.jar`). This way you could create One-JAR
archives with custom configurations, mainClass, etc., by simply creating
separate `OneJar` tasks that were dependent on your other modules without
having to worry about customizations for specific independent `Jar`
configurations since they could be made to explicitly include whichever build
was necessary.

##Building from source
The `gradle-one-jar` build plugin uses a [Gradle](http://gradle.org)-based build system. In the instructions
below, [`./gradlew`](http://vimeo.com/34436402) is invoked from the root of the source tree and serves as
a cross-platform, self-contained bootstrap mechanism for the build. The only
prerequisites are [Git](https://help.github.com/articles/set-up-git) and JDK 1.6+.

### check out sources
`git clone git://github.com/rholder/gradle-one-jar.git`

### compile and test, build all jars
`./gradlew build`

### install all jars into your local Maven cache
`./gradlew install`

##License
The `gradle-one-jar` build plugin is released under version 2.0 of the
[Apache License](http://www.apache.org/licenses/LICENSE-2.0). Distributions
built with this plugin are subject to the terms set forth
[here](http://one-jar.sourceforge.net/index.php?page=documents&file=license).
The One-JAR license is a BSD-style license. Compliance with this license is
assured by including the one-jar-license.txt file in the One-JAR archive, which
this plugin does automatically.

##Contributors
* Jochen Schalanda (joschi)
