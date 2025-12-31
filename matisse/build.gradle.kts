import com.vanniktech.maven.publish.AndroidSingleVariantLibrary

plugins {
    alias(libs.plugins.matisse.android.library)
    alias(libs.plugins.matisse.android.compose)
    alias(libs.plugins.maven.publish)
    id("maven-publish")
    id("signing")
}

val signingKeyId = properties["signing.keyId"]?.toString()

android {
    namespace = "github.leavesczy.matisse"
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.ui.tooling.preview)
    implementation (libs.zoomable)
    compileOnly(libs.coil.compose)
    compileOnly(libs.glide.compose)
    debugImplementation(libs.androidx.ui.tooling)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.constraintlayout)
}

val matisseVersion = "2.0.1"

// 配置 JitPack 发布
// 简化配置，避免生成多个重复的 artifact
publishing {
    publications {
        create<MavenPublication>("release") {
            afterEvaluate {
                from(components["release"])
            }
        }
    }
}