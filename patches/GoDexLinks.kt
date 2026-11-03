package br.com.pokegobr

/**
 * Public site entrypoints verified on 2026-11-03.
 * There is no authenticated GoDex API integration in this build.
 */
internal object GoDexLinks {
    const val home = "https://godex.site/pt-BR"
    const val shinyDex = "https://godex.site/shinydex"
    const val maxDex = "https://godex.site/maxdex"
    const val publicCollectionExample =
        "https://godex.site/public-collection/30fceaa65b9db02479d8042198dd7f28"

    private val publicCollection = Regex(
        """^https://godex\.site/public-collection/[a-zA-Z0-9-]{16,64}/?$"""
    )

    /**
     * Only allow opening first-party public-collection links typed by the user.
     * Do not accept redirects, arbitrary URI schemes, query tokens or fragments.
     */
    fun validPublicCollection(url: String): String? {
        val normalized = url.trim()
        return normalized.takeIf {
            it.length <= 140 && publicCollection.matches(it)
        }
    }
}
