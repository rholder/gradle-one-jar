package com.github.rholder.gradle;


import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.java.archives.Manifest
import org.gradle.api.java.archives.internal.DefaultManifest
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.logging.Logger

/**
 * This plugin rolls up your current project's jar and all of its dependencies
 * into the the layout expected by One-JAR, producing a single runnable
 * fat-jar, similar to the following:
 *
 * <pre>
 * my-awesome-thing-standalone.jar
 * |  META-INF/MANIFEST.MF
 * |  .version
 * |  com/simontuffs/onejar
 * |  Boot.class, ...etc.
 * |  doc/one-jar-license.txt
 * |  main/main.jar
 * |  lib/a.jar ...etc.
 * </pre>
 *
 * At a minimum, the configuration expects to find a custom 'mainClass' when
 * adding the plugin to your own builds, as in:
 *
 * <pre>
 * apply plugin: 'gradle-one-jar'
 *
 * oneJar {
 *     mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
 * }
 * </pre>
 */
class GradleOneJarPlugin implements Plugin<Project> {

    private GradleOneJarPluginExtension oneJar
    private File oneJarBuildDir
    private Logger logger
    private AntBuilder ant

    // TODO comment all the things
    // TODO push to mavenCentral()
    // TODO test task lifecycle for sub-projects
    // TODO integrate external test project
    void apply(Project project) {
        project.apply(plugin:'java')

        this.logger = project.logger
        this.ant = project.ant
        this.oneJar = project.extensions.create("oneJar", GradleOneJarPluginExtension)
        this.oneJarBuildDir = new File(project.buildDir, "one-jar-build")

        project.task('oneJar', dependsOn: [project.tasks.jar]) {
            description = "Create a One-JAR runnable archive from the current project."

            inputs.files([project.tasks.jar.outputs.files, project.configurations.getByName("compile"), project.configurations.getByName("runtime")])
            doFirst {
                if (!oneJar.mainClass) {
                    throw new IllegalStateException("The mainClass must be set in order to create a One-JAR archive.")
                }
                unpackOneJarBoot()
                buildOneJarMain(project.tasks.jar)
                buildOneJarLib(project)
                buildOneJar(project.tasks.jar)

                // TODO one-jar artifact attachment
            }
        }

        project.task('cleanOneJar', type: Delete) {
            description = "Clean up the temporary One-JAR build directory."
            delete oneJarBuildDir
        }
    }

    /**
     * Unpack one-jar-boot to create the one-jar base layout.
     */
    void unpackOneJarBoot() {
        // create /
        oneJarBuildDir.mkdirs()

        // extract one-jar-boot to /
        def oneJarBootFile = File.createTempFile("one-jar-boot", ".jar")
        oneJarBootFile.deleteOnExit()

        // TODO add ability to set your own custom one-jar-boot jar
        def oneJarBootFilename = oneJar.useStable ? "one-jar-boot-0.97.jar" : "one-jar-boot-0.98.jar"
        outputResourceFromClasspath(oneJarBootFilename, oneJarBootFile)
        ant.unzip(
                src: oneJarBootFile.absolutePath,
                dest: oneJarBuildDir.absolutePath,
                failOnEmptyArchive: true,
        ) {
            ant.patternset(excludes: 'src/**, boot-manifest.mf')
        }
    }

    /**
     * Build main/main.jar from the current project's jar.
     */
    void buildOneJarMain(Jar jar) {
        // create /main/
        def mainDir = new File(oneJarBuildDir, "main")
        mainDir.mkdirs()

        // create /main/main.jar
        def originalJar = jar.archivePath
        def mainFile = new File(mainDir.absolutePath, "main.jar")
        ant.copy(file: originalJar, tofile: mainFile.absolutePath)
    }

    /**
     * Build /lib/* from the current project's runtime and compile dependencies
     */
    void buildOneJarLib(Project project) {
        // create /lib/
        def libDir = new File(oneJarBuildDir, "lib")
        libDir.mkdirs()

        // create /lib/*.jar from dependencies
        def dependencies = [
                project.configurations.getByName("runtime").resolve(),
                project.configurations.getByName("compile").resolve()
        ].flatten().unique()

        dependencies.findAll { !it.isDirectory() }.each { dep ->
            logger.info("Adding ${dep.absolutePath} to One-JAR lib")
            ant.copy(file: dep.absolutePath, todir: libDir.absolutePath)
        }
    }

    /**
     * Output the final One-JAR archive to the given file.
     */
    private void buildOneJar(Jar jar) {

        // NOTE: if using your own custom manifest, you're responsible for adding entries for One-JAR boot
        File manifestFile
        if(oneJar.manifestFile) {
            logger.info("Using custom manifest file: " + oneJar.manifestFile.absolutePath)
            manifestFile = oneJar.manifestFile
        } else {
            // merge from Jar or create new empty manifest
            Manifest manifest = oneJar.mergeManifestFromJar ? jar.manifest.effectiveManifest : new DefaultManifest(null)
            manifestFile = writeOneJarManifestFile(manifest)
        }

        // hack to ensure we get "-standalone.jar" tacked on to archiveName + a valid version
        File finalJarFile = new File(jar.destinationDir, jar.archiveName - ("." + jar.extension) + "-standalone." + jar.extension)
        ant.jar(destfile: finalJarFile,
                basedir: oneJarBuildDir.absolutePath,
                manifest: manifestFile.absolutePath)
    }

    /**
     * Return a manifest configured to boot the jar using One-JAR and then
     * passing over control to the configured main class.
     *
     * @return
     */
    private File writeOneJarManifestFile(Manifest manifest) {
        File manifestFile = File.createTempFile("one-jar-manifest", "mf")
        manifestFile.deleteOnExit()

        manifestFile.withWriter { writer ->
            manifest.attributes.put("Main-Class", "com.simontuffs.onejar.Boot")
            manifest.attributes.put("One-Jar-Main-Class", oneJar.mainClass)
            manifest.attributes.put("One-Jar-Show-Expand", oneJar.oneJarShowExpand)
            manifest.attributes.put("One-Jar-Confirm-Expand", oneJar.oneJarConfirmExpand)
            manifest.writeTo(writer)
        }
        return manifestFile
    }

    /**
     * Pull a resource out of the current classpath and write a copy of it to
     * the given location.
     *
     * @param classpathName
     * @param outputFile
     */
    private void outputResourceFromClasspath(String classpathName, File outputFile) {
        outputFile.delete()
        outputFile.withOutputStream { os ->
            os << this.class.getResourceAsStream("/${classpathName}")
        }
    }
}
