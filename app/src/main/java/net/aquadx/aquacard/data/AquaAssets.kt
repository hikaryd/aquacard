package net.aquadx.aquacard.data

/**
 * URL ассетов AquaDX (maimai) — обложки песен и портреты игроков.
 * Хост ассетов выводится из API-базы через [AquaProfileRepository.staticBase]
 * (для https://aquadx.net/aqua → https://aquadx.net; для self-hosted следует baseUrl).
 * Источники паттернов — live-verified, см. docs/profile-enhancements-plan.md §2.
 */
object AquaAssets {

    /**
     * Обложка песни maimai: {DATA_HOST}/d/mai2/music/{file}.png,
     * где file = (musicId % 10000) с ведущими нулями до 6 цифр. Одна обложка на песню
     * (не зависит от сложности). Lossy для musicId ≥ 10000 — плейсхолдер поглощает 404.
     */
    fun jacketUrl(baseUrl: String, musicId: Int): String {
        val file = (musicId % 10000).toString().padStart(6, '0')
        return "${AquaProfileRepository.staticBase(baseUrl)}/d/mai2/music/$file.png"
    }

    /**
     * Портрет игрока: {AQUA_HOST}/uploads/net/portrait/{profilePicture}.
     * Если profilePicture пуст/null — вернуть null (UI покажет локальный InitialsAvatar,
     * не дёргая /portrait/null).
     */
    fun avatarUrlOrNull(baseUrl: String, profilePicture: String?): String? {
        val pfp = profilePicture?.takeIf { it.isNotBlank() } ?: return null
        return "${AquaProfileRepository.staticBase(baseUrl)}/uploads/net/portrait/$pfp"
    }
}
