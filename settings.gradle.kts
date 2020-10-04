rootProject.name = "Andrey's OpenOSRS Plugins"

file("plugins").list().forEach {
    println("Found plugin $it")
    include(":$it")
}

/**
 * To make plugin hotswapping work during development, each plugin's project
 * directory must contain a build file of the name $name.gradle.kts because
 * the client's ExternalPluginFilter uses it to discover valid plugin project
 * directories.
 *
 * See https://github.com/open-osrs/runelite/blob/e967884e48d9c0c78f9595105281f37c0fa793c9/runelite-client/src/main/java/net/runelite/client/plugins/ExternalPluginFileFilter.java#L24-L26.
 */
rootProject.children.forEach {
    it.apply {
        projectDir = file("plugins/$name")
        buildFileName = "$name.gradle.kts"

        require(projectDir.isDirectory) { "Project '${path} must have a $projectDir directory" }
        require(buildFile.isFile) { "Project '${path} must have a $buildFile build script" }
    }
}
