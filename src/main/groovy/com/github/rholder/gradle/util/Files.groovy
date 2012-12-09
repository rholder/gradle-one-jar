package com.github.rholder.gradle.util

class Files {

    /**
     * Pull a resource out of the current classpath and write a copy of it to
     * the given location.
     *
     * @param classpathName
     * @param outputFile
     */
    static void outputResourceFromClasspath(String classpathName, File outputFile) {
        outputFile.delete()
        outputFile.withOutputStream { os ->
            os << Files.class.getResourceAsStream("/${classpathName}")
        }
    }
}
