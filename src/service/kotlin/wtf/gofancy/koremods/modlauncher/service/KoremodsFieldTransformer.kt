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

package wtf.gofancy.koremods.modlauncher.service

import cpw.mods.modlauncher.api.ITransformer
import cpw.mods.modlauncher.api.ITransformer.Target
import cpw.mods.modlauncher.api.ITransformerVotingContext
import org.objectweb.asm.tree.FieldNode
import wtf.gofancy.koremods.dsl.FieldTransformer

object KoremodsFieldTransformer : KoremodsBaseTransformer<FieldNode, KoremodsFieldTransformer.FieldKey, FieldTransformer>(FieldTransformer::class.java), ITransformer<FieldNode> {
    override fun groupKeys(input: FieldTransformer): FieldKey = FieldKey(input.targetClassName, input.name)

    override fun getKey(input: FieldNode, context: ITransformerVotingContext): FieldKey = FieldKey(context.className, input.name) 

    override fun getTarget(key: FieldKey): Target = Target.targetField(key.owner, key.name)

    data class FieldKey(val owner: String, val name: String) {
        override fun toString(): String {
            return "$owner.$name"
        }
    }
}
