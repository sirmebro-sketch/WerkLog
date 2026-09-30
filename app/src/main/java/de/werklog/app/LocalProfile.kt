package de.werklog.app

import org.json.JSONObject
import java.util.Base64

const val MAX_PROFILE_IMAGE_BYTES = 96 * 1024
/** Private identity, encrypted with the vault. Never included in an asset share. */
data class LocalProfile(val name: String = "", val role: String = "", val team: String = "",
    val company: String = "", val phone: String = "", val email: String = "", val image: String = "") : java.io.Serializable
fun profileJson(p: LocalProfile): JSONObject = JSONObject().put("name", p.name).put("role", p.role)
    .put("team", p.team).put("company", p.company).put("phone", p.phone).put("email", p.email).put("image", p.image)
fun readProfile(j: JSONObject?): LocalProfile = if (j == null) LocalProfile() else LocalProfile(
    j.optString("name"), j.optString("role"), j.optString("team"), j.optString("company"), j.optString("phone"), j.optString("email"), j.optString("image"))
fun validateProfile(p: LocalProfile) {
    require(listOf(p.name, p.role, p.team, p.company, p.phone, p.email).all { it.length <= 200 }) { "Profilfeld zu lang" }
    if (p.image.isNotEmpty()) {
        require(p.image.length <= (MAX_PROFILE_IMAGE_BYTES + 2) / 3 * 4)
        val bytes = Base64.getDecoder().decode(p.image)
        try { require(bytes.size <= MAX_PROFILE_IMAGE_BYTES); validateImageBytes(bytes) } finally { bytes.fill(0) }
    }
}
