package io.github.crankshift.niagaraprivatespace.finder

import io.github.crankshift.niagaraprivatespace.decision.Hook
import java.io.File
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.abort
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.function.Executable
import org.luckypray.dexkit.DexKitBridge

/**
 * Runs the [Finder] against a Niagara APK on the maintainer's Mac and checks that every hook resolves. The finder
 * reports a hook only when its fingerprint matched exactly once, so a hook that is not reported is either missing
 * or ambiguous. Niagara's APK must never be committed, so the test is skipped while `NIAGARA_APK` or
 * `DEXKIT_LIBRARY` is unset, and fails when one is set to something that is not a file.
 * How to pull the APK and build the library: `docs/fingerprint-check.md`.
 */
class FingerprintTest {
    @Test
    fun `every fingerprint resolves to exactly one target in Niagara's APK`() {
        val apk = configuredFile("NIAGARA_APK")
        val library = configuredFile("DEXKIT_LIBRARY")

        System.load(library.absolutePath)
        DexKitBridge.create(apk.absolutePath).use { bridge ->
            val errors = mutableListOf<String>()
            val resolved = Finder(bridge) { what, error -> errors += "$what: $error" }.resolve()

            assertAll(
                Hook.entries.map { hook ->
                    Executable {
                        assertTrue(hook in resolved.report.found, "$hook is missing or matched more than once; finder errors: $errors")
                    }
                },
            )
        }
    }

    private fun configuredFile(variable: String): File {
        val path = System.getenv(variable)?.takeIf { it.isNotBlank() } ?: abort("$variable is not set")
        return File(path).also { check(it.isFile) { "$variable is set to $path, which is not a file" } }
    }
}
