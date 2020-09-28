version = "0.1.0"

dependencies {
    implementation(project(":_common"))
}

val commonJar = project(":_common").tasks["jar"].outputs.files.singleFile

tasks {
    withType<Jar> {
        from(zipTree(commonJar))

        doLast {
            copy {
                into("./build/deps/")
                from(commonJar)
            }
        }
    }
}
