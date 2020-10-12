rootProject.name = "Andrey's OpenOSRS Plugins"

(gradle as ExtensionAware).apply {
    extra["Author"] = "Andrey Kaipov"
    extra["Repository"] = "https://github.com/andreykaipov/osrs-plugins"
    extra["License"] = "2-Clause BSD License"
}

includeBuild("client")


listOf("lib", "plugins").forEach {
    include(":$it")
    project(":$it").buildFileName = "build.gradle.kts"
}

file("plugins").list()?.filter { it != "build.gradle.kts" }?.forEach {
    println("Found plugin $it")
    include(":plugins:$it")
    project(":plugins:$it").projectDir = file("plugins/$it")
}

pluginManagement.resolutionStrategy.eachPlugin {
    if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
        useVersion("1.4.10")
    }
}
