val g = (gradle as ExtensionAware)

subprojects {
    buildDir = file("${rootProject.projectDir}/build/plugins/${project.name}/build")

    // Read from source to avoid duplicating this configuration for both OpenOSRS and pf4j manifest metadata
    val id = project.path.substring(1)
    val description = try {
        val f = File("${project.projectDir}/src/Plugin.kt")
        val regexp = "description.*=.*\"([^\"]+?)\",".toRegex()
        regexp.find(f.readText())?.groupValues?.get(1)!!
    } catch (_: Exception) {
        "n/a"
    }

    dependencies {
        implementation(project(":lib"))
    }

    tasks.jar {
        manifest {
            attributes(mapOf(
                "Plugin-Id" to id,
                "Plugin-Version" to project.version,
                "Plugin-Description" to description,
                "Plugin-Provider" to g.extra["Author"],
                "Plugin-License" to g.extra["License"]
            ))
        }

        val commonJar: File = project(":lib").tasks["jar"].outputs.files.singleFile
        val kotlinStdlib: File = configurations.runtimeClasspath.get().filter { it.path.contains("kotlin-stdlib-${kotlin.coreLibrariesVersion}") }.singleFile

        // For final release jar
        from(zipTree(commonJar), zipTree(kotlinStdlib))

        doLast {
            // For hot-reloading during development
            file("$buildDir/../${project.name}.gradle.kts").createNewFile()
            copy {
                from(commonJar, kotlinStdlib)
                into("$buildDir/deps/")
            }

            // For convenience; move the fat jar out of nested build dirs
            copy {
                from("$buildDir/libs")
                into("${rootProject.projectDir}/release")
            }
        }
    }
}

