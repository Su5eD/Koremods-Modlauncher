package wtf.gofancy.koremods.service

import cpw.mods.modlauncher.api.ITransformer
import cpw.mods.modlauncher.api.ITransformer.Target
import cpw.mods.modlauncher.api.ITransformerVotingContext
import cpw.mods.modlauncher.api.TransformerVoteResult
import wtf.gofancy.koremods.KoremodsDiscoverer
import wtf.gofancy.koremods.applyTransform
import wtf.gofancy.koremods.dsl.Transformer

abstract class KoremodsBaseTransformer<T, K : Any, V : Transformer<T>>(cls: Class<V>) : ITransformer<T> {
    private val transformers: Map<K, List<V>> = (KoremodsDiscoverer.INSTANCE?.getFlatTransformers() ?: emptyList())
        .filterIsInstance(cls)
        .groupBy(::groupKeys)

    abstract fun groupKeys(input: V): K
    
    abstract fun getKey(input: T, context: ITransformerVotingContext): K
    
    abstract fun getTarget(key: K): Target
    
    override fun transform(input: T, context: ITransformerVotingContext): T {
        val key: K = getKey(input, context)
        val list = transformers[key] ?: throw IllegalStateException("No transformers for $input found. Have they disappeared?")

        applyTransform(key, list, input)

        return input
    }

    override fun castVote(context: ITransformerVotingContext): TransformerVoteResult = TransformerVoteResult.YES

    override fun targets(): Set<Target> = transformers.keys
        .map(::getTarget)
        .toSet()
}
