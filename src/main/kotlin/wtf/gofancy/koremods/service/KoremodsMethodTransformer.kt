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

package wtf.gofancy.koremods.service

import cpw.mods.modlauncher.api.ITransformer
import cpw.mods.modlauncher.api.ITransformer.Target
import cpw.mods.modlauncher.api.ITransformerVotingContext
import org.objectweb.asm.tree.MethodNode
import wtf.gofancy.koremods.dsl.MethodTransformer

object KoremodsMethodTransformer : KoremodsBaseTransformer<MethodNode, KoremodsMethodTransformer.MethodKey, MethodTransformer>(MethodTransformer::class.java), ITransformer<MethodNode> {
    override fun groupKeys(input: MethodTransformer): MethodKey = MethodKey(input.targetClassName, input.name, input.desc)

    override fun getKey(input: MethodNode, context: ITransformerVotingContext): MethodKey = MethodKey(context.className, input.name, input.desc) 

    override fun getTarget(key: MethodKey): Target = Target.targetMethod(key.owner, key.name, key.desc)

    data class MethodKey(val owner: String, val name: String, val desc: String) {
        override fun toString(): String {
            return "$owner.$name$desc"
        }
    }
}
