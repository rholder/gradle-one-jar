package com.github.rholder.gradle

class GradleOneJarPluginExtension {
    String mainClass
    Boolean useStable = true
    Boolean mergeManifestFromJar = false
    File manifestFile

    // TODO can we eval() this from the current project?
    String finalName = '${project.tasks.jar.baseName}' + "-standalone." + '${project.tasks.jar.extension}'

    // TODO add more one-jar-y parameters for config
    Boolean oneJarShowExpand = false
    Boolean oneJarConfirmExpand = false
}
