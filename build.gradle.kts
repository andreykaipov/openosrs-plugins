val author = "Andrey Kaipov"
val repo = "https://github.com/andreykaipov/osrs-plugins"
val license = "2-Clause BSD License"
val HOME = System.getProperty("user.home")

plugins {
    base
    kotlin("jvm") version "1.4.10"
    kotlin("kapt") version "1.4.10"
}

repositories {
    jcenter()
}

subprojects {
    repositories {
        mavenLocal()
        jcenter()
        maven(url = "https://repo.runelite.net")
        maven(url = "https://raw.githubusercontent.com/open-osrs/hosting/master")
    }

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    dependencies {
        if (project.path != ":_common") {
            implementation(project(":_common"))
        }
        kapt("org.pf4j:pf4j:3.4.1")
        implementation(group = "org.pf4j", name = "pf4j", version = "3.4.1")

        implementation(group = "com.openosrs", name = "http-api", version = "3.4.4")
        implementation(group = "com.openosrs", name = "runelite-api", version = "3.4.4")
        implementation(group = "com.openosrs", name = "runelite-client", version = "3.4.4")
        implementation(group = "javax.annotation", name = "javax.annotation-api", version = "1.3.2")
        implementation(group = "com.google.inject", name = "guice", version = "4.2.2", classifier = "no_aop")
    }

    version = File(project.projectDir, "version").readText()
    ext.set("id", project.path.substring(1))

    // Read the source file to avoid duplicating this configuration for both OpenOSRS and pf4j manifest metadata
    description = if (project.path != ":_common") {
        val f = File("${project.projectDir}/src/main/kotlin/com/kaipov/plugins/Plugin.kt")
        val regexp = "description.*=.*\"([^\"]+)\",".toRegex()
        regexp.find(f.readText())?.groupValues?.get(1)!!
    } else {
        "n/a"
    }

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.jvmTarget = "11"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
            sourceCompatibility = "11"
        }

        withType<Jar> {
            isPreserveFileTimestamps = false  // idempotent builds
            isReproducibleFileOrder = true
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE

            afterEvaluate {
                archiveFileName.set(jarName(project))
            }
        }

        if (project.path != ":_common") {
            withType<Jar> {
                afterEvaluate {
                    jar {
                        manifest { attributes(jarManifestAttributes(project, author, license)) }
                        from(configurations.runtimeClasspath.get().filter { it.name == "_common-unversioned.jar" }.map { zipTree(it) })
                    }
                }

                doLast {
                    arrayOf("../../dev/", "$HOME/.runelite/externalmanager").forEach {
                        copy {
                            from("./build/libs/")
                            into(it)
                        }
                    }
                }
            }

            // The following tasks are aliases to a plugin's specific task. This allows us
            // to build and iterate on one plugin without having to rebuild every plugin inside
            // our aggregate project, or without running each subproject's tasks directly since
            // our funky setup kinda breaks that.
            arrayOf("jar", "clean").forEach {
                project.task("${project.name}-$it") {
                    group = "dev"
                    description = "An alias to the $project's $it task."
                    dependsOn(it)
                }
            }
        }

        withType<Delete> {
            doFirst {
                delete(
                        "../../dev/${jarName(project)}",
                        "$HOME/.runelite/externalmanager/${jarName(project)}"
                )
            }
        }
    }
}
