plugins {
    id("com.android.library") version "8.12.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

fun propOrNull(name: String): String? = (findProperty(name) as? String)?.takeIf { it.isNotBlank() }

group = propOrNull("GROUP") ?: "org.endlessai.androidimageeditkit"
version = propOrNull("VERSION_NAME") ?: "0.1.0"
val sonatypePackageGroup = propOrNull("SONATYPE_PACKAGE_GROUP") ?: "org.endlessai"

nexusPublishing {
    repositories {
        sonatype {
            packageGroup.set(sonatypePackageGroup)
            nexusUrl.set(
                uri(
                    propOrNull("SONATYPE_NEXUS_URL")
                        ?: "https://ossrh-staging-api.central.sonatype.com/service/local/"
                )
            )
            snapshotRepositoryUrl.set(
                uri(
                    propOrNull("SONATYPE_SNAPSHOT_URL")
                        ?: "https://central.sonatype.com/repository/maven-snapshots/"
                )
            )
            username.set(
                propOrNull("sonatypeUsername")
                    ?: propOrNull("OSSRH_USERNAME")
                    ?: System.getenv("SONATYPE_USERNAME")
                    ?: System.getenv("OSSRH_USERNAME")
            )
            password.set(
                propOrNull("sonatypePassword")
                    ?: propOrNull("OSSRH_PASSWORD")
                    ?: System.getenv("SONATYPE_PASSWORD")
                    ?: System.getenv("OSSRH_PASSWORD")
            )
        }
    }
}
