package com.ovhsms.app

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class MainActivity : AppCompatActivity() {

    // ─── UI Elements ───────────────────────────────────────────────────────────
    private lateinit var etServiceName:  EditText
    private lateinit var etLogin:        EditText
    private lateinit var etPassword:     EditText
    private lateinit var etRecipient:    EditText
    private lateinit var etMessage:      EditText
    private lateinit var tvCharCount:    TextView
    private lateinit var btnSend:        Button
    private lateinit var progressBar:    ProgressBar
    private lateinit var tvStatus:       TextView
    private lateinit var cardStatus:     View
    private lateinit var tvCreditsInfo:  TextView

    // ─── OVH SMS Config ────────────────────────────────────────────────────────
    private val OVH_SMS_API_BASE = "https://www.ovh.com/cgi-bin/sms/http2sms.cgi"
    private val MAX_SMS_CHARS    = 160

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // ───────────────────────────────────────────────────────────────────────────
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bindViews()
        setupCharCounter()
        setupSendButton()
    }

    // ─── Bind Views ────────────────────────────────────────────────────────────
    private fun bindViews() {
        etServiceName  = findViewById(R.id.etServiceName)
        etLogin        = findViewById(R.id.etLogin)
        etPassword     = findViewById(R.id.etPassword)
        etRecipient    = findViewById(R.id.etRecipient)
        etMessage      = findViewById(R.id.etMessage)
        tvCharCount    = findViewById(R.id.tvCharCount)
        btnSend        = findViewById(R.id.btnSend)
        progressBar    = findViewById(R.id.progressBar)
        tvStatus       = findViewById(R.id.tvStatus)
        cardStatus     = findViewById(R.id.cardStatus)
        tvCreditsInfo  = findViewById(R.id.tvCreditsInfo)
    }

    // ─── Character Counter ─────────────────────────────────────────────────────
    private fun setupCharCounter() {
        etMessage.addTextChangedListener(object : android.text.TextWatcher {
            override fun afterTextChanged(s: android.text.Editable?) {
                val len = s?.length ?: 0
                val smsCount = if (len == 0) 1 else Math.ceil(len.toDouble() / MAX_SMS_CHARS).toInt()
                tvCharCount.text = "$len / $MAX_SMS_CHARS  •  $smsCount SMS"
                tvCharCount.setTextColor(
                    if (len > MAX_SMS_CHARS)
                        ContextCompat.getColor(this@MainActivity, R.color.warning)
                    else
                        ContextCompat.getColor(this@MainActivity, R.color.text_secondary)
                )
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    // ─── Send Button ───────────────────────────────────────────────────────────
    private fun setupSendButton() {
        btnSend.setOnClickListener { sendSms() }
    }

    // ─── Validate Inputs ───────────────────────────────────────────────────────
    private fun validate(): Boolean {
        val serviceName = etServiceName.text.toString().trim()
        val login       = etLogin.text.toString().trim()
        val password    = etPassword.text.toString().trim()
        val recipient   = etRecipient.text.toString().trim()
        val message     = etMessage.text.toString().trim()

        return when {
            serviceName.isEmpty() -> { showError("Renseignez le nom du service SMS OVH"); false }
            login.isEmpty()       -> { showError("Renseignez le login OVH"); false }
            password.isEmpty()    -> { showError("Renseignez le mot de passe OVH"); false }
            recipient.isEmpty()   -> { showError("Renseignez le numéro destinataire"); false }
            !recipient.startsWith("+") -> {
                showError("Format international requis : +336XXXXXXXX")
                false
            }
            message.isEmpty()     -> { showError("Rédigez votre message"); false }
            serviceName.length > 11 -> {
                showError("Le Sender ID ne peut pas dépasser 11 caractères")
                false
            }
            else -> true
        }
    }

    // ─── Send SMS ──────────────────────────────────────────────────────────────
    private fun sendSms() {
        if (!validate()) return
        setLoading(true)
        hideStatus()

        val serviceName = etServiceName.text.toString().trim()
        val login       = etLogin.text.toString().trim()
        val password    = etPassword.text.toString().trim()
        val recipient   = etRecipient.text.toString().trim()
        val message     = etMessage.text.toString().trim()

        scope.launch {
            val result = withContext(Dispatchers.IO) {
                callOvhApi(
                    serviceName = serviceName,
                    login       = login,
                    password    = password,
                    recipient   = recipient,
                    message     = message
                )
            }
            setLoading(false)
            handleResult(result)
        }
    }

    // ─── OVH HTTP API Call ─────────────────────────────────────────────────────
    private fun callOvhApi(
        serviceName: String,
        login: String,
        password: String,
        recipient: String,
        message: String
    ): OvhResult {
        return try {
            val params = mapOf(
                "account"  to serviceName,
                "login"    to login,
                "password" to password,
                "from"     to serviceName,   // Sender ID alphanumérique
                "to"       to recipient,
                "msg"      to message,
                "contentType" to "text/json"
            )

            val query = params.entries.joinToString("&") {
                "${URLEncoder.encode(it.key, "UTF-8")}=${URLEncoder.encode(it.value, "UTF-8")}"
            }

            val url = URL("$OVH_SMS_API_BASE?$query")
            val conn = url.openConnection() as HttpURLConnection
            conn.apply {
                requestMethod  = "GET"
                connectTimeout = 15_000
                readTimeout    = 15_000
            }

            val responseCode = conn.responseCode
            val responseBody = conn.inputStream.bufferedReader().readText()
            conn.disconnect()

            parseOvhResponse(responseCode, responseBody)

        } catch (e: Exception) {
            OvhResult.Error("Erreur réseau : ${e.localizedMessage}")
        }
    }

    // ─── Parse OVH Response ────────────────────────────────────────────────────
    private fun parseOvhResponse(httpCode: Int, body: String): OvhResult {
        return try {
            val json   = JSONObject(body)
            val status = json.optInt("status", -1)

            when {
                httpCode == 200 && (status == 100 || status == 101) -> {
                    val credits = json.optInt("creditLeft", -1)
                    OvhResult.Success(
                        message     = "SMS envoyé avec succès !",
                        creditsLeft = credits
                    )
                }
                else -> {
                    val desc = json.optString("message", "Erreur inconnue (status=$status)")
                    OvhResult.Error("Erreur OVH : $desc")
                }
            }
        } catch (e: Exception) {
            // Fallback : réponse texte brute OVH (ancien format)
            when {
                body.contains("OK", ignoreCase = true) ->
                    OvhResult.Success("SMS envoyé avec succès !", -1)
                else ->
                    OvhResult.Error("Réponse inattendue : $body")
            }
        }
    }

    // ─── Handle Result ─────────────────────────────────────────────────────────
    private fun handleResult(result: OvhResult) {
        when (result) {
            is OvhResult.Success -> {
                showSuccess(result.message)
                if (result.creditsLeft >= 0) {
                    tvCreditsInfo.visibility = View.VISIBLE
                    tvCreditsInfo.text       = "Crédits restants : ${result.creditsLeft}"
                }
                etMessage.text.clear()
            }
            is OvhResult.Error -> showError(result.message)
        }
    }

    // ─── UI Helpers ────────────────────────────────────────────────────────────
    private fun setLoading(loading: Boolean) {
        btnSend.isEnabled    = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        btnSend.text         = if (loading) "Envoi…" else "Envoyer le SMS"
    }

    private fun showSuccess(msg: String) {
        cardStatus.visibility = View.VISIBLE
        tvStatus.text         = "✓  $msg"
        cardStatus.setBackgroundResource(R.drawable.bg_status_success)
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.success))
    }

    private fun showError(msg: String) {
        cardStatus.visibility = View.VISIBLE
        tvStatus.text         = "✗  $msg"
        cardStatus.setBackgroundResource(R.drawable.bg_status_error)
        tvStatus.setTextColor(ContextCompat.getColor(this, R.color.error))
    }

    private fun hideStatus() {
        cardStatus.visibility   = View.GONE
        tvCreditsInfo.visibility = View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}

// ─── Result sealed class ───────────────────────────────────────────────────────
sealed class OvhResult {
    data class Success(val message: String, val creditsLeft: Int) : OvhResult()
    data class Error(val message: String)   : OvhResult()
}
