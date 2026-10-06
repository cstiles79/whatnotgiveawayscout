package com.example.giveawayscout;

import android.app.*;
import android.content.*;
import android.os.Build;
import org.json.*;

public class AlertReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        if(!"DRAWING_15".equals(intent.getAction())) return;
        String id=intent.getStringExtra("id");
        String seller="Giveaway"; String prize="Drawing in 15 seconds";
        try {
            android.content.SharedPreferences p=context.getSharedPreferences(MainActivity.PREFS, Context.MODE_PRIVATE);
            JSONArray a=new JSONArray(p.getString("entries","[]"));
            for(int i=0;i<a.length();i++){ JSONObject o=a.getJSONObject(i); if(id!=null&&id.equals(o.optString("id"))){seller=o.optString("seller","Giveaway"); prize=o.optString("prize","Drawing in 15 seconds"); break;} }
        } catch(Exception ignored){}
        NotificationManager nm=(NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent open=new Intent(context,MainActivity.class);
        PendingIntent pi=PendingIntent.getActivity(context,0,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder n=new Notification.Builder(context,MainActivity.CHANNEL)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setContentTitle("🚨 GIVEAWAY DRAWING IN 15 SECONDS")
                .setContentText(seller+" — "+prize+". Return to Whatnot now.")
                .setAutoCancel(false).setOngoing(true).setPriority(Notification.PRIORITY_MAX)
                .setVibrate(new long[]{0,500,150,500,150,900}).setContentIntent(pi);
        nm.notify(MainActivity.NOTIFY_DRAWING+(id==null?0:id.hashCode()),n.build());
    }
}
