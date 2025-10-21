package com.synapse.social.studioasinc

import android.app.Application
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.AuthDevelopmentUtils

class SynapseApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize authentication service
        SupabaseAuthenticationService.initialize(this)
        
        // Log authentication configuration in development builds
        if (AuthDevelopmentUtils.isDevelopmentBuild()) {
            AuthDevelopmentUtils.logAuthConfig(this)
        }
    }
}