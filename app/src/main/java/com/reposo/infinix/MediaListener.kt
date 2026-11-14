package com.reposo.infinix

import android.service.notification.NotificationListenerService

/**
 * No hace nada por si mismo: Android exige este servicio para que la app
 * pueda ver y controlar la musica de otras apps.
 */
class MediaListener : NotificationListenerService()
