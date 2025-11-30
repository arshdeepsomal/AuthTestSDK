package com.devconsole.auth_sdk.auth.client

import android.content.Context
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthorizationService
import net.openid.appauth.browser.AnyBrowserMatcher
import net.openid.appauth.browser.BrowserAllowList
import net.openid.appauth.browser.VersionedBrowserMatcher

internal object DefaultAuthServiceProvider : AuthServiceProvider {

    override fun provide(context: Context): AuthorizationService {
        val config = AppAuthConfiguration.Builder()
            .setBrowserMatcher(
                BrowserAllowList(
                    VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                    VersionedBrowserMatcher.FIREFOX_CUSTOM_TAB,
                    VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB,
                    VersionedBrowserMatcher.CHROME_BROWSER,
                    VersionedBrowserMatcher.FIREFOX_BROWSER,
                    VersionedBrowserMatcher.SAMSUNG_BROWSER,
                    AnyBrowserMatcher.INSTANCE,
                )
            )
            .build()

        return AuthorizationService(context, config)
    }
}
