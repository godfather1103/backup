fun RepositoryHandler.enableMirror() {
    all {
        if (this is MavenArtifactRepository) {
            val originalUrl = this.url.toString().removeSuffix("/")
            urlMappings[originalUrl]?.let {
                logger.lifecycle("Repository[$url] is mirrored to $it")
                this.setUrl(it)
            }
        }
    }
}

val urlMappings = mapOf(
    "https://repo.maven.apache.org/maven2" to "https://maven.aliyun.com/repository/public/",
    "https://dl.google.com/dl/android/maven2" to "https://maven.aliyun.com/repository/google/",
    "https://plugins.gradle.org/m2" to "https://maven.aliyun.com/repository/gradle-plugin/"
)

gradle.allprojects {
    buildscript {
        repositories.enableMirror()
    }
    repositories.enableMirror()
}

gradle.beforeSettings { 
    pluginManagement.repositories.enableMirror()
    dependencyResolutionManagement.repositories.enableMirror()
}

allprojects {
    configurations.all {
        resolutionStrategy {
            // 变更中版本的依赖强制更新(快照版或声明为变更版本的依赖)
            cacheChangingModulesFor(24, TimeUnit.HOURS)
            // 动态版本声明的依赖缓存1小时(区间声明的依赖)
            cacheDynamicVersionsFor(24, TimeUnit.HOURS)
        }
    }
}
