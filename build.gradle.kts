plugins {
    id("com.android.library") version "8.12.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.25" apply false
    id("io.github.gradle-nexus.publish-plugin") version "2.0.0"
}

group = (findProperty("GROUP") as? String) ?: "org.endlessai.androidimageeditkit"
version = (findProperty("VERSION_NAME") as? String) ?: "0.1.0"

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(
                uri(
                    (findProperty("SONATYPE_NEXUS_URL") as? String)
                        ?: "https://ossrh-staging-api.central.sonatype.com/service/local/"
                )
            )
            snapshotRepositoryUrl.set(
                uri(
                    (findProperty("SONATYPE_SNAPSHOT_URL") as? String)
                        ?: "https://central.sonatype.com/repository/maven-snapshots/"
                )
            )
            username.set(
                (findProperty("sonatypeUsername") as? String)
                    ?: (findProperty("OSSRH_USERNAME") as? String)
                    ?: System.getenv("SONATYPE_USERNAME")
                    ?: System.getenv("OSSRH_USERNAME")
            )
            password.set(
                (findProperty("sonatypePassword") as? String)
                    ?: (findProperty("OSSRH_PASSWORD") as? String)
                    ?: System.getenv("SONATYPE_PASSWORD")
                    ?: System.getenv("OSSRH_PASSWORD")
            )
        }
    }
}
