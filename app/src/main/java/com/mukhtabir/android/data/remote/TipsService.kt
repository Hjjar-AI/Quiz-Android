package com.mukhtabir.android.data.remote

/**
 * Tips for the dashboard "Did you know?" card.
 *
 * The backend accepts `?locale=ar|en`, returns the requested locale's
 * tips, and echoes the resolved locale in the payload. When the
 * requested locale has no active tips the backend falls back to the
 * default locale and reports that in the `locale` field — the client
 * trusts the returned list rather than re-checking the locale.
 */
class TipsService(private val api: ApiClient) : LearnerTips {
    override suspend fun forLocale(locale: String): TipsResponseDto = api.decode(
        TipsResponseDto.serializer(),
        api.get(
            ApiEndpoints.TIPS,
            mapOf("locale" to locale),
        ),
    )
}
