rootProject.name = "Andrey's OpenOSRS Plugins"

includeBuild("client")

file("plugins").list()?.forEach {
    println("Found plugin $it")
    include(":$it")
    project(":$it").apply {
        projectDir = file("plugins/$name")
        buildFileName = "$name.gradle.kts"

        require(projectDir.isDirectory) { "Project '${path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${path} must have a $buildFile build script" }
    }
}

pluginManagement.resolutionStrategy.eachPlugin {
    if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
        useVersion("1.4.10")
    }
}
