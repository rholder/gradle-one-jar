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
import org.gradle.api.tasks.*
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.file.FileCollection

class OneJar extends Jar {

    @Internal File oneJarBuildDir
    @Internal Logger logger

    @Input boolean useStable = true
    @Input boolean mergeManifestFromJar = false

    // TODO expose One-Jar-Expand functionality
    @Input boolean showExpand = false
    @Input boolean confirmExpand = false

    @Input boolean noClassifier = false;

    @Input String mainClass
    @Optional @InputFile File manifestFile
    @Optional @InputFiles Jar baseJar
    @Optional @InputFiles Configuration targetConfiguration
    @Optional @InputFiles Configuration oneJarConfiguration
    @Optional @InputFiles FileCollection binLib
    @Optional @InputDirectory File binDir
    @Optional @InputDirectory File additionalDir

    OneJar() {
        logger = project.logger
        oneJarBuildDir = new File(new File(project.buildDir, "one-jar-build"), name)
        logger.debug("Created " + oneJarBuildDir.absolutePath)

        description = "Create a One-JAR runnable archive from the current project using a given main Class."

        // use the main project jar if none is specified
        if(!baseJar) {
            baseJar = project.tasks.jar
        }

        // use runtimeClasspath configuration if none is specified
        if(!targetConfiguration) {
            targetConfiguration = project.configurations.runtimeClasspath
        }

        doFirst {
            if (!mainClass) {
                throw new IllegalStateException("The mainClass must be set in order to create a One-JAR archive.")
            }
            oneJarBuildDir.mkdirs()

            // set standalone as classifier if unspecified
            if (!noClassifier && (!archiveClassifier.isPresent() || archiveClassifier.get().isEmpty())) {
                archiveClassifier.set('standalone')
            }
            // unpack OneJar root layout to build dir
            unpackOneJarBoot(oneJarBuildDir.absolutePath)

            // create main/main.jar from the current project's jar
            ant.copy(file: baseJar.archiveFile.get(),
                    toFile: new File(oneJarBuildDir, 'main/main.jar'))

            // copy /lib/* from the current project's runtime dependencies
            def libs = targetConfiguration.resolve()
            libs.each {
                logger.debug("Including dependency: " + it.absolutePath)
                ant.copy(file: it,
                        todir: new File(oneJarBuildDir.absolutePath, "lib"))
            }

            // flatten everything specified in binLib to /binlib/*
            if(binLib) {
                binLib.each {
                    ant.copy(file: it,
                            todir: new File(oneJarBuildDir.absolutePath, "binlib"))
                }
            }

            // copy binDir including sub-folders
            if(binDir && binDir.isDirectory()) {
                logger.debug("Adding all additional binary files found in: " + binDir.absolutePath)
                ant.copy(todir: new File(oneJarBuildDir.absolutePath, "binlib")) {
                    fileset(dir: binDir.absolutePath)
                }
            }

            // copy everything from this dir over the top of the final archive
            if(additionalDir) {
                if(additionalDir.isDirectory()) {
                    logger.debug("Adding all additional files found in: " + additionalDir.absolutePath)
                    ant.copy(todir: oneJarBuildDir.absolutePath) {
                        fileset(dir: additionalDir.absolutePath)
                    }
                }
            }

            File finalJarFile = buildOneJar(baseJar)
            logger.debug("Built One-JAR: " + finalJarFile.absolutePath)
        }
    }

    /**
     * Unpack one-jar-boot to create the one-jar base layout.
     */
    void unpackOneJarBoot(targetDir) {

        // pull one-jar-boot out of the classpath to this file
        def oneJarBootFile = File.createTempFile("one-jar-boot", ".jar")
        oneJarBootFile.deleteOnExit()
        logger.debug("Extacting temporary boot file: " + oneJarBootFile.absolutePath)

        if (oneJarConfiguration && oneJarConfiguration.dependencies.size() == 1){
            def oneJarFile = oneJarConfiguration.singleFile
            logger.debug("Using one-jar-boot from dependency: ${oneJarFile}")

            oneJarBootFile.withOutputStream { output ->
                oneJarFile.withInputStream { input ->
                    output << input
                }
            }
        } else {
            def oneJarBootFilename = useStable ? "one-jar-boot-0.97.1.jar" : "one-jar-boot-0.98.jar"
            Files.outputResourceFromClasspath(oneJarBootFilename, oneJarBootFile)
        }

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
            logger.debug("Using custom manifest file: " + manifestFile.absolutePath)
            targetManifestFile = manifestFile
        } else {
            // merge from Jar or create new empty manifest
            Manifest manifest = mergeManifestFromJar ? jar.manifest.effectiveManifest : this.manifest
            targetManifestFile = writeOneJarManifestFile(manifest)
        }

        File finalJarFile = new File(jar.destinationDirectory.getAsFile().get(), getArchiveFileName().get())
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

        manifest.attributes.put("Main-Class", "com.simontuffs.onejar.Boot")
        manifest.attributes.put("One-Jar-Main-Class", mainClass)
        manifest.attributes.put("One-Jar-Show-Expand", showExpand)
        manifest.attributes.put("One-Jar-Confirm-Expand", confirmExpand)
        manifest.writeTo(manifestFile.path)

        manifestFile
    }
}
