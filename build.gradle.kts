extra["Author"] = "Andrey Kaipov"
extra["Repository"] = "https://github.com/andreykaipov/osrs-plugins"
extra["License"] = "2-Clause BSD License"

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

tasks.clean {
    delete("$projectDir/build")
}

allprojects {
    // redundant build dir at the end for plugin hotswapping to work during dev
    buildDir = file("${rootProject.projectDir}/build/${project.name}/build")

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    repositories {
        jcenter()
        maven(url = "https://repo.runelite.net")
    }
}

subprojects {
    group = "com.kaipov"
    kapt.includeCompileClasspath = false
    kotlin.sourceSets["main"].kotlin.srcDir("src")

    // Read from source to avoid duplicating this configuration for both OpenOSRS and pf4j manifest metadata
    extra["ID"] = project.path.substring(1)
    extra["Description"] = try {
        val f = File("${project.projectDir}/src/Plugin.kt")
        val regexp = "description.*=.*\"([^\"]+?)\",".toRegex()
        regexp.find(f.readText())?.groupValues?.get(1)!!
    } catch (_: Exception) {
        "n/a"
    }

    dependencies {
        if (project.path != ":_common") implementation(project(":_common"))

        implementation(group = "com.openosrs", name = "http-api")
        implementation(group = "com.openosrs", name = "runelite-api")
        implementation(group = "com.openosrs", name = "runelite-client")
        implementation(group = "com.openosrs.rs", name = "runescape-api")
        implementation(group = "com.openosrs.rs", name = "runescape-client")

        kapt(group = "org.pf4j", name = "pf4j", version = "3.4.1")
        implementation(group = "org.pf4j", name = "pf4j", version = "3.4.1")

        implementation(group = "com.google.inject", name = "guice", version = "4.2.3", classifier = "no_aop")
        implementation(group = "io.reactivex.rxjava3", name = "rxjava", version = "3.0.6")
    }

    configure<JavaPluginConvention> {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.includeRuntime = true
            kotlinOptions.jvmTarget = "11"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
            sourceCompatibility = JavaVersion.VERSION_11.toString()
            targetCompatibility = JavaVersion.VERSION_11.toString()
        }

        withType<AbstractArchiveTask> {
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            dirMode = 493
            fileMode = 420
        }

        withType<Jar> {
            outputs.upToDateWhen { false }

            afterEvaluate {
                manifest {
                    attributes(mapOf(
                        "Plugin-Id" to project.extra["ID"],
                        "Plugin-Version" to project.version,
                        "Plugin-Description" to project.extra["Description"],
                        "Plugin-Provider" to rootProject.extra["Author"],
                        "Plugin-License" to rootProject.extra["License"]
                    ))
                }
            }

            doLast {
                copy {
                    from("$buildDir/libs")
                    into("${rootProject.projectDir}/release")
                }
            }

            if (project.path != ":_common") {
                val commonJar: File = project(":_common").tasks["jar"].outputs.files.singleFile
                val kotlinStdlib: File = configurations.runtimeClasspath.get().filter { it.path.contains("kotlin-stdlib-${kotlin.coreLibrariesVersion}") }.singleFile

                // For final release jar
                from(zipTree(commonJar), zipTree(kotlinStdlib))

                // For hot-reloading during development
                doLast {
                    copy {
                        from(commonJar, kotlinStdlib)
                        into("$buildDir/deps/")
                    }

                    file("$buildDir/../${project.name}.gradle.kts").createNewFile()
                }
            }
        }
    }
}
