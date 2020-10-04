extra["Author"] = "Andrey Kaipov"
extra["Repository"] = "https://github.com/andreykaipov/osrs-plugins"
extra["License"] = "2-Clause BSD License"

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

allprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply<MavenPublishPlugin>()

    repositories {
        mavenLocal()
        maven(url = "https://raw.githubusercontent.com/open-osrs/hosting/master")
        maven(url = "https://repo.runelite.net")
        jcenter()
    }
}

subprojects {
    group = "com.openosrs.externals"
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

        kapt(group = "org.pf4j", name = "pf4j", version = "3.4.1")
        implementation(group = "org.pf4j", name = "pf4j", version = "3.4.1")

        implementation(group = "com.openosrs", name = "http-api", version = "3.4.4")
        implementation(group = "com.openosrs", name = "runelite-api", version = "3.4.4")
        implementation(group = "com.openosrs", name = "runelite-client", version = "3.4.4")
        implementation(group = "com.openosrs.rs", name = "runescape-api", version = "3.4.4")
        implementation(group = "com.openosrs.rs", name = "runescape-client", version = "3.4.4")

        implementation(group = "org.apache.commons", name = "commons-text", version = "1.9")
        implementation(group = "com.google.guava", name = "guava", version = "29.0-jre")
        implementation(group = "com.google.inject", name = "guice", version = "4.2.3", classifier = "no_aop")
        implementation(group = "com.google.code.gson", name = "gson", version = "2.8.6")
        implementation(group = "net.sf.jopt-simple", name = "jopt-simple", version = "5.0.4")
        implementation(group = "ch.qos.logback", name = "logback-classic", version = "1.2.3")
        implementation(group = "com.squareup.okhttp3", name = "okhttp", version = "4.8.1")
        implementation(group = "io.reactivex.rxjava3", name = "rxjava", version = "3.0.6")
        implementation(group = "org.pushing-pixels", name = "radiance-substance", version = "2.5.1")
    }

    configure<PublishingExtension> {
        repositories {
            maven {
                url = uri("$buildDir/repo")
            }
        }
        publications {
            register("mavenJava", MavenPublication::class) {
                from(components["java"])
            }
        }
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
                    from("./build/libs/")
                    into("../../release/")
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
                        into("./build/deps/")
                        from(commonJar, kotlinStdlib)
                    }
                }
            }
        }
    }
}
