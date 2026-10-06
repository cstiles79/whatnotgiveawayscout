package com.example.giveawayscout;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.util.*;

public class MainActivity extends Activity {
    static final String PREFS = "scout";
    static final String CHANNEL = "giveaways";
    static final int NOTIFY_DISCOVERY = 1000;
    static final int NOTIFY_DRAWING = 2000;
    static final int REQ_NOTIFY = 7;

    LinearLayout root, activeList;
    EditText feedUrl, seller, prize, showUrl, duration;
    TextView status;
    SharedPreferences prefs;
    AlarmManager alarms;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        alarms = (AlarmManager)getSystemService(ALARM_SERVICE);
        createChannel();
        if (Build.VERSION.SDK_INT >= 33) requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_NOTIFY);
        buildUi();
    }

    void buildUi() {
        ScrollView sv = new ScrollView(this);
        root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(28,28,28,28);
        sv.addView(root);

        TextView title = new TextView(this); title.setText("Whatnot Giveaway Scout"); title.setTextSize(26); root.addView(title);
        TextView sub = new TextView(this); sub.setText("Scan → notify → open show → you enter → 15-second warning"); sub.setPadding(0,10,0,18); root.addView(sub);

        status = new TextView(this); status.setText("Scanner is ready. No giveaway-entry automation is performed."); root.addView(status);

        TextView scanHead = header("SCANNER"); root.addView(scanHead);
        feedUrl = field("Discovery feed URL (optional)"); feedUrl.setText(prefs.getString("feed", "")); root.addView(feedUrl);
        Button saveFeed = button("SAVE SCANNER SOURCE"); root.addView(saveFeed);
        saveFeed.setOnClickListener(v -> { prefs.edit().putString("feed", feedUrl.getText().toString().trim()).apply(); status.setText("Scanner source saved. Use SCAN NOW to test it."); });
        Button scan = button("SCAN NOW"); root.addView(scan); scan.setOnClickListener(v -> scanFeed());
        Button simulate = button("SIMULATE GIVEAWAY DETECTED"); root.addView(simulate); simulate.setOnClickListener(v -> discover("Demo Seller", "Demo Prize", "https://www.whatnot.com/", 300));

        TextView entryHead = header("ENTERED GIVEAWAY"); root.addView(entryHead);
        seller = field("Seller"); root.addView(seller);
        prize = field("Prize"); root.addView(prize);
        showUrl = field("Whatnot show URL"); root.addView(showUrl);
        duration = field("Seconds until drawing (default 300)"); duration.setInputType(2); duration.setText("300"); root.addView(duration);
        Button entered = button("I ENTERED — START 15-SECOND ALERT"); root.addView(entered); entered.setOnClickListener(v -> addEntered());
        Button open = button("OPEN WHATNOT SHOW"); root.addView(open); open.setOnClickListener(v -> openShow(showUrl.getText().toString()));

        TextView listHead = header("TRACKING"); root.addView(listHead);
        activeList = new LinearLayout(this); activeList.setOrientation(LinearLayout.VERTICAL); root.addView(activeList);
        Button clear = button("CLEAR TRACKING"); root.addView(clear); clear.setOnClickListener(v -> { prefs.edit().remove("entries").apply(); refreshList(); });

        setContentView(sv);
        refreshList();
    }

    TextView header(String s){ TextView t=new TextView(this); t.setText(s); t.setTextSize(18); t.setPadding(0,24,0,8); return t; }
    EditText field(String hint){ EditText e=new EditText(this); e.setHint(hint); e.setSingleLine(true); return e; }
    Button button(String s){ Button b=new Button(this); b.setText(s); return b; }

    void discover(String s, String p, String u, int secondsUntilEnd) {
        seller.setText(s); prize.setText(p); showUrl.setText(u); duration.setText(String.valueOf(secondsUntilEnd));
        notifyUser("Giveaway found", s + " — " + p, NOTIFY_DISCOVERY);
        status.setText("Giveaway detected. Open the show and enter it manually.");
    }

    void addEntered() {
        String s=seller.getText().toString().trim(); String p=prize.getText().toString().trim(); String u=showUrl.getText().toString().trim();
        if(s.isEmpty()) s="Unknown seller"; if(p.isEmpty()) p="Giveaway";
        int sec=300; try{sec=Integer.parseInt(duration.getText().toString());}catch(Exception ignored){}
        long end=System.currentTimeMillis()+Math.max(15,sec)*1000L;
        String id=UUID.randomUUID().toString();
        JSONArray a=getEntries(); JSONObject o=new JSONObject();
        try { o.put("id",id); o.put("seller",s); o.put("prize",p); o.put("url",u); o.put("end",end); a.put(o); prefs.edit().putString("entries",a.toString()).apply(); }
        catch(Exception ignored){}
        scheduleWarning(id, end); refreshList(); status.setText("Tracking " + s + ". 15-second warning scheduled.");
        if(!u.isEmpty()) openShow(u);
    }

    void scheduleWarning(String id, long end) {
        long trigger=Math.max(System.currentTimeMillis()+1000, end-15000);
        Intent i=new Intent(this, AlertReceiver.class); i.setAction("DRAWING_15"); i.putExtra("id",id);
        PendingIntent pi=PendingIntent.getBroadcast(this, id.hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        if(Build.VERSION.SDK_INT>=31 && !alarms.canScheduleExactAlarms()) {
            status.setText("Android exact-alarm access is off. Enable it in Settings for precise 15-second alerts.");
            startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM));
            return;
        }
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi);
    }

    JSONArray getEntries(){ try{return new JSONArray(prefs.getString("entries","[]"));}catch(Exception e){return new JSONArray();} }

    void refreshList(){
        if(activeList==null)return; activeList.removeAllViews(); JSONArray a=getEntries();
        for(int i=0;i<a.length();i++) try{
            JSONObject o=a.getJSONObject(i); long left=o.getLong("end")-System.currentTimeMillis();
            TextView t=new TextView(this); t.setText(o.getString("seller")+" — "+o.getString("prize")+"\n"+(left>0?(left/1000)+" sec remaining":"drawing time passed")); t.setPadding(0,8,0,8); activeList.addView(t);
        }catch(Exception ignored){}
    }

    void scanFeed(){
        String url=feedUrl.getText().toString().trim();
        if(url.isEmpty()){ status.setText("No discovery feed configured. Use SIMULATE GIVEAWAY DETECTED to test the full alert workflow."); return; }
        status.setText("Scanner source configured. This prototype expects a JSON feed; no Whatnot login or giveaway-entry automation is used.");
        // Network polling is intentionally isolated in ScannerService in the production build.
    }

    void openShow(String u){ if(u==null||u.trim().isEmpty())return; try{startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(u.trim())));}catch(Exception ignored){} }

    void createChannel(){ if(Build.VERSION.SDK_INT>=26){ NotificationChannel c=new NotificationChannel(CHANNEL,"Giveaway Alerts",NotificationManager.IMPORTANCE_HIGH); c.setDescription("Giveaway detection and 15-second drawing alerts"); c.enableVibration(true); ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(c); } }

    void notifyUser(String title,String text,int id){
        NotificationManager nm=(NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        Intent i=new Intent(this,MainActivity.class); PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder n=new Notification.Builder(this,CHANNEL).setSmallIcon(android.R.drawable.ic_dialog_info).setContentTitle(title).setContentText(text).setAutoCancel(true).setPriority(Notification.PRIORITY_MAX).setContentIntent(pi).setVibrate(new long[]{0,400,200,700});
        nm.notify(id,n.build());
    }
}
