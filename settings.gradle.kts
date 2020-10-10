rootProject.name = "Andrey's OpenOSRS Plugins"

includeBuild("client")

file("plugins").list()?.forEach {
    println("Found plugin $it")
    include(":$it")
    project(":$it").projectDir = file("plugins/$it")
}

pluginManagement.resolutionStrategy.eachPlugin {
    if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
        useVersion("1.4.10")
    }
}
