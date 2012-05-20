package com.github.rholder.gradle

class GradleOneJarPluginExtension {
    def mainClass
    def useStable = true

    // TODO can we eval() this from the current project?
    def finalName = '${project.tasks.jar.baseName}' + "-standalone." + '${project.tasks.jar.extension}'

    // TODO add more one-jar-y parameters for config
    def oneJarShowExpand = false
    def oneJarConfirmExpand = false
}
