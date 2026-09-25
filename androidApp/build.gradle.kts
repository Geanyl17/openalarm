import com.android.build.api.artifact.SingleArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

// Releases are signed on the maintainer's machine, and the key never goes into the repo or CI.
// The openalarm.signing Gradle property (for example in ~/.gradle/gradle.properties) points to a
// properties file with storeFile, storePassword, keyAlias and keyPassword. Without it, release
// builds come out unsigned. RELEASING.md has the details.
val releaseSigning: Properties? = providers.gradleProperty("openalarm.signing").orNull?.let { path ->
    Properties().apply { load(StringReader(providers.fileContents(layout.projectDirectory.file(path)).asText.get())) }
}

android {
    namespace = "io.github.geanyl17.openalarm"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Permanent once published on an app store.
        applicationId = "io.github.geanyl17.openalarm"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 2
        versionName = "0.2.0"
    }
    signingConfigs {
        if (releaseSigning != null) {
            create("release") {
                storeFile = file(releaseSigning.getProperty("storeFile"))
                storePassword = releaseSigning.getProperty("storePassword")
                keyAlias = releaseSigning.getProperty("keyAlias")
                keyPassword = releaseSigning.getProperty("keyPassword")
            }
        }
    }
    buildTypes {
        release {
            // R8 shrinks the APK to a fraction of its size and makes Compose noticeably faster.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    dependenciesInfo {
        // This block is encrypted with Google's key, so F-Droid and IzzyOnDroid reject APKs that contain it.
        includeInApk = false
        includeInBundle = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(projects.shared.data)
    implementation(projects.shared.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core)
    implementation(libs.androidx.exifinterface)
    implementation(libs.compose.components.resources)
}

/** Fails the build if the final app manifest requests the internet permission. */
abstract class CheckNoInternetPermission : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @TaskAction
    fun verify() {
        val internet = Regex("""android:name\s*=\s*"android\.permission\.INTERNET"""")
        if (internet.containsMatchIn(mergedManifest.get().asFile.readText())) {
            throw GradleException(
                "The app requests android.permission.INTERNET, but OpenAlarm must work offline " +
                    "(ROADMAP.md, principle 4). A dependency probably added it: see the manifest merger " +
                    "report in androidApp/build/outputs/logs/.",
            )
        }
    }
}

androidComponents {
    onVariants { variant ->
        val verifyTask = tasks.register<CheckNoInternetPermission>(
            "check${variant.name.replaceFirstChar(Char::titlecase)}NoInternetPermission",
        ) {
            mergedManifest = variant.artifacts.get(SingleArtifact.MERGED_MANIFEST)
        }
        tasks.named("check") { dependsOn(verifyTask) }
    }
}
