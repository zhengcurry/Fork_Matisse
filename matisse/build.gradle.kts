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
    implementation("androidx.media3:media3-exoplayer:1.8.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
}

val matisseVersion = "1.0.0"

// 配置 JitPack 发布
publishing {
    publications {
        create<MavenPublication>("release") {
            groupId = "com.github.zhengcurry"
            artifactId = "Fork_Matisse"
            version = matisseVersion

            afterEvaluate {
                from(components["release"])
            }

            pom {
                name.set("Matisse")
                description.set("An Android Image and Video Selection Framework Implemented with Jetpack Compose - Fork Version")
                url.set("https://github.com/zhengcurry/Fork_Matisse")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                developers {
                    developer {
                        id.set("zhengcurry")
                        name.set("zhengcurry")
                        url.set("https://github.com/zhengcurry")
                    }
                }

                scm {
                    url.set("https://github.com/zhengcurry/Fork_Matisse")
                    connection.set("scm:git:git://github.com/zhengcurry/Fork_Matisse.git")
                    developerConnection.set("scm:git:ssh://git@github.com/zhengcurry/Fork_Matisse.git")
                }
            }
        }
    }
}