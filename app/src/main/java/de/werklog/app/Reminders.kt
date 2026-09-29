package de.werklog.app

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId

object Reminders {
    private fun pending(c: Context) = PendingIntent.getBroadcast(c, 700, Intent(c, ReminderReceiver::class.java).setAction("de.werklog.REMINDER"), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun update(c: Context, data: Data) {
        val now = System.currentTimeMillis()
        val times = data.work.appointments.filter { it.status == "Geplant" && it.remind >= 0 }.flatMap { a ->
            occurrences(a, LocalDate.now(), LocalDate.now().plusYears(1)).map { appointmentTime(it.start)!!.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - it.remind * 60000L }
        }.filter { it > now }.distinct().sorted().take(2000)
        // Only trigger timestamps outside the vault; no titles, companies, assets or passwords.
        c.getSharedPreferences("reminders", 0).edit().putString("times", times.joinToString(",")).apply()
        schedule(c)
    }
    fun schedule(c: Context) {
        val alarm = c.getSystemService(AlarmManager::class.java); alarm.cancel(pending(c))
        val next = c.getSharedPreferences("reminders", 0).getString("times", "")!!.split(',').mapNotNull { it.toLongOrNull() }.filter { it > System.currentTimeMillis() }.minOrNull() ?: return
        alarm.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next, pending(c))
    }
    fun notify(c: Context) {
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val manager = c.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("appointments", "Lokale Termine", NotificationManager.IMPORTANCE_DEFAULT))
        val open = PendingIntent.getActivity(c, 701, Intent(c, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        manager.notify(700, Notification.Builder(c, "appointments").setSmallIcon(R.drawable.ic_werklog).setContentTitle("WerkLog · Termin prüfen")
            .setContentText("Ein hinterlegter Termin steht an. Zum Anzeigen Tresor entsperren.").setVisibility(Notification.VISIBILITY_SECRET).setAutoCancel(true).setContentIntent(open).build())
    }
}
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == "de.werklog.REMINDER") Reminders.notify(context)
        Reminders.schedule(context)
    }
}
