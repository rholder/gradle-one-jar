/*
 * Copyright 2013 Ray Holder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.rholder.gradle.task

import com.github.rholder.gradle.util.Files
import org.gradle.api.artifacts.Configuration
import org.gradle.api.java.archives.Manifest
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.bundling.Jar

class OneJar extends Jar {

    File oneJarBuildDir
    Logger logger
    AntBuilder ant

    boolean useStable = true
    boolean mergeManifestFromJar = false

    // TODO expose One-Jar-Expand functionality
    boolean showExpand = false
    boolean confirmExpand = false

    String mainClass
    File manifestFile
    Jar baseJar
    Configuration targetConfiguration

    OneJar() {

        logger = project.logger
        ant = project.ant
        oneJarBuildDir = new File(project.buildDir, "one-jar-build")
        logger.info("Created " + oneJarBuildDir.absolutePath)

        description = "Create a One-JAR runnable archive from the current project using a given main Class."

        // use the main project jar if none is specified
        if(!baseJar) {
            baseJar = project.tasks.jar
        }

        // use runtime configuration if none is specified
        if(!targetConfiguration) {
            targetConfiguration = project.configurations.runtime
        }

        // set standalone as classifier if unspecified
        if(!classifier || classifier.isEmpty()) {
            classifier = 'standalone'
        }

        dependsOn = [baseJar]

        inputs.files([baseJar.getArchivePath().absoluteFile])
        outputs.file(new File(baseJar.getArchivePath().parentFile.absolutePath, generateFilename(baseJar, getClassifier())))

        doFirst {
            if (!mainClass) {
                throw new IllegalStateException("The mainClass must be set in order to create a One-JAR archive.")
            }
            oneJarBuildDir.mkdirs()

            // unpack OneJar root layout to build dir
            unpackOneJarBoot(oneJarBuildDir.absolutePath)

            // create main/main.jar from the current project's jar
            new AntBuilder().copy(file: baseJar.archivePath.absolutePath,
                    toFile: new File(oneJarBuildDir, 'main/main.jar'))

            // copy /lib/* from the current project's runtime dependencies
            def libs = targetConfiguration.resolve()
            logger.info("Including dependencies: " + libs)
            libs.each {
                new AntBuilder().copy(file: it,
                        todir: new File(oneJarBuildDir.absolutePath, "lib"))
            }

            File finalJarFile = buildOneJar(baseJar)
            logger.info("Built One-JAR: " + finalJarFile.absolutePath)
        }
    }

    /**
     * This is kind of a hack to ensure we get "-classifier.jar" tacked on to
     * archiveName + a valid version.
     */
    static String generateFilename(Jar jar, String classifier) {
        return jar.archiveName - ("." + jar.extension) + "-" + classifier + "." + jar.extension
    }

    /**
     * Unpack one-jar-boot to create the one-jar base layout.
     */
    void unpackOneJarBoot(targetDir) {

        // pull one-jar-boot out of the classpath to this file
        def oneJarBootFile = File.createTempFile("one-jar-boot", ".jar")
        oneJarBootFile.deleteOnExit()
        logger.info("Extacting temporary boot file: " + oneJarBootFile.absolutePath)

        // TODO add ability to set your own custom one-jar-boot jar
        def oneJarBootFilename = useStable ? "one-jar-boot-0.97.1.jar" : "one-jar-boot-0.98.jar"
        Files.outputResourceFromClasspath(oneJarBootFilename, oneJarBootFile)

        ant.unzip(
                src: oneJarBootFile.absolutePath,
                dest: targetDir,
                failOnEmptyArchive: true,
        ) {
            ant.patternset(excludes: 'src/**, boot-manifest.mf')
        }
    }

    /**
     * Return the destination File for the output of the final One-JAR archive..
     */
    File buildOneJar(Jar jar) {

        // NOTE: if using your own custom manifest, you're responsible for adding entries for One-JAR boot
        File targetManifestFile
        if(manifestFile) {
            logger.info("Using custom manifest file: " + manifestFile.absolutePath)
            targetManifestFile = manifestFile
        } else {
            // merge from Jar or create new empty manifest
            Manifest manifest = mergeManifestFromJar ? jar.manifest.effectiveManifest : this.manifest
            targetManifestFile = writeOneJarManifestFile(manifest)
        }


        File finalJarFile = new File(jar.destinationDir, generateFilename(jar, getClassifier()))
        ant.jar(destfile: finalJarFile,
                basedir: oneJarBuildDir.absolutePath,
                manifest: targetManifestFile.absolutePath)
        return finalJarFile
    }

    /**
     * Return a manifest configured to boot the jar using One-JAR and then
     * passing over control to the configured main class.
     */
    File writeOneJarManifestFile(Manifest manifest) {
        File manifestFile = File.createTempFile("one-jar-manifest", ".mf")
        manifestFile.deleteOnExit()

        manifestFile.withWriter { writer ->
            manifest.attributes.put("Main-Class", "com.simontuffs.onejar.Boot")
            manifest.attributes.put("One-Jar-Main-Class", mainClass)
            manifest.attributes.put("One-Jar-Show-Expand", showExpand)
            manifest.attributes.put("One-Jar-Confirm-Expand", confirmExpand)
            manifest.writeTo(writer)
        }
        return manifestFile
    }
}
