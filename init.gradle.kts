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
    // "https://repo.maven.apache.org/maven2" to "https://maven.aliyun.com/repository/public/",
    "https://repo.maven.apache.org/maven2" to "https://mirrors.huaweicloud.com/repository/maven/",
    "https://dl.google.com/dl/android/maven2" to "https://maven.aliyun.com/repository/google/",
    // "https://plugins.gradle.org/m2" to "https://maven.aliyun.com/repository/gradle-plugin/",
    "https://jcenter.bintray.com" to "https://maven.aliyun.com/repository/jcenter/"
)

gradle.allprojects {
    buildscript {
        repositories?.enableMirror()
    }
    repositories?.enableMirror()
}

gradle.beforeSettings { 
    pluginManagement.repositories.enableMirror()
    // getDependencyResolutionManagement 从6.8版本开始提供，所以只能通过反射的方式来获取，避免低版本出现异常
    Settings::class.java
            .methods
            .find { it.name == "getDependencyResolutionManagement" }
            ?.invoke(this)?.let {
                it.javaClass.methods.find { m -> m.name == "getRepositories" }?.invoke(it) as RepositoryHandler
            }?.enableMirror()
}
