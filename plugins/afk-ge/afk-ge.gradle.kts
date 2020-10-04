version = "0.1.0"

dependencies {
    implementation(project(":_common"))
}

val commonJar = project(":_common").tasks["jar"].outputs.files.singleFile
val kotlinStdilib = configurations.runtimeClasspath.get().filter { it.path.contains("kotlin-stdlib-1.4.10") }.singleFile

tasks {
    withType<Jar> {
        // For final release jar
        from(zipTree(commonJar))

        // For hot-reloading during development
        doLast {
            copy {
                into("./build/deps/")
                from(commonJar, kotlinStdilib)
            }
        }
    }
}
