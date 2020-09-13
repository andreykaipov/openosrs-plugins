import java.security.MessageDigest
import org.gradle.api.Project

@SuppressWarnings()
fun sha512sum(file: ByteArray): String {
    return MessageDigest.getInstance("SHA-512").digest(file).fold("", { str, it -> str + "%02x".format(it) }).toUpperCase()
}

fun jarName(p: Project): String {
    val id: String = if (p.hasProperty("id")) p.property("id") as String else p.name
    val version = p.version as String
    return "$id-$version.jar"
}

fun remoteJarURL(repo: String, project: Project): String {
    return "$repo/blob/master/release/${jarName(project)}?raw=true"
}

fun jarManifestAttributes(p: Project, provider: String, license: String): Map<String, String> {
    return mapOf(
            "Plugin-Id" to if (p.hasProperty("id")) p.property("id") as String else p.name,
            "Plugin-Version" to p.version as String,
            "Plugin-Description" to (p.description ?: "n/a:"),
            "Plugin-Provider" to provider,
            "Plugin-License" to license
    )
}

// slug removes any non-alpha characters and appends a hash of the original name to guarantee uniqueness
// if ever the slugged project paths of two plugins are ever the same, e.g. my-plugin-v1 and my-plugin-v2
fun slug(name: String): String {
    return name.replace("[^A-Za-z]".toRegex(), "").toLowerCase() + name.hashCode()
}