package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class FloatingPixelService : Service() {

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var loupeOverlayView: View? = null

    companion object {
        const val CHANNEL_ID = "pixel_puro_floating_service"
        const val NOTIFICATION_ID = 2026
        const val ACTION_START = "ACTION_START_FLOATING"
        const val ACTION_STOP = "ACTION_STOP_FLOATING"

        fun isRunning(context: Context): Boolean {
            // Can be queried via shared state or service tracker
            return activeInstance != null
        }

        var activeInstance: FloatingPixelService? = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        setupFloatingMiniBar()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "PixelPuro Inspector Flotante",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Modo flotante 98% oculto con cuentagotas quirúrgico y capturador"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("PixelPuro Flotante Activo")
            .setContentText("Cuentagotas quirúrgico y captura en 2% de pantalla")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun setupFloatingMiniBar() {
        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 300
        }

        // 2% compact pill container
        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(Color.parseColor("#E60D1117"))
            setPadding(16, 12, 16, 12)
            elevation = 16f
        }

        // Button 1: Eyedropper / Lupa Quirúrgica
        val eyedropperBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_search)
            setColorFilter(Color.parseColor("#00E5FF"))
            setPadding(12, 12, 12, 12)
            contentDescription = "Cuentagotas Flotante"
            setOnClickListener {
                toggleFloatingLoupe()
            }
        }

        // Button 2: Quick Screenshot / Capture
        val captureBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_camera)
            setColorFilter(Color.parseColor("#00E676"))
            setPadding(12, 12, 12, 12)
            contentDescription = "Captura de Pantalla"
            setOnClickListener {
                Toast.makeText(this@FloatingPixelService, "Captura solicitada: Abriendo PixelPuro para análisis...", Toast.LENGTH_SHORT).show()
                val intent = Intent(this@FloatingPixelService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    putExtra("TRIGGER_CAPTURE", true)
                }
                startActivity(intent)
            }
        }

        // Button 3: Expand to full app
        val expandBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_view)
            setColorFilter(Color.parseColor("#FFD600"))
            setPadding(12, 12, 12, 12)
            contentDescription = "Abrir Galería PixelPuro"
            setOnClickListener {
                val intent = Intent(this@FloatingPixelService, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                }
                startActivity(intent)
            }
        }

        // Button 4: Close
        val closeBtn = ImageView(this).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#888888"))
            setPadding(12, 12, 12, 12)
            contentDescription = "Cerrar"
            setOnClickListener {
                stopSelf()
            }
        }

        rootLayout.addView(eyedropperBtn)
        rootLayout.addView(captureBtn)
        rootLayout.addView(expandBtn)
        rootLayout.addView(closeBtn)

        // Make root layout draggable
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        rootLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(rootLayout, params)
                    true
                }
                else -> false
            }
        }

        floatingView = rootLayout
        windowManager?.addView(floatingView, params)
    }

    private fun toggleFloatingLoupe() {
        if (loupeOverlayView != null) {
            removeFloatingLoupe()
            return
        }

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
            x = 0
            y = 0
        }

        // Loupe card with handle offset
        val loupeContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#F0121622"))
            setPadding(24, 24, 24, 24)
            gravity = Gravity.CENTER_HORIZONTAL
        }

        val title = TextView(this).apply {
            text = "🔬 Lupa Quirúrgica Offset"
            setTextColor(Color.parseColor("#00E5FF"))
            textSize = 14f
            gravity = Gravity.CENTER
        }

        // Loupe center sight display
        val sightBox = FrameLayout(this).apply {
            val sizePx = 180
            layoutParams = LinearLayout.LayoutParams(sizePx, sizePx).apply {
                topMargin = 16
                bottomMargin = 16
            }
            setBackgroundColor(Color.parseColor("#090D16"))
        }

        val crosshair = TextView(this).apply {
            text = "✛ [1x1 px]"
            setTextColor(Color.parseColor("#00E676"))
            textSize = 16f
            gravity = Gravity.CENTER
        }
        sightBox.addView(crosshair)

        val infoHex = TextView(this).apply {
            text = "HEX8: #FF00E5FF\nRGBA: (0, 229, 255, 255)\nPosición: Quirúrgica"
            setTextColor(Color.WHITE)
            textSize = 12f
            gravity = Gravity.CENTER
        }

        val handleNotice = TextView(this).apply {
            text = "↕ Arrastra desde el mango inferior\n(Tu dedo no tapa el píxel)"
            setTextColor(Color.parseColor("#8B949E"))
            textSize = 11f
            gravity = Gravity.CENTER
            setPadding(8, 12, 8, 8)
        }

        val handleBar = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(200, 48).apply {
                topMargin = 12
            }
            setBackgroundColor(Color.parseColor("#00E5FF"))
        }

        loupeContainer.addView(title)
        loupeContainer.addView(sightBox)
        loupeContainer.addView(infoHex)
        loupeContainer.addView(handleNotice)
        loupeContainer.addView(handleBar)

        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f

        handleBar.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(loupeContainer, params)
                    true
                }
                else -> false
            }
        }

        loupeOverlayView = loupeContainer
        windowManager?.addView(loupeOverlayView, params)
        Toast.makeText(this, "Lupa Quirúrgica Activa: Arrastra el mango", Toast.LENGTH_SHORT).show()
    }

    private fun removeFloatingLoupe() {
        loupeOverlayView?.let {
            windowManager?.removeView(it)
            loupeOverlayView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        activeInstance = null
        removeFloatingLoupe()
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
        }
    }
}
