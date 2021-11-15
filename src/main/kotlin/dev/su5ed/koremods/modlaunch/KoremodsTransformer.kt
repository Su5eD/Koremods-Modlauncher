/*
 * This file is part of Koremods, licensed under the MIT License
 *
 * Copyright (c) 2021 Garden of Fancy
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

package dev.su5ed.koremods.modlaunch

import cpw.mods.modlauncher.api.ITransformer
import cpw.mods.modlauncher.api.ITransformer.Target
import cpw.mods.modlauncher.api.ITransformerVotingContext
import cpw.mods.modlauncher.api.TransformerVoteResult
import dev.su5ed.koremods.KoremodsDiscoverer
import dev.su5ed.koremods.dsl.Transformer
import dev.su5ed.koremods.transformClass
import org.objectweb.asm.tree.ClassNode

class KoremodsTransformer : ITransformer<ClassNode> {
    
    override fun transform(node: ClassNode, context: ITransformerVotingContext): ClassNode {
        // TODO Logging
        transformClass(node.name, node)
        return node
    }

    override fun castVote(context: ITransformerVotingContext): TransformerVoteResult = TransformerVoteResult.YES

    override fun targets(): Set<Target> {
        return KoremodsDiscoverer.getFlatTransformers()
            .map(Transformer::targetClassName)
            .map(Target::targetClass)
            .toMutableSet()
    }
}
