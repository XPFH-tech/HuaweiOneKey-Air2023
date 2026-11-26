package dev.air2023.clonec4r1;
import android.content.*;
import android.content.pm.*;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructStat;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

final class Policy {
 static final String PATH="/data/system/air2023_clonec4.list";
 static final String HEADER="AIR2023_CLONE_C4_V1";
 static final String TEST="com.coolapk.market";
 static final Set<String> STOCK=Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("com.tencent.mm","com.tencent.mobileqq","com.facebook.katana","com.whatsapp","jp.naver.line.android","com.snapchat.android","com.facebook.orca")));
 static final int MAX=512, BYTES=128*1024;
 static boolean valid(String p){
   return p!=null&&p.length()<=200&&p.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+");
 }
 static final Set<String> EXTRA_BLOCK=Collections.unmodifiableSet(new HashSet<String>(Arrays.asList("app.revanced.android.gms","com.didjdk.adbhelper","com.huawei.onekey.optimizer","dev.dby2.rootkit","moe.shizuku.privileged.api","com.kldp.android.orientationmanager","com.oray.sunlogin")));
 static boolean excluded(String p){return EXTRA_BLOCK.contains(p)||oldExcluded(p);}
 static boolean oldExcluded(String p){
   String x=p.toLowerCase(Locale.ROOT);
   return x.startsWith("dev.air2023.")||x.startsWith("com.topjohnwu.")||x.startsWith("org.lsposed.")||
      x.startsWith("io.github.vvb2060.")||x.startsWith("me.weishu.kernelsu")||x.startsWith("me.bmax.apatch")||
      x.startsWith("com.google.android.gms")||x.equals("com.android.vending")||x.equals("com.google.android.gsf")||
      x.contains("zygisk")||x.contains("magisk")||x.contains("lsposed")||x.contains("kernelsu")||x.contains("ghostlock")||x.contains("dfroot")||x.contains("vector");
 }
 static Set<String> candidates(Context ctx){
   PackageManager pm=ctx.getPackageManager();
   Set<String> blocked=new HashSet<String>();
   for(ResolveInfo r:pm.queryBroadcastReceivers(new Intent("android.app.action.DEVICE_ADMIN_ENABLED"),PackageManager.MATCH_DISABLED_COMPONENTS))
      if(r.activityInfo!=null)blocked.add(r.activityInfo.packageName);
   for(String action:new String[]{"android.net.VpnService","android.accessibilityservice.AccessibilityService","android.service.notification.NotificationListenerService","android.service.quicksettings.action.QS_TILE"}){
      // QS tiles are not in themselves privileged: only block other sensitive service types.
      if(action.endsWith("QS_TILE"))continue;
      for(ResolveInfo r:pm.queryIntentServices(new Intent(action),PackageManager.MATCH_DISABLED_COMPONENTS))
         if(r.serviceInfo!=null)blocked.add(r.serviceInfo.packageName);
   }
   Intent i=new Intent(Intent.ACTION_MAIN);i.addCategory(Intent.CATEGORY_LAUNCHER);
   Set<String> result=new TreeSet<String>();
   for(ResolveInfo r:pm.queryIntentActivities(i,0)){
      if(r.activityInfo==null||r.activityInfo.applicationInfo==null)continue;
      ApplicationInfo a=r.activityInfo.applicationInfo;String p=a.packageName;
      if(!valid(p)||excluded(p)||blocked.contains(p)||!a.enabled||!r.activityInfo.enabled||!r.activityInfo.exported)continue;
      if((a.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP|ApplicationInfo.FLAG_SUSPENDED))!=0)continue;
      if(!STOCK.contains(p))result.add(p);
   }
   return result;
 }
 static Set<String> parse(String data)throws IOException{return parseInternal(data,false);}
 static Set<String> parseLegacy(String data)throws IOException{return parseInternal(data,true);}
 private static Set<String> parseInternal(String data,boolean migration)throws IOException{
   if(data.length()>BYTES)throw new IOException("snapshot size");
   String[] lines=data.split("\n",-1);
   if(lines.length<3||!HEADER.equals(lines[0])||!"END".equals(lines[lines.length-2])||!lines[lines.length-1].isEmpty())throw new IOException("snapshot format");
   Set<String> out=new TreeSet<String>();
   for(int i=1;i<lines.length-2;i++){
      String p=lines[i];if(!valid(p)||(migration?oldExcluded(p):excluded(p))||STOCK.contains(p)||!out.add(p)||out.size()>MAX)throw new IOException("snapshot entry");
   }
   return out;
 }
 static String serialize(Set<String> names)throws IOException{
   StringBuilder b=new StringBuilder(HEADER).append('\n');
   for(String n:new TreeSet<String>(names))b.append(n).append('\n');
   b.append("END\n");String s=b.toString();parse(s);return s;
 }
 static Set<String> readSnapshot()throws Exception{
   StructStat st=Os.lstat(PATH);
   if(!OsConstants.S_ISREG(st.st_mode)||st.st_uid!=0||st.st_gid!=1000||(st.st_mode&0777)!=0640||st.st_size>BYTES)throw new IOException("snapshot owner/mode/type");
   try(InputStream in=new FileInputStream(PATH)){
      ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[4096];int n;
      while((n=in.read(x))!=-1){if(b.size()+n>BYTES)throw new IOException("snapshot too large");b.write(x,0,n);}
      return parse(new String(b.toByteArray(),StandardCharsets.UTF_8));
   }
 }
}
