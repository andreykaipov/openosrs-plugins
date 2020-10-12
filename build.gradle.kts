val g = (gradle as ExtensionAware)

tasks.wrapper { gradleVersion = "6.6.1" }
tasks.clean { delete("$projectDir/build") }

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

allprojects {
    group = "com.kaipov"
    buildDir = file("${rootProject.projectDir}/build/${project.name}/build")

    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")

    repositories {
        jcenter()
        maven(url = "https://repo.runelite.net")
    }
}

subprojects {
    if (path == ":plugins") return@subprojects

    dependencies {
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

    kapt.includeCompileClasspath = false
    kotlin.sourceSets["main"].kotlin.srcDir("src")

    tasks {
        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions.includeRuntime = true
            kotlinOptions.jvmTarget = "11"
            kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
            sourceCompatibility = JavaVersion.VERSION_11.toString()
            targetCompatibility = JavaVersion.VERSION_11.toString()
        }

        withType<AbstractArchiveTask> {
            outputs.upToDateWhen { false }
            isPreserveFileTimestamps = false
            isReproducibleFileOrder = true
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        }
    }
}
