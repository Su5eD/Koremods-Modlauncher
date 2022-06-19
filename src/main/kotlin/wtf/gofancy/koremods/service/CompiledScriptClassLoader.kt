package wtf.gofancy.koremods.service

import java.io.InputStream
import java.nio.file.Path
import java.security.ProtectionDomain
import kotlin.io.path.inputStream

class CompiledScriptClassLoader(private val path: Path, parent: ClassLoader?) : ClassLoader(parent) {

    override fun findClass(name: String): Class<*> {
        val resource = name.replace('.', '/') + ".class"
        val bytes = getResourceAsStream(resource)?.use(InputStream::readBytes) ?: throw ClassNotFoundException(name)

        val protectionDomain = ProtectionDomain(null, null)
        return defineClass(name, bytes, 0, bytes.size, protectionDomain)
    }

    override fun getResourceAsStream(name: String): InputStream? {
        return try {
            path.resolve(name).inputStream()
        } catch (e: NoSuchFileException) {
            null
        }
    }
}