rootProject.name = "Andrey's OpenOSRS Plugins"

File("plugins").list().forEach {
    println("Found plugin $it")
    include(":$it")

    project(":$it").apply {
        projectDir = File("plugins/$it")
        buildFileName = "build.gradle.kts"
    }
}