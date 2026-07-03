pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REUSE)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "remote-control-project"
include(":app")