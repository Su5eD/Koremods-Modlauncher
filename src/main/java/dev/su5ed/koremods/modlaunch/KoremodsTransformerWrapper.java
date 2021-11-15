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

package dev.su5ed.koremods.modlaunch;

import cpw.mods.modlauncher.api.ITransformer;
import cpw.mods.modlauncher.api.ITransformerVotingContext;
import cpw.mods.modlauncher.api.TransformerVoteResult;
import dev.su5ed.koremods.prelaunch.KoremodsPrelaunch;
import net.minecraftforge.common.util.Lazy;
import org.jetbrains.annotations.NotNull;
import org.objectweb.asm.tree.ClassNode;

import java.util.Set;

public class KoremodsTransformerWrapper implements ITransformer<ClassNode> {
    private static final String TRANSFORMER_CLASS = "dev.su5ed.koremods.modlaunch.KoremodsTransformer";
    private final Lazy<ITransformer<ClassNode>> actualTransformer = Lazy.of(() -> {
        try {
            Class<?> cls = KoremodsPrelaunch.dependencyClassLoader.loadClass(TRANSFORMER_CLASS);
            //noinspection unchecked
            return (ITransformer<ClassNode>) cls.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot initialize KoremodsTransformer", e);
        }
    });
    
    @NotNull
    @Override
    public ClassNode transform(ClassNode input, ITransformerVotingContext context) {
        return this.actualTransformer.get().transform(input, context);
    }
    
    @Override
    public TransformerVoteResult castVote(ITransformerVotingContext context) {
        return this.actualTransformer.get().castVote(context);
    }

    @NotNull
    @Override
    public Set<Target> targets() {
        return this.actualTransformer.get().targets();
    }
}
