pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

rootProject.name = "intellij"

val pomXml = file("../pom.xml")
val pomVersion = groovy.xml.XmlSlurper().parse(pomXml)
    .getProperty("version")
    .toString()

gradle.extra["projectVersion"] = pomVersion