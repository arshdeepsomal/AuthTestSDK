package com.devconsole.auth_sdk.testutil

import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.security.Security

class SecurityProviderRule : TestRule {
    private val provider = BouncyCastleProvider()

    override fun apply(base: Statement, description: Description): Statement {
        return object : Statement() {
            override fun evaluate() {
                val wasPresent = Security.getProvider(provider.name) != null
                if (!wasPresent) {
                    Security.addProvider(provider)
                }
                try {
                    base.evaluate()
                } finally {
                    if (!wasPresent) {
                        Security.removeProvider(provider.name)
                    }
                }
            }
        }
    }
}
