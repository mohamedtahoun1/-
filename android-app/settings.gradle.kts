pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PRESERVE_SHARED_REPOSITORIES)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "remote-control-project"
include(":app")
