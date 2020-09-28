rootProject.name = "Andrey's OpenOSRS Plugins"

file("plugins").list().forEach {
    println("Found plugin $it")
    include(":$it")
}

rootProject.children.forEach {
    it.apply {
        projectDir = file("plugins/$name")
        buildFileName = "$name.gradle.kts"

        require(projectDir.isDirectory) { "Project '${path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${path} must have a $buildFile build script" }
    }
}
