/*
 * This file is part of Koremods, licensed under the MIT License
 *
 * Copyright (c) 2021-2023 Garden of Fancy
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

package wtf.gofancy.koremods.modlauncher.dsl

import cpw.mods.modlauncher.Launcher
import cpw.mods.modlauncher.api.INameMappingService.Domain
import org.objectweb.asm.Type
import wtf.gofancy.koremods.dsl.TransformerPropertyKeys
import java.util.*
import kotlin.script.experimental.util.PropertiesCollection

val TransformerPropertyKeys.autoRemap by PropertiesCollection.key(true)

fun mapClassName(name: String): String {
    return map(name, Domain.CLASS)
}

fun mapMethodName(name: String): String {
    return map(name, Domain.METHOD)
}

fun mapFieldName(name: String): String {
    return map(name, Domain.FIELD)
}

fun mapMethodDesc(methodDescriptor: String): String {
    val stringBuilder = StringBuilder("(")
    Type.getArgumentTypes(methodDescriptor)
        .map(::mapType)
        .forEach(stringBuilder::append)
    val returnType = Type.getReturnType(methodDescriptor)
    stringBuilder.append(")").append(mapType(returnType))
    return stringBuilder.toString()
}

private fun map(name: String, domain: Domain): String {
    return Optional.ofNullable(Launcher.INSTANCE)
        .map(Launcher::environment)
        .flatMap { env -> env.findNameMapping("srg") }
        .map { f -> f.apply(domain, name) }
        .orElse(name)
}

private fun mapType(type: Type): Type {
    return if (type.sort == Type.OBJECT) {
        Type.getObjectType(mapClassName(type.className).replace('.', '/'))
    } else type
}
