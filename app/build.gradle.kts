@file:Suppress("SpellCheckingInspection")

import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.api.BaseVariantOutputImpl
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.Properties
import java.util.TimeZone
import java.util.zip.CRC32
import kotlin.text.RegexOption.IGNORE_CASE

val globalApplicationId = "org.autojs.autojs6"

val sign = Sign("$rootDir/sign.properties")
val versions = Versions("$rootDir/version.properties")

val dimention = "channel"
val flavorNameApp = "app"
val flavorNameInrt = "inrt"
val buildTypeDebug = "debug"
val buildTypeRelease = "release"
val buildActionAssemble = "assemble"
val templateName = "template"

plugins {
    id("com.android.application")
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.android") /* kotlin("android") */
}

dependencies /* Unclassified */ {
    // Compose
    // implementation("androidx.compose.ui:ui-android:1.6.7")

    // Kotlin reflect
    implementation(kotlin("reflect"))

    // LeakCanary
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")

    // Android supports
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.multidex:multidex:2.0.1")

    // Material Components
    implementation("com.google.android.material:material:1.12.0")

    // SwipeRefreshLayout
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    // FlexboxLayout
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Common Markdown
    implementation("com.github.atlassian:commonmark-java:commonmark-parent-0.9.0")

    // Licenses Dialog
    implementation("de.psdev.licensesdialog:licensesdialog:2.2.0")

    // Apache Commons
    implementation("org.apache.commons:commons-lang3:3.16.0")

    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.retrofit2:adapter-rxjava2:2.11.0")
    implementation("com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2")

    // Glide
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")

    // Joda Time
    implementation("joda-time:joda-time:2.12.7")

    // Flurry
    implementation("com.flurry.android:analytics:14.4.0")

    // Bugly
    implementation(project(":libs:com.tencent.bugly.crashreport-4.0.4"))

    // OkHttp
    // implementation("com.squareup.okhttp3:okhttp:5.0.0-alpha.12")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Webkit
    implementation("androidx.webkit:webkit:1.13.0")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // Zip4j
    implementation("net.lingala.zip4j:zip4j:2.11.5")

    // Log4j
    // FIXME by SuperMonster003 on Aug 14, 2024.
    //  ! Vulnerable dependency (5 vulnerabilities) for log4j (version 1):
    //  ! - CVE-2022-23307, Score: 8.8
    //  ! - CVE-2022-23305, Score: 9.8
    //  ! - CVE-2022-23302, Score: 8.8
    //  ! - CVE-2021-4104, Score: 7.5
    //  ! - CVE-2019-17571, Score: 9.8
    //  ! However, log4j version 2 which requires Android API Level not lower than 26
    //  ! is not compatible with current project with min API Level 24.
    //  !
    //  ! zh-CN:
    //  !
    //  ! 依赖库 log4j (第一版本) 是易受攻击的 (含 5 项漏洞):
    //  ! - CVE-2022-23307, 评分: 8.8
    //  ! - CVE-2022-23305, 评分: 9.8
    //  ! - CVE-2022-23302, 评分: 8.8
    //  ! - CVE-2021-4104, 评分: 7.5
    //  ! - CVE-2019-17571, 评分: 9.8
    //  ! 但 log4j 第二版本要求安卓 API 级别不低于 26,
    //  ! 与最低 API 级别为 24 的当前项目无法兼容.
    implementation("log4j:log4j:1.2.17")

    // Android Logging Log4j
    implementation("de.mindpipe.android:android-logging-log4j:1.0.3")

    // Preference
    implementation("androidx.preference:preference-ktx:1.2.1")

    // RootShell
    implementation("com.github.Stericson:RootShell:1.6")

    // JDeferred
    implementation("org.jdeferred:jdeferred-android-aar:1.2.6")

    // Rx
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1@aar")

    // Device Names
    implementation("com.jaredrummler:android-device-names:2.1.1")

    // Version Compare
    implementation("io.github.g00fy2:versioncompare:1.5.0")

    // Terminal Emulator
    implementation(project(":libs:jackpal.androidterm.libtermexec-1.0"))
    implementation(project(":libs:jackpal.androidterm.emulatorview-1.0.42"))
    implementation(project(":libs:jackpal.androidterm-1.0.70"))

    // Dex
    implementation(files("$rootDir/libs/com.android.dx-1.14.jar"))
    implementation(files("$rootDir/libs/com.legacy.android.dx-1.7.0.jar"))

    // OpenCV
    implementation(project(":libs:org.opencv-4.8.0"))

    // PaddleOCR
    implementation(project(":libs:paddleocr"))

    // RapidOCR
    implementation(project(":libs:rapidocr"))

    // Android Job
    implementation(project(":libs:android-job-simplified-1.4.3"))

    // APK Parser (https://github.com/jaredrummler/APKParser)
    implementation(project(":libs:apk-parser-1.0.2"))

    // Prism4j
    implementation(files("$rootDir/libs/prism4j-2.0.0.jar"))
    implementation(files("$rootDir/libs/prism4j-bundler-2.0.0.jar"))
    implementation(project(":libs:markwon-core-4.6.2"))
    implementation(project(":libs:markwon-syntax-highlight-4.6.2"))

    // Rhino
    implementation(files("$rootDir/libs/org.mozilla.rhino-1.8.1-SNAPSHOT.jar"))

    // Tasker Plugin
    implementation(project(":libs:android-spackle-9.0.0"))
    implementation(project(":libs:android-assertion-9.0.0"))
    implementation(project(":libs:android-plugin-client-sdk-for-locale-9.0.0"))

    // JavaMail for Android
    implementation(files("$rootDir/libs/javamail-android/activation.jar"))
    implementation(files("$rootDir/libs/javamail-android/additionnal.jar"))
    implementation(files("$rootDir/libs/javamail-android/mail.jar"))

    // Shizuku
    implementation("dev.rikka.shizuku:api:13.1.5")
    implementation("dev.rikka.shizuku:provider:13.1.5")

    // ARSCLib
    implementation("io.github.reandroid:ARSCLib:1.3.1")

    // Toaster
    implementation("com.github.getActivity:Toaster:12.6")
    implementation("com.github.getActivity:EasyWindow:10.3")

    // Pinyin4j
    implementation("com.belerweb:pinyin4j:2.5.1")

    // Jieba Analysis (zh-CN: 结巴分词)
    // implementation("com.huaban:jieba-analysis:1.0.2")
    implementation(project(":modules:jieba-analysis"))

    // Tiny Sign
    implementation(files("$rootDir/libs/tiny-sign-0.9.jar"))

    // Room
    implementation("androidx.room:room-runtime:2.7.0")
    implementation("androidx.room:room-ktx:2.7.0")
    ksp("androidx.room:room-compiler:2.7.0")

    // ApkSig
    // implementation("com.android.tools.build:apksig:8.7.3")

    // ApkSigner
    implementation(project(":modules:apk-signer"))

    // Spongy Castle
    implementation("com.madgag.spongycastle:prov:1.58.0.0")

    // MQTT
    implementation("org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0")
    implementation("org.eclipse.paho:org.eclipse.paho.android.service:1.1.1")
}

dependencies /* MIME */ {
    // @Hint by SuperMonster003 on Oct 5, 2023.
    //  ! Only for Android API 26 (8.0) [O] and above.
    //  ! zh-CN: 仅适用于安卓 API 级别 26 (8.0) [O] 及以上.
    // Apache Tika Core
    // implementation("org.apache.tika:tika-core:2.9.2")

    // MIME Util
    // implementation("eu.medsea.mimeutil:mime-util:2.1.3")
    implementation(files("$rootDir/libs/mime-util-2.1.3.jar"))
}

dependencies /* Test */ {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
}

dependencies /* Annotations */ {
    // Android Annotations
    implementation("org.androidannotations:androidannotations-api:4.8.0")
    implementation("androidx.annotation:annotation:1.9.1")
    ksp("org.androidannotations:androidannotations:4.8.0")

    // JCIP Annotations
    implementation("net.jcip:jcip-annotations:1.0")

    // EventBus
    implementation("org.greenrobot:eventbus:3.3.1")
}

dependencies /* AppCompat */ {
    // @Hint by SuperMonster003 on Oct 5, 2023.
    //  ! To check the releases for Appcompat library,
    //  ! visit https://developer.android.com/jetpack/androidx/releases/appcompat.
    //  ! zh-CN:
    //  ! 查看 Appcompat 库的发行版本,
    //  ! 可访问 https://developer.android.com/jetpack/androidx/releases/appcompat.
    implementation("androidx.appcompat:appcompat:1.7.0")

    // AppCompat for legacy views (such as JsTextViewLegacy)
    implementation(project(":libs:androidx.appcompat-1.0.2")) {
        setVersion("1.0.2")
    }
}

dependencies /* Material Dialogs */ {
    // Material Dialogs
    // TODO by SuperMonster003 on Feb 5, 2022.
    //  ! Upgrade to 3.3.0 (more difficult than expected).
    //  ! zh-CN: 升级至 3.3.0 (实际难度超出预期较多).
    val configuration: (ExternalModuleDependency).() -> Unit = {
        version {
            prefer("0.9.6.0")
            because("Not ready to update to version 3.3.0 yet")
        }
    }
    configuration.let { cfg ->
        implementation("com.afollestad.material-dialogs:core", cfg)
        implementation("com.afollestad.material-dialogs:commons", cfg)
    }
}

dependencies /* Layout */ {
    // Expandable Layout
    implementation("com.github.aakira:expandable-layout:1.6.0")

    // Expandable RecyclerView
    implementation("com.bignerdranch.android:expandablerecyclerview:3.0.0-RC1")

    // Flexible Divider
    implementation("com.yqritc:recyclerview-flexibledivider:1.4.0")
}

dependencies /* View */ {
    // RoundedImageView
    implementation("com.makeramen:roundedimageview:2.3.0")

    // CircleImageView
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Animated SVG
    implementation("com.jaredrummler:animated-svg-view:1.0.6")
}

dependencies /* GitHub API */ {
    implementation(files("$rootDir/libs/github-api-1.306.jar"))

    implementation("commons-io:commons-io") {
        because("Compatibility for Android API Level < 26 (Android 8.0) [O]")
        version {
            strictly("2.8.0")
            because("Exception on newer versions: 'NoClassDefFoundError: org.apache.commons.io.IOUtils'")
        }
    }

    implementation("com.fasterxml.jackson.core:jackson-databind") {
        because("Compatibility for Android API Level < 26 (Android 8.0) [O]")
        version {
            strictly("2.13.4.2")
            because("Exception on 2.14.x: 'No virtual method getParameterCount()I in class Ljava/lang/reflect/Method'")
        }
    }

    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4") {
        because("Compatibility of java.time.* for Android API Level < 26 (Android 8.0) [O]")
    }
}

dependencies /* MLKit */ {
    // OCR
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")

    // Barcode
    implementation("com.google.mlkit:barcode-scanning:17.3.0")
}

dependencies /* OpenCC */ {
    // OpenCC
    implementation("com.github.qichuan:android-opencc:1.2.0")
}

dependencies /* Auto.js Extensions */ {
    // Settings Compat
    // @Integrated by SuperMonster003 on Mar 30, 2023.
    //  # implementation("com.github.hyb1996:settingscompat:1.1.5")

    // Enhanced Floaty
    // @Integrated by SuperMonster003 on Mar 30, 2023.
    //  # implementation("com.github.hyb1996:EnhancedFloaty:0.31")

    // MultiLevelListView
    // @Integrated by SuperMonster003 on Mar 30, 2023.
    //  # implementation("com.github.hyb1996:android-multi-level-listview:1.1")

    // Auto.js APK Builder
    // @Integrated by SuperMonster003 on Mar 30, 2023.
    //  # implementation(project(":libs:Auto.js-ApkBuilder-1.0.3"))

    // Extracted from com.github.hyb1996:MutableTheme:1.0.0
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.github.ozodrukh:CircularReveal:2.0.1")
    // @Legacy com.jrummyapps:colorpicker:2.1.7
    // @Integrated by SuperMonster003 on Mar 25, 2025.
    //  # implementation("com.jaredrummler:colorpicker:1.1.0")
    implementation(project(":modules:color-picker"))
}

dependencies /* Archived */ {
    // Kotlin
    // @Comment by SuperMonster003 on May 19, 2022.
    //  ! It is no longer necessary to declare a dependency on the stdlib library in any Kotlin Gradle project.
    //  ! The dependency is added by default.
    //  ! See https://kotlinlang.org/docs/gradle.html#dependency-on-the-standard-library.
    //  ! zh-CN:
    //  ! 已无需在 Kotlin Gradle 项目中显式声明标准库 (stdlib).
    //  ! 相关依赖已默认被添加.
    //  ! 参阅 https://kotlinlang.org/docs/gradle.html#dependency-on-the-standard-library.
    // implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk7:1.6.21")

    // Google Guava
    // @Comment by SuperMonster003 on Jun 1, 2022.
    //  ! Not necessary for current project but worth keeping the trace.
    //  ! zh-CN: 于当前项目已不再需要, 但依然值得留存其踪迹 (以备不时之需).
    // implementation("com.google.guava:guava:31.1-jre")

    // Javax WS RS API (Java API for RESTful Web Services)
    // @Comment by SuperMonster003 on Apr 9, 2024.
    //  ! It was ever imported and used for MediaType constants.
    //  ! zh-CN: 曾用于 MediaType 常量的导入及使用.
    // implementation("javax.ws.rs:javax.ws.rs-api:2.1.1")
}

dependencies /* Reserved for auto append by IDE */ {

}

android {

    namespace = globalApplicationId
    compileSdk = versions.sdkVersionCompile

    defaultConfig {
        applicationId = applicationId

        minSdk = versions.sdkVersionMin
        targetSdk = versions.sdkVersionTarget

        versionCode = versions.appVersionCode
        versionName = versions.appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        multiDexEnabled = true

        buildConfigField("String", "VERSION_DATE", "\"${Utils.getDateString("MMM d, yyyy", "GMT+08:00")}\"")
        buildConfigField("String", "VSCODE_EXT_REQUIRED_VERSION", "\"${versions.vscodeExtRequiredVersion}\"")
        buildConfigField("boolean", "is${flavorNameInrt.uppercaseFirstChar()}", "false")

        javaCompileOptions {
            annotationProcessorOptions {
                mapOf(
                    "resourcePackageName" to (this@defaultConfig.applicationId ?: globalApplicationId),
                    "androidManifestFile" to ("$projectDir/src/main/AndroidManifest.xml")
                ).let { arguments(it) }
            }
        }
    }

    flavorDimensions.add(dimention)

    productFlavors {

        create(flavorNameApp) {
            dimension = dimention
            versionCode = versions.appVersionCode
            versionName = versions.appVersionName
            buildConfigField("String", "CHANNEL", "\"$flavorNameApp\"")
            buildConfigField("boolean", "is${flavorNameInrt.uppercaseFirstChar()}", "false")
            manifestPlaceholders.putAll(
                mapOf(
                    "CHANNEL" to flavorNameApp,
                    "appName" to "@string/app_name",
                    "intentCategory" to "android.intent.category.LAUNCHER",
                    "intentCategoryInrt" to "android.intent.category.DEFAULT",
                    "authorities" to "org.autojs.autojs6.fileprovider",
                    "icon" to "@drawable/autojs6_material",
                )
            )
        }

        create(flavorNameInrt) {
            dimension = dimention
            applicationIdSuffix = ".$flavorNameInrt"
            minSdk = versions.sdkVersionMin
            targetSdk = versions.sdkVersionTargetInrt
            compileSdk = versions.sdkVersionCompile
            versionCode = versions.appVersionCode
            versionName = versions.appVersionName

            buildConfigField("String", "CHANNEL", "\"$flavorNameInrt\"")
            buildConfigField("boolean", "is${flavorNameInrt.uppercaseFirstChar()}", "true")

            manifestPlaceholders.putAll(
                mapOf(
                    "CHANNEL" to flavorNameInrt,
                    "appName" to "AutoJs6.$flavorNameInrt",
                    "intentCategory" to "android.intent.category.DEFAULT",
                    "intentCategoryInrt" to "android.intent.category.LAUNCHER",
                    "authorities" to "org.autojs.autojs6.$flavorNameInrt.fileprovider",
                    "icon" to "@mipmap/ic_launcher",
                )
            )

            // @Reference to kkevsekk1/AutoX (https://github.com/kkevsekk1/AutoX) by SuperMonster003 on Nov 16, 2023.
            //  ! https://github.com/kkevsekk1/AutoX/blob/a6d482189291b460c3be60970b74c5321d26e457/inrt/build.gradle.kts#L93
            // noinspection ChromeOsAbiSupport
            ndk.abiFilters += ""

            gradle.taskGraph.whenReady(object : Action<TaskExecutionGraph> {
                override fun execute(taskGraph: TaskExecutionGraph) {
                    val taskName = "$buildActionAssemble${flavorNameInrt.uppercaseFirstChar()}${buildTypeRelease.uppercaseFirstChar()}"
                    project.getTasksByName(taskName, true)
                        .firstOrNull()
                        ?.doLast {
                            copy {
                                val src = "build/outputs/apk/$flavorNameInrt/$buildTypeRelease"

                                // @Reference to LZX284 (https://github.com/LZX284) by SuperMonster003 on Nov 16, 2023.
                                val dst = "src/main/assets-$flavorNameApp"

                                val ext = Utils.FILE_EXTENSION_APK

                                if (!file(src).isDirectory) {
                                    return@copy
                                }

                                from(src); into(dst)

                                val verName = versionName?.replace(Regex("\\s"), "-")?.lowercase()

                                /* e.g. inrt-v6.4.0-beta-universal.apk */
                                val srcFileName = "$flavorNameInrt-v$verName-universal.$ext".also {
                                    if (!file(File(src, it)).exists()) {
                                        throw GradleException("Source file \"${file(File(src, it))}\" doesn't exist")
                                    }
                                }

                                val dstFileName = "$templateName.$ext"
                                val isOverridden = file(File(dst, dstFileName)).exists()
                                include(srcFileName)
                                rename(srcFileName, dstFileName)
                                println("Source: ${file(File(src, srcFileName))}")
                                println("Destination: ${file(File(dst, dstFileName))}${if (isOverridden) " [overridden]" else ""}")
                            }
                        }
                        ?: println("$taskName doesn't exist in project ${project.name}")
                }
            })
        }

        androidResources {
            if (gradle.startParameter.taskNames.any { it.contains(Regex("^(:?$flavorNameApp:)?$buildActionAssemble")) }) {
                ignoreAssetsPatterns.addAll(listOf(".idea", "declarations", "sample/declarations"))
            }
            if (gradle.startParameter.taskNames.any { it.contains(Regex("^(:?$flavorNameApp:)?$buildActionAssemble$flavorNameInrt", IGNORE_CASE)) }) {
                // @Hint by SuperMonster003 on Oct 16, 2023.
                //  ! Runtime assets will be copied from flavor "app"
                //  ! while building an apk on org.autojs.autojs.ui.project.BuildActivity.
                //  ! zh-CN:
                //  ! 类 org.autojs.autojs.ui.project.BuildActivity 构建 APK 时,
                //  ! 运行时资产文件 (runtime assets) 将由名为 "app" 的 Gradle flavor 作为源地址进行复制.
                ignoreAssetsPatterns.addAll(emptyList())
            }
        }

    }

    sourceSets {
        // @Hint by LZX284 (https://github.com/LZX284) on Nov 15, 2023.
        //  ! The assets file is divided into three directories according to different flavors.
        //  ! But the files are not actually moved to avoid conflicts with the latest modifications.
        //  ! zh-CN (translated by SuperMonster003 on Jul 26, 2024):
        //  ! 资产文件根据不同的 Gradle flavor 被隔离为三个不同的目录.
        //  ! 不过真实文件并没有进行移动, 以避免与最新的项目修改发生冲突.
        //  !
        // @Hint by SuperMonster003 on Nov 16, 2023.
        //  ! The assets division idea was accepted, and it wouldn't hurt to try. :)
        //  ! zh-CN: 资产隔离的想法可以被接受, 毕竟试一下也无妨. [笑脸符号]

        getByName("main") {
            assets.srcDirs("src/main/assets")
        }
        getByName("release") {
            java.srcDirs("src/release/java")
        }
        getByName("debug") {
            java.srcDirs("src/debug/java")
        }
        getByName(flavorNameApp) {
            assets.srcDirs("src/main/assets-$flavorNameApp")
        }
        getByName(flavorNameInrt) {
            assets.srcDirs("src/main/assets-$flavorNameInrt")
        }
    }

    // 创建一个任务来修复生成的AIDL Java文件
    tasks.register("fixGeneratedAidlJavaFile") {
        doLast {
            val generatedFile = File(project.projectDir, "app/build/generated/aidl_source_output_dir/appDebug/out/org/autojs/autojs/core/shizuku/IUserService.java")
            
            if (generatedFile.exists()) {
                println("[AIDL修复] 正在处理文件: ${generatedFile.absolutePath}")
                
                // 读取文件内容
                val content = generatedFile.readText()
                
                // 替换文件内容 - 完全替换为正确的版本
                generatedFile.writeText("""
                    /*
                     * 手动修复文件 - 替代自动生成的有问题文件
                     */
                    package org.autojs.autojs.core.shizuku;

                    public interface IUserService extends android.os.IInterface {
                        /** Default implementation for IUserService. */
                        public static class Default implements org.autojs.autojs.core.shizuku.IUserService {
                            @Override public void destroy() throws android.os.RemoteException {}
                            @Override public void exit() throws android.os.RemoteException {}
                            @Override public String execCommand(String command) throws android.os.RemoteException { return null; }
                            @Override public String currentPackage() throws android.os.RemoteException { return null; }
                            @Override public String currentActivity() throws android.os.RemoteException { return null; }
                            @Override public String currentComponent() throws android.os.RemoteException { return null; }
                            @Override public String currentComponentShort() throws android.os.RemoteException { return null; }
                            @Override public android.os.IBinder asBinder() { return null; }
                        }
                        
                        /** Local-side IPC implementation stub class. */
                        public static abstract class Stub extends android.os.Binder implements org.autojs.autojs.core.shizuku.IUserService {
                            static final int TRANSACTION_destroy = android.os.IBinder.FIRST_CALL_TRANSACTION + 16777114;
                            static final int TRANSACTION_exit = android.os.IBinder.FIRST_CALL_TRANSACTION + 1;
                            static final int TRANSACTION_execCommand = android.os.IBinder.FIRST_CALL_TRANSACTION + 2;
                            static final int TRANSACTION_currentPackage = android.os.IBinder.FIRST_CALL_TRANSACTION + 11;
                            static final int TRANSACTION_currentActivity = android.os.IBinder.FIRST_CALL_TRANSACTION + 12;
                            static final int TRANSACTION_currentComponent = android.os.IBinder.FIRST_CALL_TRANSACTION + 13;
                            static final int TRANSACTION_currentComponentShort = android.os.IBinder.FIRST_CALL_TRANSACTION + 14;
                            
                            @SuppressWarnings("this-escape")
                            public Stub() {
                                this.attachInterface(this, DESCRIPTOR);
                            }
                            
                            public static org.autojs.autojs.core.shizuku.IUserService asInterface(android.os.IBinder obj) {
                                if ((obj==null)) {
                                    return null;
                                }
                                android.os.IInterface iin = obj.queryLocalInterface(DESCRIPTOR);
                                if (((iin!=null)&&(iin instanceof org.autojs.autojs.core.shizuku.IUserService))) {
                                    return ((org.autojs.autojs.core.shizuku.IUserService)iin);
                                }
                                return new org.autojs.autojs.core.shizuku.IUserService.Stub.Proxy(obj);
                            }
                            
                            @Override
                            public android.os.IBinder asBinder() {
                                return this;
                            }
                            
                            @Override
                            public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException {
                                java.lang.String descriptor = DESCRIPTOR;
                                if (code >= android.os.IBinder.FIRST_CALL_TRANSACTION && code <= android.os.IBinder.LAST_CALL_TRANSACTION) {
                                    data.enforceInterface(descriptor);
                                }
                                if (code == INTERFACE_TRANSACTION) {
                                    reply.writeString(descriptor);
                                    return true;
                                }
                                switch (code) {
                                    case TRANSACTION_destroy: {
                                        this.destroy();
                                        reply.writeNoException();
                                        break;
                                    }
                                    case TRANSACTION_exit: {
                                        this.exit();
                                        reply.writeNoException();
                                        break;
                                    }
                                    case TRANSACTION_execCommand: {
                                        String _arg0;
                                        _arg0 = data.readString();
                                        String _result = this.execCommand(_arg0);
                                        reply.writeNoException();
                                        reply.writeString(_result);
                                        break;
                                    }
                                    case TRANSACTION_currentPackage: {
                                        String _result = this.currentPackage();
                                        reply.writeNoException();
                                        reply.writeString(_result);
                                        break;
                                    }
                                    case TRANSACTION_currentActivity: {
                                        String _result = this.currentActivity();
                                        reply.writeNoException();
                                        reply.writeString(_result);
                                        break;
                                    }
                                    case TRANSACTION_currentComponent: {
                                        String _result = this.currentComponent();
                                        reply.writeNoException();
                                        reply.writeString(_result);
                                        break;
                                    }
                                    case TRANSACTION_currentComponentShort: {
                                        String _result = this.currentComponentShort();
                                        reply.writeNoException();
                                        reply.writeString(_result);
                                        break;
                                    }
                                    default: {
                                        return super.onTransact(code, data, reply, flags);
                                    }
                                }
                                return true;
                            }
                            
                            private static class Proxy implements org.autojs.autojs.core.shizuku.IUserService {
                                private android.os.IBinder mRemote;
                                
                                Proxy(android.os.IBinder remote) {
                                    mRemote = remote;
                                }
                                
                                @Override
                                public android.os.IBinder asBinder() {
                                    return mRemote;
                                }
                                
                                public String getInterfaceDescriptor() {
                                    return DESCRIPTOR;
                                }
                                
                                @Override
                                public void destroy() throws android.os.RemoteException {
                                    android.os.Parcel _data = android.os.Parcel.obtain();
                                    android.os.Parcel _reply = android.os.Parcel.obtain();
                                    try {
                                        _data.writeInterfaceToken(DESCRIPTOR);
                                        boolean _status = mRemote.transact(Stub.TRANSACTION_destroy, _data, _reply, 0);
                                        _reply.readException();
                                    }
                                    finally {
                                        _reply.recycle();
                                        _data.recycle();
                                    }
                                }
                                
                                @Override
                                public void exit() throws android.os.RemoteException {
                                    android.os.Parcel _data = android.os.Parcel.obtain();
                                    android.os.Parcel _reply = android.os.Parcel.obtain();
                                    try {
                                        _data.writeInterfaceToken(DESCRIPTOR);
                                        boolean _status = mRemote.transact(Stub.TRANSACTION_exit, _data, _reply, 0);
                                        _reply.readException();
                                    }
                                    finally {
                                        _reply.recycle();
                                        _data.recycle();
                                    }
                                }
                                
                                @Override
                                public String execCommand(String command) throws android.os.RemoteException {
                                    android.os.Parcel _data = android.os.Parcel.obtain();
                                    android.os.Parcel _reply = android.os.Parcel.obtain();
                                    String _result;
                                    try {
                                        _data.writeInterfaceToken(DESCRIPTOR);
                                        _data.writeString(command);
                                        boolean _status = mRemote.transact(Stub.TRANSACTION_execCommand, _data, _reply, 0);
                                        _reply.readException();
                                        _result = _reply.readString();
                                    }
                                    finally {
                                        _reply.recycle();
                                        _data.recycle();
                                    }
                                    return _result;
                                }
                                
                                @Override
                                public String currentPackage() throws android.os.RemoteException {
                                    android.os.Parcel _data = android.os.Parcel.obtain();
                                    android.os.Parcel _reply = android.os.Parcel.obtain();
                                    String _result;
                                    try {
                                        _data.writeInterfaceToken(DESCRIPTOR);
                                        boolean _status = mRemote.transact(Stub.TRANSACTION_currentPackage, _data, _reply, 0);
                                        _reply.readException();
                                        _result = _reply.readString();
                                    }
                                    finally {
                                        _reply.recycle();
                                        _data.recycle();
                                    }
                                    return _result;
                                }
                                
                                @Override
                                public String currentActivity() throws android.os.RemoteException {
                                    android.os.Parcel _data = android.os.Parcel.obtain();
                                    android.os.Parcel _reply = android.os.Parcel.obtain();
                                    String _result;
                                    try {
                                        _data.writeInterfaceToken(DESCRIPTOR);
                                        boolean _status = mRemote.transact(Stub.TRANSACTION_currentActivity, _data, _reply, 0);
                                        _reply.readException();
                                        _result = _reply.readString();
                                    }
                                    finally {
                                        _reply.recycle();
                                        _data.recycle();
                                    }
                                    return _result;
                                }
                                
                                @Override
                                public String currentComponent() throws android.os.RemoteException {
                                    android.os.Parcel _data = android.os.Parcel.obtain();
                                    android.os.Parcel _reply = android.os.Parcel.obtain();
                                    String _result;
                                    try {
                                        _data.writeInterfaceToken(DESCRIPTOR);
                                        boolean _status = mRemote.transact(Stub.TRANSACTION_currentComponent, _data, _reply, 0);
                                        _reply.readException();
                                        _result = _reply.readString();
                                    }
                                    finally {
                                        _reply.recycle();
                                        _data.recycle();
                                    }
                                    return _result;
                                }
                                
                                @Override
                                public String currentComponentShort() throws android.os.RemoteException {
                                    android.os.Parcel _data = android.os.Parcel.obtain();
                                    android.os.Parcel _reply = android.os.Parcel.obtain();
                                    String _result;
                                    try {
                                        _data.writeInterfaceToken(DESCRIPTOR);
                                        boolean _status = mRemote.transact(Stub.TRANSACTION_currentComponentShort, _data, _reply, 0);
                                        _reply.readException();
                                        _result = _reply.readString();
                                    }
                                    finally {
                                        _reply.recycle();
                                        _data.recycle();
                                    }
                                    return _result;
                                }
                            }
                        }
                        /** @hide */
                        public static final java.lang.String DESCRIPTOR = "org.autojs.autojs.core.shizuku.IUserService";
                        public void destroy() throws android.os.RemoteException;
                        public void exit() throws android.os.RemoteException;
                        public String execCommand(String command) throws android.os.RemoteException;
                        public String currentPackage() throws android.os.RemoteException;
                        public String currentActivity() throws android.os.RemoteException;
                        public String currentComponent() throws android.os.RemoteException;
                        public String currentComponentShort() throws android.os.RemoteException;
                    }
                """.trimIndent())
                
                // 修改文件为只读，防止被覆盖
                generatedFile.setReadOnly()
                
                println("[AIDL修复] 文件已修复并设为只读")
            } else {
                println("[AIDL修复] 文件不存在，可能尚未生成: ${generatedFile.absolutePath}")
            }
        }
    }
    
    // 在compileJava前运行修复任务
    tasks.whenTaskAdded {
        if (name.startsWith("compileDebugJava") || name.startsWith("compileReleaseJava")) {
            dependsOn("fixGeneratedAidlJavaFile")
        }
    }

    // 我们仍然保留原来的AIDL文件修复任务
    tasks.register("fixAidlFile") {
        doLast {
            val aidlFilePath = "src/main/aidl/org/autojs/autojs/core/shizuku/IUserService.aidl"
            val aidlFile = File(project.projectDir, aidlFilePath)
            
            // 如果文件存在，我们替换其内容
            if (aidlFile.exists()) {
                println("[AIDL处理] 正在替换文件: ${aidlFile.absolutePath}")
                
                // 替换为简化版，去除可能导致问题的转义字符
                aidlFile.writeText("""
                    package org.autojs.autojs.core.shizuku;
                    
                    interface IUserService {
                        void destroy();
                        void exit();
                        String execCommand(String command);
                        String currentPackage();
                        String currentActivity();
                        String currentComponent();
                        String currentComponentShort();
                    }
                """.trimIndent())
                
                println("[AIDL处理] 文件已替换")
            } else {
                println("[AIDL处理] 文件不存在: ${aidlFile.absolutePath}")
            }
        }
    }
    
    // 确保在preBuild前先修复AIDL文件
    tasks.named("preBuild").configure {
        dependsOn("fixAidlFile")
    }

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = versions.javaVersion
        targetCompatibility = versions.javaVersion
    }

    // @Hint by SuperMonster003 on Sep 25, 2024.
    //  ! To maintain compatibility with lower versions of Gradle (such as 7.4.2).
    //  ! zh-CN: 为了兼容低版本 Gradle (如 7.4.2).
    //  # packaging { ... }
    @Suppress("DEPRECATION")
    packagingOptions {
        listOf(
            "META-INF/DEPENDENCIES",
            "META-INF/LICENSE",
            "META-INF/LICENSE.*",
            "META-INF/LICENSE-notice.*",
            "META-INF/license.*",
            "META-INF/NOTICE",
            "META-INF/NOTICE.*",
            "META-INF/notice.*",
            "META-INF/ASL2.0",
            "META-INF/*.kotlin_module",
            "lib/x86/libc++_shared.so",
            "lib/x86_64/libc++_shared.so",
            "lib/armeabi-v7a/libc++_shared.so",
            "lib/arm64-v8a/libc++_shared.so",
            "lib/armeabi/libc++_shared.so",
        ).let { resources.pickFirsts.addAll(it) }

        listOf(
            "com/**/*",
            "frameworks/**/*",
            "junit/**/*",
            "LICENSE-junit.txt",
            "spec.txt",
        ).let { resources.excludes.addAll(it) }

        if (gradle.startParameter.taskNames.any { it.contains(flavorNameInrt, true) }) {
            listOf(
                "**/prob_emit.txt", // Jieba Analysis (zh-CN: 结巴分词)
                "**/dict-chinese-*.db.gzip", // Jieba Analysis (zh-CN: 结巴分词)
            ).let { resources.excludes.addAll(it) }
        }

        jniLibs {
            // @Reference to kkevsekk1/AutoX (https://github.com/kkevsekk1/AutoX) by SuperMonster003 on Nov 16, 2023.
            //  ! https://github.com/kkevsekk1/AutoX/blob/a6d482189291b460c3be60970b74c5321d26e457/inrt/build.gradle.kts#L91
            excludes += "*"
            useLegacyPackaging = true
        }
    }

    kotlinOptions {
        jvmTarget = versions.javaVersion.toString()
        // freeCompilerArgs = listOf("-Xjvm-default=all-compatibility")
    }

    lint {
        abortOnError = false
    }

    signingConfigs {
        if (sign.isValid) {
            create(buildTypeRelease) {
                storeFile = sign.properties["storeFile"]?.let { file(it as String) }
                keyPassword = sign.properties["keyPassword"] as String
                keyAlias = sign.properties["keyAlias"] as String
                storePassword = sign.properties["storePassword"] as String
            }
        }
    }

    buildTypes {
        val proguardFiles = arrayOf<Any>(
            getDefaultProguardFile("proguard-android.txt"),
            "proguard-rules.pro",
        )
        val niceSigningConfig = takeIf { sign.isValid }?.let {
            signingConfigs.getByName(buildTypeRelease)
        }
        debug {
            isMinifyEnabled = getByName(buildTypeRelease).isMinifyEnabled
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
        release {
            isMinifyEnabled = false
            proguardFiles(*proguardFiles)
            niceSigningConfig?.let { signingConfig = it }
        }
    }

    buildFeatures {
        aidl = true
        viewBinding = true
        // @Hint by SuperMonster003 on Aug 14, 2023.
        //  ! Substitution of "android.defaults.buildfeatures.buildconfig=true"
        //  ! zh-CN: "android.defaults.buildfeatures.buildconfig=true" 的替代方案
        buildConfig = true
        // @Archived by SuperMonster003 on Sep 23, 2024.
        //  ! Jetpack Compose
        //  # compose = true
        //  # composeOptions {
        //  #     kotlinCompilerExtensionVersion = "1.5.12"
        //  # }
    }

    applicationVariants.all {
        mergeAssetsProvider.configure {
            doLast {
                mapOf(
                    "dir" to outputDir,
                    "includes" to when (variantName.startsWith(flavorNameInrt)) {
                        true -> listOf(
                            "mlkit-google-ocr-models/**/*",
                            "mlkit_barcode_models/**/*",
                            "models/**/*",
                            "modules/obsolete/**/*",
                            "openccdata/**/*",
                            "project/**/*",
                            "android-devices.db",
                            "autojs.keystore",
                            "**/prob_emit.txt", // Jieba Analysis (zh-CN: 结巴分词)
                            "**/dict-chinese-*.db.gzip", // Jieba Analysis (zh-CN: 结巴分词)
                        )
                        else -> listOf(
                            "declarations/**/*",
                            "sample/declarations/**/*",
                            "modules/obsolete/**/*",
                        )
                    },
                ).let { delete(fileTree(it)) }
            }
        }

        outputs.map { it as BaseVariantOutputImpl }.forEach {
            it.outputFileName = Utils.getOutputFileName(this@all as ApplicationVariantImpl, it)
        }
    }

    splits {
        // Configures multiple APKs based on ABI.
        abi {
            // Enables building multiple APKs per ABI.
            isEnable = /* isNotAssembleInrt */ !gradle.startParameter.taskNames.any {
                it.contains(Regex("^(:?$flavorNameApp:)?$buildActionAssemble$flavorNameInrt", IGNORE_CASE))
            }
            // By default, all ABIs are included, so use reset() and include to specify that we only
            // want APKs for x86 and x86_64.
            // Resets the list of ABIs that Gradle should create APKs for to none.
            reset()
            // Specifies a list of ABIs that Gradle should create APKs for.
            include("arm64-v8a", "x86_64", "armeabi-v7a", "x86", "armeabi")
            // Specifies that we do not want to also generate a universal APK that includes all ABIs.
            isUniversalApk = true
        }
    }

    externalNativeBuild {
        cmake {
            version = "3.10.2"
            // 可选：指定本地CMake路径，如果已经安装
            // path = "C:\\Users\\Administrator\\AppData\\Local\\Android\\Sdk\\cmake\\3.10.2"
        }
    }

}

tasks {
    withType(JavaCompile::class.java) {
        options.encoding = "UTF-8"
    }

    // 修改这里，使用新的任务名称
    afterEvaluate {
        tasks.matching { task ->
            task.name.startsWith("compile") && task.name.endsWith("JavaWithJavac")
        }.configureEach {
            dependsOn("fixAidlGeneratedFiles")
        }
    }

    withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        // @Archived by SuperMonster003 on Aug 14, 2024.
        //  # kotlinOptions { jvmTarget = versions.javaVersion.toString() }
        compilerOptions { jvmTarget.set(JvmTarget.valueOf("JVM_${versions.javaVersion}")) }
    }

    register<Copy>("appendDigestToReleasedFiles") {
        listOf(flavorNameApp, flavorNameInrt).forEach { flavorName ->
            val src = "$flavorName/$buildTypeRelease"
            val dst = "${src}s"
            val ext = Utils.FILE_EXTENSION_APK

            if (!file(src).isDirectory) {
                return@forEach
            }

            from(src); into(dst); include("*.$ext")

            rename { name ->
                Utils.digestCRC32(file("${src}/$name")).let { digest ->
                    name.replace("^(.+?)(\\.$ext)\$".toRegex(), "\$1-$digest\$2")
                }
            }

            doLast { println("Destination: ${file(dst)}") }
        }
    }

    register("fixAidlGeneratedFiles") {
        doLast {
            val aidlDir = file("build/generated/aidl_source_output_dir/appDebug/out")
            if (aidlDir.exists()) {
                aidlDir.walk()
                    .filter { it.name.endsWith(".java") }
                    .forEach { javaFile ->
                        val content = javaFile.readText()
                        val fixedContent = content.replace("\\", "/")
                        javaFile.writeText(fixedContent)
                    }
            }
        }
    }
}

extra {
    versions.handleIfNeeded(project, flavorNameApp, listOf(buildTypeDebug, buildTypeRelease))
}

gradle.beforeProject {
    extensions.extraProperties["compileSdk"] = versions.sdkVersionCompile
    extensions.extraProperties["minSdk"] = versions.sdkVersionMin
    extensions.extraProperties["targetSdk"] = versions.sdkVersionTarget
}

class Sign(filePath: String) {

    var isValid = false
        private set

    val properties = Properties().also { props ->
        File(filePath).takeIf { it.exists() }?.let {
            props.load(FileInputStream(it))
            isValid = props.isNotEmpty()
        }
    }

}

class Versions(filePath: String) {

    private val properties = Properties()
    private val file = File(filePath).apply {
        if (!canRead()) {
            throw FileNotFoundException("Cannot read file '$filePath'")
        }
        properties.load(FileInputStream(this))
    }

    val sdkVersionMin = properties["MIN_SDK_VERSION"].let { it as String }.toInt()
    val sdkVersionTarget = properties["TARGET_SDK_VERSION"].let { it as String }.toInt()
    val sdkVersionTargetInrt = properties["TARGET_SDK_VERSION_INRT"].let { it as String }.toInt()
    val sdkVersionCompile = properties["COMPILE_SDK_VERSION"].let { it as String }.toInt()
    val appVersionName = properties["VERSION_NAME"] as String
    val appVersionCode = properties["VERSION_BUILD"].let { it as String }.toInt()
    val vscodeExtRequiredVersion = properties["VSCODE_EXT_REQUIRED_VERSION"] as String

    private val currentVersionInt = JavaVersion.current().majorVersion.toInt()

    private val javaVersionMinSupported: Int = properties["JAVA_VERSION_MIN_SUPPORTED"]
        .let { it as String }.toInt()
        .also {
            if (currentVersionInt < it) {
                throw GradleException(
                    "Current Gradle JDK version ${JavaVersion.current()} does not meet " +
                            "the minimum requirement which $it is needed."
                )
            }
        }
    private val javaVersionMinSuggested: Int = properties["JAVA_VERSION_MIN_SUGGESTED"].let { it as String }.toInt()
    private val javaVersionMinRadical: Int = properties["JAVA_VERSION_MIN_RADICAL"].let { it as String }.toInt()
    private val javaVersionRaw = properties["JAVA_VERSION"] as String
    private var javaVersionInfoSuffix = ""

    val javaVersion: JavaVersion by lazy {
        var niceVersionInt = javaVersionRaw.toInt()
        var isFallback = false

        while (niceVersionInt > javaVersionMinSupported) {
            if (JvmTarget.values().any { it.name.contains(Regex("_$niceVersionInt$")) }) {
                break
            }
            niceVersionInt -= 1
            isFallback = true
        }

        if (isFallback) {
            javaVersionInfoSuffix += " [fallback]"
        }

        if (gradle.extra.has("gradleVersionToCoerceJavaVersion")) {
            (gradle.extra["gradleVersionToCoerceJavaVersion"] as? String)?.let {
                val maxGradleVersion = getMaxSupportedJavaVersion(it)
                if (niceVersionInt > maxGradleVersion) {
                    niceVersionInt = maxGradleVersion
                    javaVersionInfoSuffix += " [coerced]"
                }
            }
        }

        if (niceVersionInt > currentVersionInt) {
            niceVersionInt = currentVersionInt
            javaVersionInfoSuffix += " [consistent]"
        }

        gradle.beforeProject {
            extensions.extraProperties["javaVersion"] = niceVersionInt
        }

        JavaVersion.toVersion(niceVersionInt)
    }

    private var isBuildNumberAutoIncremented = false
    private val minBuildTimeGap = Utils.hours2Millis(0.75)

    private val isBuildGapEnough
        get() = properties["BUILD_TIME"]?.let {
            Date().time - (it as String).toLong() > minBuildTimeGap
        } == true

    init {
        if (currentVersionInt < javaVersionMinSuggested) {
            logger.error(
                "It is recommended to upgrade current Gradle JDK version ${JavaVersion.current()} to $javaVersionMinSuggested or higher${
                    if (javaVersionMinRadical > 0) " (but lower than $javaVersionMinRadical)" else ""
                }."
            )
        }
        if (javaVersionMinRadical in 1..currentVersionInt) {
            logger.error(
                "It is recommended to downgrade current Gradle JDK version $currentVersionInt " +
                        "to ${javaVersionMinRadical - 1}${if (javaVersionMinRadical - 1 > javaVersionMinSuggested) " or lower (but not lower than $javaVersionMinSuggested)" else ""}, " +
                        "as Gradle may be not compatible with JDK $javaVersionMinRadical${if (currentVersionInt > javaVersionMinRadical) " (and above)" else ""} for now."
            )
        }
    }

    fun showInfo() {
        val title = "Version information for AutoJs6 app library"

        val infoVerName = "Version name: $appVersionName"
        val infoVerCode = "Version code: ${if (isBuildNumberAutoIncremented) "${appVersionCode + 1} [auto-incremented]" else appVersionCode}"
        val infoVerSdk = "SDK versions: min [$sdkVersionMin] / target [$sdkVersionTarget] / compile [$sdkVersionCompile]"
        val infoVerJava = "Java version: $javaVersion$javaVersionInfoSuffix"

        val maxLength = arrayOf(title, infoVerName, infoVerCode, infoVerSdk, infoVerJava).maxOf { it.length }

        arrayOf(
            "=".repeat(maxLength),
            title,
            "-".repeat(maxLength),
            infoVerName,
            infoVerCode,
            infoVerSdk,
            infoVerJava,
            "=".repeat(maxLength),
            "",
        ).forEach { println(it) }
    }

    fun handleIfNeeded(project: Project, flavorName: String, targetBuildType: List<String>) {
        project.gradle.taskGraph.whenReady(object : Action<TaskExecutionGraph> {
            override fun execute(taskGraph: TaskExecutionGraph) {
                for (buildType in targetBuildType) {
                    if (taskGraph.hasTask(Utils.getAssembleFullTaskName(project.name, flavorName, buildType))) {
                        return appendToTask(project, flavorName, buildType)
                    }
                }
                return showInfo()
            }
        })
    }

    private fun appendToTask(project: Project, flavorName: String, buildType: String) {
        project.tasks.getByName(Utils.getAssembleTaskName(flavorName, buildType)).doLast {
            updateProperties()
            println()
            showInfo()
        }
    }

    private fun updateProperties() {
        if (isBuildGapEnough) {
            properties["VERSION_BUILD"] = "${appVersionCode + 1}"
            isBuildNumberAutoIncremented = true
        }
        properties["BUILD_TIME"] = "${Date().time}"
        properties.store(file.writer(), null)
    }

    private fun getMaxSupportedJavaVersion(gradleVersion: String): Int {

        /* https://docs.gradle.org/current/userguide/compatibility.html . */
        val presetVersionMap = listOf(
            17 to "7.3",
            18 to "7.5",
            19 to "7.6",
            20 to "8.3",
            21 to "8.5",
            22 to "8.8",
            23 to "8.10",
            24 to "8.11", /* Unofficial as of Mar 19, 2025. */
        )

        fun parseVersion(version: String) = version.split(Regex("[.-]")).map { it.toIntOrNull() ?: 0 }

        val inputGradleVersionInts = parseVersion(gradleVersion)

        var maxJavaVersion: Int = presetVersionMap.first().first

        for ((presetJavaVersion, presetGradleVersion) in presetVersionMap) {
            val presetGradleVersionInts: List<Int> = parseVersion(presetGradleVersion)

            for (i in presetGradleVersionInts.indices) {
                when {
                    i > inputGradleVersionInts.lastIndex -> {
                        break
                    }
                    inputGradleVersionInts[i] > presetGradleVersionInts[i] -> {
                        maxJavaVersion = presetJavaVersion
                        break
                    }
                    inputGradleVersionInts[i] < presetGradleVersionInts[i] -> {
                        break
                    }
                    i == presetGradleVersionInts.lastIndex -> {
                        maxJavaVersion = presetJavaVersion
                    }
                }
            }
        }

        return maxJavaVersion
    }

}

object Utils {

    const val FILE_EXTENSION_APK = "apk"

    fun hours2Millis(hour: Double) = hour * 3.6e6

    fun getDateString(format: String, zone: String): String {
        // e.g. May 23, 2011
        return SimpleDateFormat(format).apply { timeZone = TimeZone.getTimeZone(zone) }.format(Date())
    }

    fun getOutputFileName(variant: ApplicationVariantImpl, output: BaseVariantOutputImpl): String {
        val autojs = variant.applicationId.replace("^.+\\.(.+)$".toRegex(), "$1") // e.g. autojs6
        val version = variant.versionName.replace("\\s".toRegex(), "-") // e.g. 6.1.0
        val architecture = output.getFilter("ABI") ?: "universal"
        val extension = FILE_EXTENSION_APK

        return "$autojs-v$version-$architecture.$extension".lowercase(Locale.getDefault())
    }

    fun getAssembleTaskName(flavorName: String, buildType: String) = "assemble${capitalize(flavorName)}${capitalize(buildType)}"

    fun getAssembleFullTaskName(projectName: String, flavorName: String, buildType: String) = ":$projectName:${getAssembleTaskName(flavorName, buildType)}"

    fun digestCRC32(file: File): String {
        val fis = FileInputStream(file)
        val buffer = ByteArray(4096)
        var read: Int

        return CRC32().let { o ->
            while (fis.read(buffer).also { read = it } > 0) {
                o.update(buffer, 0, read)
            }
            String.format("%08x", o.value)
        }
    }

    private fun capitalize(s: String) = "${s[0].uppercase(Locale.getDefault())}${s.substring(1)}"

}
