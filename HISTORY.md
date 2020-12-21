## 1.0.6 - 2020-12-21

* This is (still) an Onslip fork.
* Merged PR #3: Allow empty classifier.
* Merged PR #4: Remove redundant output declaration.

## 1.0.5 - 2017-08-12

* This is an Onslip fork.
* Added binDir property to include a file hierarchy in binlib. This is required
  when having JNI modules for multiple systems and architectures.
* Applied PR rholder/gradle-one-jar#32: Stop using deprecated
  Manifest.writeTo(Writer) and using Manifest.writeTo(Object) instead (alisiikh,
  PR #1)

## 1.0.4 - 2014-01-25

* Bump Gradle wrapper up to 1.10
* Add notes about ClassLoader support to README.md (ben-manes, walec51, issue #11)
* Possible fix for dependent subproject rebuilding (issue #12)
* Add ability to change final artifact name to align with builtin Jar task (issue #14)

## 1.0.3 - 2013-02-28

* Support for custom one-jar-boot.jar via Gradle dependency (squiddle)

## 1.0.2 - 2013-02-20

* Add binLib and additionalDir functionality, similar to binlib and fileset tags in the One-JAR Ant task (rholder, issue #5)
* Update one-jar-boot 0.97 to version 0.97.1 (joschi, issue #4)

## 1.0.1 - 2013-02-02

* Bump Gradle wrapper 1.3 to 1.4 (joschi)
* Fix to remove automatic artifact publishing (joschi, issue #3)
* Adding HISTORY.md

## 1.0.0 - 2013-01-13

* Stable release, packaging for Maven Central
* Fixes for artifact publishing (joschi)
* Documentation for all the things

## 0.0.1-beta - 2012-12-09

* Refactor everything to become a Gradle Task

## 0.0.1-alpha - 2012-05-19

* First commit, appears to be mostly working
