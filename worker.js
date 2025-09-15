// Cloudflare Worker for handling push notifications via OneSignal
// This worker receives notification requests and forwards them to OneSignal's REST API

export default {
  async fetch(request, env, ctx) {
    // Handle CORS preflight requests
    if (request.method === 'OPTIONS') {
      return new Response(null, {
        status: 200,
        headers: {
          'Access-Control-Allow-Origin': '*',
          'Access-Control-Allow-Methods': 'POST, GET, OPTIONS',
          'Access-Control-Allow-Headers': 'Content-Type, Authorization',
          'Access-Control-Max-Age': '86400',
        },
      });
    }

    // Only allow POST requests
    if (request.method !== 'POST') {
      return new Response('Method not allowed', { status: 405 });
    }

    try {
      // Parse the request body
      const requestData = await request.json();
      const { recipientUserId, notificationMessage } = requestData;

      // Validate required fields
      if (!recipientUserId || !notificationMessage) {
        return new Response(
          JSON.stringify({ 
            error: 'Missing required fields: recipientUserId and notificationMessage' 
          }), 
          { 
            status: 400,
            headers: {
              'Content-Type': 'application/json',
              'Access-Control-Allow-Origin': '*',
            }
          }
        );
      }

      // OneSignal configuration
      const ONESIGNAL_APP_ID = '044e1911-6911-4871-95f9-d60003002fe2';
      const ONESIGNAL_REST_API_KEY = env.ONESIGNAL_REST_API_KEY;

      if (!ONESIGNAL_REST_API_KEY) {
        console.error('OneSignal REST API key not configured');
        return new Response(
          JSON.stringify({ error: 'Server configuration error' }), 
          { 
            status: 500,
            headers: {
              'Content-Type': 'application/json',
              'Access-Control-Allow-Origin': '*',
            }
          }
        );
      }

      // Prepare the notification payload for OneSignal
      const notificationPayload = {
        app_id: ONESIGNAL_APP_ID,
        include_player_ids: [recipientUserId],
        contents: {
          en: notificationMessage
        },
        headings: {
          en: 'New Message'
        },
        data: {
          message: notificationMessage,
          timestamp: new Date().toISOString()
        },
        // Simplified notification settings - removed problematic fields
        priority: 10, // High priority for messages
        android_accent_color: '#FF6B6B', // Custom accent color
        // Removed android_channel_id and other problematic fields
      };

      // Send notification to OneSignal
      const oneSignalResponse = await fetch('https://onesignal.com/api/v1/notifications', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Basic ${ONESIGNAL_REST_API_KEY}`,
        },
        body: JSON.stringify(notificationPayload),
      });

      if (!oneSignalResponse.ok) {
        const errorText = await oneSignalResponse.text();
        console.error('OneSignal API error:', errorText);
        return new Response(
          JSON.stringify({ 
            error: 'Failed to send notification via OneSignal',
            details: errorText
          }), 
          { 
            status: oneSignalResponse.status,
            headers: {
              'Content-Type': 'application/json',
              'Access-Control-Allow-Origin': '*',
            }
          }
        );
      }

      const oneSignalResult = await oneSignalResponse.json();
      
      // Log successful notification
      console.log('Notification sent successfully:', {
        recipientId: recipientUserId,
        message: notificationMessage,
        oneSignalId: oneSignalResult.id
      });

      // Return success response
      return new Response(
        JSON.stringify({ 
          success: true, 
          message: 'Notification sent successfully',
          oneSignalId: oneSignalResult.id
        }), 
        { 
          status: 200,
          headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*',
          }
        }
      );

    } catch (error) {
      console.error('Worker error:', error);
      return new Response(
        JSON.stringify({ 
          error: 'Internal server error',
          message: error.message 
        }), 
        { 
          status: 500,
          headers: {
            'Content-Type': 'application/json',
            'Access-Control-Allow-Origin': '*',
          }
        }
      );
    }
  },
};
