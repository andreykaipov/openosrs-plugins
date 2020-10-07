val g = (gradle as ExtensionAware)
val oprsVersion = g.extra["oprsVersion"] as String
val pf4jVersion = g.extra["pf4jVersion"] as String

plugins {
    kotlin("jvm")
    kotlin("kapt")
}

tasks.clean {
    delete("${rootProject.projectDir}/.build")
}

allprojects {
    // redundant build dir at the end for plugin hotswapping to work during dev
    buildDir = file("${rootProject.projectDir}/.build/${project.name}/build")

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
    if (path == ":plugins") return@subprojects
    println(path)

    dependencies {
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

    group = "com.kaipov"

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

    kapt.includeCompileClasspath = false
    kotlin.sourceSets["main"].kotlin.srcDirs("src", ".")

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions.includeRuntime = true
        kotlinOptions.jvmTarget = "11"
        kotlinOptions.freeCompilerArgs = listOf("-Xjvm-default=enable")
        sourceCompatibility = JavaVersion.VERSION_11.toString()
        targetCompatibility = JavaVersion.VERSION_11.toString()
    }

    tasks.withType<AbstractArchiveTask> {
        outputs.upToDateWhen { false }
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }
}
