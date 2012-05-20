package com.github.rholder.gradle;

import org.gradle.api.*
import org.gradle.api.tasks.*

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
 * At a minimum, the configuration expects find a custom 'mainClass' when
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

    private Project project
    private GradleOneJarPluginExtension oneJar
    private File oneJarBuildDir

    // TODO comment all the things
    // TODO push to mavenCentral()
    // TODO test task lifecycle for sub-projects
    // TODO integrate external test project
    void apply(Project project) {
        project.apply(plugin:'java')

        this.project = project
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
                buildOneJarMain()
                buildOneJarLib()
                buildOneJar()

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
        project.ant.unzip(
                src: oneJarBootFile.absolutePath,
                dest: oneJarBuildDir.absolutePath,
                failOnEmptyArchive: true,
        ) {
            project.ant.patternset(excludes: 'src/**, boot-manifest.mf')
        }
    }

    /**
     * Build main/main.jar from the current project's jar.
     */
    void buildOneJarMain() {
        // create /main/
        def mainDir = new File(oneJarBuildDir, "main")
        mainDir.mkdirs()

        // create /main/main.jar
        def originalJar = project.tasks.jar.archivePath
        project.logger.info(originalJar.toString())
        def mainFile = new File(mainDir.absolutePath, "main.jar")
        project.ant.copy(file: originalJar, tofile: mainFile.absolutePath)
    }

    /**
     * Build /lib/* from the current project's runtime and compile dependencies
     */
    void buildOneJarLib() {
        // create /lib/
        def libDir = new File(oneJarBuildDir, "lib")
        libDir.mkdirs()

        // create /lib/*.jar from dependencies
        def dependencies = [
                project.configurations.getByName("runtime").resolve(),
                project.configurations.getByName("compile").resolve()
        ].flatten().unique()

        dependencies.findAll { !it.isDirectory() }.each { dep ->
            project.logger.info("Adding ${dep.absolutePath} to One-JAR lib")
            project.ant.copy(file: dep.absolutePath, todir: libDir.absolutePath)
        }
    }

    /**
     * Output the final One-JAR archive to the given file.
     */
    private void buildOneJar() {

        def manifestFile = writeOneJarManifestFile()

        // hack to ensure we get "-standalone.jar" tacked on to archiveName + a valid version
        def jar = project.tasks.jar
        File finalJarFile = new File(jar.destinationDir, jar.archiveName - ("." + jar.extension) + "-standalone." + jar.extension)
        project.ant.jar(destfile: finalJarFile,
                        basedir: oneJarBuildDir.absolutePath,
                        manifest: manifestFile.absolutePath)
    }

    /**
     * Return a manifest configured to boot the jar using One-JAR and then
     * passing over control to the configured main class.
     *
     * @return
     */
    private File writeOneJarManifestFile() {
        def manifestFile = File.createTempFile("one-jar-manifest", "mf")
        manifestFile.deleteOnExit()

        manifestFile.withWriter { writer ->
            // TODO add config for custom manifest file
            def manifest = project.tasks.jar.manifest.effectiveManifest
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
