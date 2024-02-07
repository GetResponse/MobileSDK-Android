pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

rootProject.name = "PushTests"
val notJitpack = System.getenv("JITPACK")?.isEmpty() ?: true

if (notJitpack)
    include(":app")
include(":mobile_sdk")
