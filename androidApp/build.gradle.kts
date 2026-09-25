import com.android.build.api.artifact.SingleArtifact
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "io.github.geanyl17.openalarm"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        // Permanent once published on an app store.
        applicationId = "io.github.geanyl17.openalarm"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
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
