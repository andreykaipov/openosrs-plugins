val g = (gradle as ExtensionAware)

val commonJar = project(":common").tasks["jar"].outputs.files.singleFile
val kotlinStdlib: File = configurations.runtimeClasspath.get().filter { it.path.contains("kotlin-stdlib-${kotlin.coreLibrariesVersion}") }.singleFile

val oprsVersion = g.extra["oprsVersion"] as String
val pf4jVersion = g.extra["pf4jVersion"] as String

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply<MavenPublishPlugin>()

    repositories {
        mavenLocal()
        maven(url = "https://raw.githubusercontent.com/open-osrs/hosting/master")
        maven(url = "https://repo.runelite.net")
        jcenter()
    }
    
    dependencies {
        implementation(project(":common"))

        implementation(group = "com.openosrs", name = "http-api", version = oprsVersion)
        implementation(group = "com.openosrs", name = "runelite-api", version = oprsVersion)
        implementation(group = "com.openosrs", name = "runelite-client", version = oprsVersion)
        implementation(group = "com.openosrs.rs", name = "runescape-api", version = oprsVersion)
        implementation(group = "com.openosrs.rs", name = "runescape-client", version = oprsVersion)

        kapt(group = "org.pf4j", name = "pf4j", version = pf4jVersion)
        implementation(group = "org.pf4j", name = "pf4j", version = pf4jVersion)

        implementation(group = "com.google.inject", name = "guice", version = "4.2.3", classifier = "no_aop")
        implementation(group = "io.reactivex.rxjava3", name = "rxjava", version = "3.0.6")
    }

    println(kotlin.sourceSets["main"].kotlin.srcDirs)

    // Read from source to avoid duplicating this configuration for both OpenOSRS and PF4J manifest metadata
    val id = project.path.substring(1)
    val description = ""
//        let {
//        val f = File("${project.projectDir}/Plugin.kt")
//        val regexp = "description.*=.*\"([^\"]+?)\",".toRegex()
//        regexp.find(f.readText())?.groupValues?.get(1)!!
//    }

    tasks.jar {
        manifest {
            attributes(mapOf(
                "Plugin-Id" to id,
                "Plugin-Description" to description,
                "Plugin-Version" to project.version,
                "Plugin-Provider" to g.extra["Author"],
                "Plugin-License" to g.extra["License"]
            ))
        }

        // Fat jar for the final release
        from(zipTree(commonJar), zipTree(kotlinStdlib))

        doLast {
            // Make the fat jar more accessible
            copy {
                from("$buildDir/libs")
                into("${rootProject.projectDir}/release")
            }

            // Plugins during dev need dependencies inside of $buildDir/deps, akin to the fat jar above. Further,
            // for the client to recognize which projects are plugins, we create a dummy Gradle build script.
            // The PLUGIN_DEVELOPMENT_PATH should be set to "${rootProject.projectDir}/.build".
            copy {
                from(commonJar, kotlinStdlib)
                into("$buildDir/deps/")
            }

            file("$buildDir/../${project.name}.gradle.kts").createNewFile()
        }
    }

    tasks.clean {
        delete("${rootProject.projectDir}/release")
    }
}
