pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Maven do DAT exige GitHub token (read:packages) — ver docs/LIMITACOES.md AND-07/DAT-08.
        val githubToken: String? = System.getenv("GITHUB_TOKEN")
            ?: java.util.Properties().let { props ->
                val f = file("local.properties")
                if (f.exists()) f.inputStream().use(props::load)
                props.getProperty("github_token")
            }
        if (githubToken != null) {
            maven {
                url = uri("https://maven.pkg.github.com/facebook/meta-wearables-dat-android")
                credentials {
                    username = ""
                    password = githubToken
                }
            }
        }
    }
}

rootProject.name = "prontuario-glasses"
include(":app")
