import org.gradle.api.publish.maven.MavenPublication

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
    id("signing")
}

group = (findProperty("GROUP") as? String) ?: "org.endlessai.androidimageeditkit"
version = (findProperty("VERSION_NAME") as? String) ?: "0.1.0"

fun prop(name: String, default: String): String {
    return (findProperty(name) as? String)
        ?.takeIf { it.isNotBlank() }
        ?: default
}

val pomArtifactId = prop("POM_ARTIFACT_ID", "imageeditkit")

android {
    namespace = "org.endlessai.androidimageeditkit"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                groupId = project.group.toString()
                artifactId = pomArtifactId
                version = project.version.toString()

                from(components["release"])

                pom {
                    name.set(prop("POM_NAME", "AndroidImageEditKit"))
                    description.set(
                        prop(
                            "POM_DESCRIPTION",
                            "Android image edit kit for external image crop and precise export workflows."
                        )
                    )
                    url.set(prop("POM_URL", "https://github.com/chenlidongorg/AndroidImageEditKit"))

                    licenses {
                        license {
                            name.set(prop("POM_LICENSE_NAME", "The Apache License, Version 2.0"))
                            url.set(
                                prop(
                                    "POM_LICENSE_URL",
                                    "https://www.apache.org/licenses/LICENSE-2.0.txt"
                                )
                            )
                        }
                    }

                    developers {
                        developer {
                            id.set(prop("POM_DEVELOPER_ID", "endlessai"))
                            name.set(prop("POM_DEVELOPER_NAME", "EndlessAI"))
                            email.set(prop("POM_DEVELOPER_EMAIL", "opensource@endlessai.org"))
                        }
                    }

                    scm {
                        connection.set(
                            prop(
                                "POM_SCM_CONNECTION",
                                "scm:git:git://github.com/chenlidongorg/AndroidImageEditKit.git"
                            )
                        )
                        developerConnection.set(
                            prop(
                                "POM_SCM_DEV_CONNECTION",
                                "scm:git:ssh://git@github.com/chenlidongorg/AndroidImageEditKit.git"
                            )
                        )
                        url.set(prop("POM_SCM_URL", "https://github.com/chenlidongorg/AndroidImageEditKit"))
                    }
                }
            }
        }
    }
}

signing {
    val shouldSignForCentral = gradle.startParameter.taskNames.any { taskName ->
        taskName.contains("Sonatype", ignoreCase = true)
    }
    setRequired { shouldSignForCentral }

    val signingKey = (findProperty("SIGNING_KEY") as? String)
        ?: System.getenv("SIGNING_KEY")
    val signingPassword = (findProperty("SIGNING_PASSWORD") as? String)
        ?: System.getenv("SIGNING_PASSWORD")

    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassword)
    }
    sign(publishing.publications)
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.5")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("androidx.compose.ui:ui:1.7.0")
    implementation("androidx.compose.ui:ui-graphics:1.7.0")
    implementation("androidx.compose.ui:ui-tooling-preview:1.7.0")
    implementation("androidx.compose.material3:material3:1.3.0")
    implementation("androidx.compose.material:material-icons-extended:1.7.0")

    debugImplementation("androidx.compose.ui:ui-tooling:1.7.0")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.0")
}
