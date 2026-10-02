buildscript {
    configurations.classpath {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.json:json:20160810"))
                .using(module("org.json:json:20231013"))
                .because("Use the maintained FLOSS org.json release for the build classpath")
        }
    }
}

plugins {
    id("com.android.application") version "8.8.2" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.0" apply false
    id("com.chaquo.python") version "17.0.0" apply false
}
