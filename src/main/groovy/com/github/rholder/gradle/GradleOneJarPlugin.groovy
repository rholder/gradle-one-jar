package com.github.rholder.gradle;

import org.gradle.api.Plugin
import org.gradle.api.Project
import com.github.rholder.gradle.task.OneJar

/**
 * This plugin rolls up your current project's jar and all of its dependencies
 * into the the layout expected by One-JAR, producing a single runnable
 * fat-jar, similar to the following:
 *
 * <pre>
 * my-awesome-thing-1.2.3-standalone.jar
 * |
 * +---- com
 * |   +---- simontuffs
 * |       +---- onejar
 * |           +---- Boot.class
 * |           +---- (etc., etc.)
 * |           +---- OneJarURLConnection.class
 * +---- doc
 * |   +---- one-jar-license.txt
 * +---- lib
 * |   +---- other-cool-lib-1.7.jar
 * |   +---- some-cool-lib-2.5.jar
 * +---- main
 * |   +-- main.jar
 * +---- META-INF
 * |   +---- MANIFEST.MF
 * +---- OneJar.class
 * +---- .version
 *
 * </pre>
 *
 * At a minimum, the configuration expects to find a custom 'mainClass' when
 * adding the plugin to your own builds, as in:
 *
 * <pre>
 * apply plugin: 'gradle-one-jar'
 *
 * task awesomeFunJar(type: OneJar) {
 *     mainClass = 'com.github.rholder.awesome.MyAwesomeMain'
 * }
 *
 * </pre>
 */
class GradleOneJarPlugin implements Plugin<Project> {

    // TODO comment all the things
    // TODO push to mavenCentral()
    // TODO test task lifecycle for sub-projects
    // TODO integrate external test project
    void apply(Project project) {
        project.apply(plugin:'java')
        project.ext.OneJar = OneJar.class
    }
}
