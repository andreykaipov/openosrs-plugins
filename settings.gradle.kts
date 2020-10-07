rootProject.name = "Andrey's OpenOSRS Plugins"

(gradle as ExtensionAware).apply {
    extra["Author"] = "Andrey Kaipov"
    extra["Repository"] = "https://github.com/andreykaipov/osrs-plugins"
    extra["License"] = "2-Clause BSD License"

    extra["oprsVersion"] = "3.4.5"
    extra["pf4jVersion"] = "3.4.1"
}

pluginManagement.resolutionStrategy.eachPlugin {
    if (requested.id.id.startsWith("org.jetbrains.kotlin.")) {
        useVersion("1.4.10")
    }
}

listOf("common", "plugins").forEach {
    include(":$it")
    project(":$it").buildFileName = "$it.gradle.kts"
}

file("plugins").list()!!.filter { it != "plugins.gradle.kts" && it != ".build" }.forEach {
    include(":plugins:$it")
    project(":plugins:$it").apply {
        buildFileName = "$it.gradle.kts"
        projectDir = file("plugins/$name")
        require(projectDir.isDirectory) { "Expected directory ${projectDir.relativeTo(rootProject.projectDir)} for plugin '$name'" }

        val pluginFile = file("$projectDir/Plugin.kt")
//        require(pluginFile.exists()) { "Expected file ${pluginFile.relativeTo(rootProject.projectDir)} for plugin '$name'" }
    }
}
