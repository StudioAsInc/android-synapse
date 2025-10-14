
package com.synapse.social.studioasinc

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DisconnectReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        if (intent.action == "disconnect") {
            val userId = intent.getStringExtra("user_id")
            if (userId != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Supabase.client.postgrest["profiles"].update(
                            {
                                set("status", System.currentTimeMillis().toString())
                            }
                        ) {
                            filter("id", "eq", userId)
                        }
                    } catch (e: Exception) {
                        // Handle error
                    } finally {
                        pendingResult.finish()
                    }
                }
            }
        }
    }
}
