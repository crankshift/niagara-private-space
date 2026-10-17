package io.github.crankshift.niagaraprivatespace.finder

import io.github.crankshift.niagaraprivatespace.decision.Hook
import io.github.crankshift.niagaraprivatespace.decision.ResolutionReport
import org.luckypray.dexkit.result.FieldData
import org.luckypray.dexkit.result.MethodData

/**
 * What the [Finder] located in Niagara. Method hooks hold one or more targets; field hooks hold exactly one.
 * Targets stay as DexKit descriptions so the Finder can run without loading Niagara's classes; the hook
 * adapters turn them into reflection objects with Niagara's class loader.
 */
class Resolved(
    val methods: Map<Hook, List<MethodData>>,
    val fields: Map<Hook, FieldData>,
) {
    /** The hooks that resolved to at least one target. */
    val report = ResolutionReport(methods.filterValues { it.isNotEmpty() }.keys + fields.keys)

    fun methodsOf(hook: Hook): List<MethodData> = methods[hook].orEmpty()

    fun fieldOf(hook: Hook): FieldData? = fields[hook]
}
