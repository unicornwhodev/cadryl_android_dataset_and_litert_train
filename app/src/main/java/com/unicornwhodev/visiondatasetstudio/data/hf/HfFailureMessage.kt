package com.unicornwhodev.visiondatasetstudio.data.hf

import com.unicornwhodev.visiondatasetstudio.core.i18n.tr

/** No remote body, signed URL or credential is exposed in user-facing diagnostics. */
object HfFailureMessage {
    fun describe(code: Int, writing: Boolean = false): String = when (code) {
        401 -> tr("Connexion HF requise ou jeton expiré (HTTP 401). Reconnectez votre compte dans Destination.",
            "HF sign-in required or token expired (HTTP 401). Reconnect your account in Destination.")
        403 -> if (writing) tr("Écriture refusée (HTTP 403). Vérifiez que le jeton et votre compte peuvent écrire dans ce dépôt. Pour une URL de stockage expirée, reprenez l’envoi. Les fichiers locaux sont conservés.",
            "Write denied (HTTP 403). Check that the token and your account can write to this repository. For an expired storage URL, resume the upload. Local files are preserved.")
        else tr("Lecture refusée (HTTP 403). Vérifiez les droits du compte, du jeton et les conditions d’accès au dataset sur Hugging Face.",
            "Read denied (HTTP 403). Check account and token permissions and the dataset's access conditions on Hugging Face.")
        404 -> tr("Ressource introuvable ou privée (HTTP 404). Vérifiez le dépôt, la configuration, le split et le compte connecté.",
            "Resource missing or private (HTTP 404). Check the repository, configuration, split and signed-in account.")
        429 -> tr("Trop de requêtes (HTTP 429). Attendez puis reprenez le transfert.", "Too many requests (HTTP 429). Wait, then resume the transfer.")
        else -> tr("Service distant indisponible (HTTP $code). Réessayez ; les données locales sont conservées.",
            "Remote service unavailable (HTTP $code). Retry; local data is preserved.")
    }
}
