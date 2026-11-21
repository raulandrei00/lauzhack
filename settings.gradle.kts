pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Playwise"
include(":composeApp")
include(":features:graph")
include(":features:game-theory")
include(":features:probability")
