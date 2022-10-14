package io.gitlab.arturbosch.detekt.rules.bugs

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution

@RequiresTypeResolution
// Not active by default (yet?): @ActiveByDefault(since = "1.23.0")
class CloseableRequiresUse(config: Config) : Rule(config) {

    override val issue = Issue(
        "CloseableRequiresUse",
        Severity.Defect,
        "Owned Closeable instances must be invoked with .use { }; see documentation for definitions of 'owned'.",
        Debt.FIVE_MINS
    )

}
