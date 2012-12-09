package com.github.rholder.gradle.task

import com.github.rholder.gradle.util.Files
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.api.logging.Logger
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.artifacts.PublishArtifact

class OneJar extends Jar {

    File oneJarBuildDir
    Logger logger
    AntBuilder ant

    boolean useStable = true
    boolean showExpand = false
    boolean confirmExpand = false
    boolean mergeManifestFromJar = false

    String classifier = 'standalone'

    String mainClass
    File manifestFile
    Jar baseJar

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
        dependsOn = [baseJar]

        inputs.files([baseJar.getArchivePath().absoluteFile])
        outputs.files([new File(baseJar.getArchivePath().parentFile.absolutePath, generateFilename(baseJar, classifier))])

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

            // TODO allow other configurations to be passed in
            // copy /lib/* from the current project's runtime dependencies
            def libs = project.configurations.runtime.resolve()
            logger.info("Including dependencies: " + libs)
            libs.each {
                new AntBuilder().copy(file: it,
                        todir: new File(oneJarBuildDir.absolutePath, "lib"))
            }

            File finalJarFile = buildOneJar(baseJar)
            logger.info("Building One-JAR: " + finalJarFile.absolutePath)
            Date date = new Date()
            String name = baseJar.baseName
            project.artifacts.add('archives',
                    [
                            getClassifier: { -> classifier },
                            getDate: {-> date },
                            getExtension: {-> "jar" },
                            getType: {-> "jar" },
                            getFile: {-> finalJarFile },
                            getName: {-> name }
                    ] as PublishArtifact
            )
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
        def oneJarBootFilename = useStable ? "one-jar-boot-0.97.jar" : "one-jar-boot-0.98.jar"
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
            Manifest manifest = mergeManifestFromJar ? jar.manifest.effectiveManifest : new DefaultManifest(null)
            targetManifestFile = writeOneJarManifestFile(manifest)
        }


        File finalJarFile = new File(jar.destinationDir, generateFilename(jar, classifier))
        //File finalJarFile = new File(jar.archivePath.parentFile.absolutePath, 'one-jar.jar')
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
            manifest.attributes.put("Created-By", "Gradle OneJar Task")
            manifest.writeTo(writer)
        }
        return manifestFile
    }
}
