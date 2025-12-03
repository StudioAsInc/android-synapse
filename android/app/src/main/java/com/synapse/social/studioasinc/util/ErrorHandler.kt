package com.synapse.social.studioasinc.util

import android.content.Context
import com.synapse.social.studioasinc.R
import io.github.jan.supabase.exceptions.BadRequestRestException
import io.github.jan.supabase.exceptions.NotFoundRestException
import io.github.jan.supabase.exceptions.UnauthorizedRestException
import io.github.jan.supabase.exceptions.UnknownRestException
import io.github.jan.supabase.exceptions.RestException
import java.net.UnknownHostException

object ErrorHandler {

    fun getErrorMessage(exception: Exception, context: Context): String {
        return when (exception) {
            is UnknownHostException -> context.getString(R.string.error_no_internet)
            is UnauthorizedRestException -> context.getString(R.string.error_unauthorized)
            is BadRequestRestException -> context.getString(R.string.error_bad_request)
            is NotFoundRestException -> context.getString(R.string.error_not_found)
            is RestException -> {
                when (exception.statusCode) {
                    403 -> context.getString(R.string.error_forbidden)
                    409 -> context.getString(R.string.error_conflict)
                    else -> exception.message ?: context.getString(R.string.error_unknown_rest)
                }
            }
            is UnknownRestException -> context.getString(R.string.error_unknown_rest)
            else -> exception.message ?: context.getString(R.string.error_generic)
        }
    }
}
