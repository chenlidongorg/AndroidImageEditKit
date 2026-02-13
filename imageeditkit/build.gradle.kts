plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("maven-publish")
}

group = (findProperty("GROUP") as? String) ?: "org.endlessai.androidimageeditkit"
version = (findProperty("VERSION_NAME") as? String) ?: "0.1.0"

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
        }
    }
}

publishing {
    publications {
        create("release", org.gradle.api.publish.maven.MavenPublication::class) {
            groupId = project.group.toString()
            artifactId = "imageeditkit"
            version = project.version.toString()
            afterEvaluate {
                from(components["release"])
            }
        }
    }
    repositories {
        maven {
            name = "GitHubPackages"
            val githubRepository = (findProperty("GITHUB_REPOSITORY") as? String)
                ?: System.getenv("GITHUB_REPOSITORY")
                ?: "chenlidongorg/AndroidImageEditKit"
            url = uri("https://maven.pkg.github.com/$githubRepository")

            credentials {
                username = (findProperty("GITHUB_PACKAGES_USER") as? String)
                    ?: System.getenv("GITHUB_ACTOR")
                    ?: System.getenv("GITHUB_USERNAME")
                password = (findProperty("GITHUB_PACKAGES_TOKEN") as? String)
                    ?: System.getenv("GITHUB_TOKEN")
                    ?: System.getenv("GITHUB_PAT")
            }
        }
    }
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
