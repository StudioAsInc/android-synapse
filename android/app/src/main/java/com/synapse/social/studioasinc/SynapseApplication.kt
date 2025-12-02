package com.synapse.social.studioasinc

import android.app.Application
import com.synapse.social.studioasinc.backend.SupabaseAuthenticationService
import com.synapse.social.studioasinc.backend.AuthDevelopmentUtils
import com.synapse.social.studioasinc.util.MediaCacheCleanupManager
import com.synapse.social.studioasinc.chat.service.DatabaseMaintenanceManager

import android.content.Context

class SynapseApplication : Application() {
    
    private lateinit var mediaCacheCleanupManager: MediaCacheCleanupManager
    private lateinit var databaseMaintenanceManager: DatabaseMaintenanceManager

    init {
        instance = this
    }

    companion object {
        private var instance: SynapseApplication? = null

        fun applicationContext(): Context {
            return instance!!.applicationContext
        }
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize authentication service
        SupabaseAuthenticationService.initialize(this)
        
        // Initialize background maintenance services
        initializeMaintenanceServices()
        
        // Log authentication configuration in development builds
        if (AuthDevelopmentUtils.isDevelopmentBuild()) {
            AuthDevelopmentUtils.logAuthConfig(this)
        }
    }
    
    private fun initializeMaintenanceServices() {
        // Initialize media cache cleanup
        mediaCacheCleanupManager = MediaCacheCleanupManager(this)
        mediaCacheCleanupManager.initialize()
        
        // Initialize database maintenance (if not already initialized)
        databaseMaintenanceManager = DatabaseMaintenanceManager(this)
        databaseMaintenanceManager.initialize()
    }
    
    override fun onTerminate() {
        super.onTerminate()
        
        // Clean up maintenance services
        if (::mediaCacheCleanupManager.isInitialized) {
            mediaCacheCleanupManager.shutdown()
        }
        
        if (::databaseMaintenanceManager.isInitialized) {
            databaseMaintenanceManager.shutdown()
        }
    }
}