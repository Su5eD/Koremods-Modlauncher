package dev.su5ed.koremods.modlaunch

import cpw.mods.modlauncher.api.ITransformer
import cpw.mods.modlauncher.api.ITransformer.Target
import cpw.mods.modlauncher.api.ITransformerVotingContext
import cpw.mods.modlauncher.api.TransformerVoteResult
import dev.su5ed.koremods.KoremodDiscoverer
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
        return KoremodDiscoverer.getFlatTransformers()
            .map(Transformer::targetClassName)
            .map(Target::targetClass)
            .toMutableSet()
    }
}
