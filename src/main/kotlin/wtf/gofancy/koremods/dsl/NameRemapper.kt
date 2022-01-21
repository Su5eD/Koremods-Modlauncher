package wtf.gofancy.koremods.dsl

import cpw.mods.modlauncher.Launcher
import cpw.mods.modlauncher.api.INameMappingService.Domain
import java.util.*

fun mapClassName(name: String): String {
    return map(name, Domain.CLASS)
}

fun mapMethodName(name: String): String {
    return map(name, Domain.METHOD)
}

fun mapFieldName(name: String): String {
    return map(name, Domain.FIELD)
}

private fun map(name: String, domain: Domain): String {
    return Optional.ofNullable(Launcher.INSTANCE)
        .map(Launcher::environment)
        .flatMap { env -> env.findNameMapping("srg") }
        .map { f -> f.apply(domain, name) }
        .orElse(name)
}
