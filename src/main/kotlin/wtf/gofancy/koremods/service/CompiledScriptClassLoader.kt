/*
 * This file is part of Koremods, licensed under the MIT License
 *
 * Copyright (c) 2021-2022 Garden of Fancy
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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