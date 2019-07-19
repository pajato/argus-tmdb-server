import org.gradle.api.Project

class SSLHelper(val project: Project) {
    val hostNameKey = "hostName"
    val isProductionServer = project.hasProperty(hostNameKey) && project.property(hostNameKey) == "einstein.local"

    fun getKeyStorePath(): String =
        if (isProductionServer)  "/etc/letsencrypt/live/tmdb.pajato.com/keystore.jks" else "build/keystore.jks"

    fun getKeyStorePassword(): String =
        if (isProductionServer) "${project.property("tmdbServerKeystorePassword") ?: ""}" else "abc123"

    fun getPrivateKeyPassword(): String =
        if (isProductionServer) "${project.property("tmdbServerPrivateKeyPassword") ?: ""}" else "xyz678"

    init {
        println("Is production server: $isProductionServer")
        println("Key store path: ${getKeyStorePath()}")
        println("Key store password: ${getKeyStorePassword()}")
        println("Private key password: ${getPrivateKeyPassword()}")
    }
}