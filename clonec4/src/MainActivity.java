package dev.air2023.clonec4;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.widget.*;
import android.view.View;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class MainActivity extends Activity {
 private TextView output;private Button preview,save,copy;private boolean busy;
 private String token,staged;
 private static final String CHECK="set -eu; if [ \"$(id -u)\" != 0 ] || [ \"$(getprop ro.product.model)\" != DBY2-W00 ] || [ \"$(getprop ro.build.display.id)\" != 'DBY2-W00 4.2.0.252(C00E100R3P5)' ]; then echo DEVICE_OR_ROOT_MISMATCH; exit 20; fi; ";
 private static final String STATE="f=/data/system/air2023_clonec4.list; check() { if [ -L \"$f\" ]; then echo UNSAFE_SYMLINK; exit 21; fi; if [ -e \"$f\" ]; then [ -f \"$f\" ] && [ \"$(stat -c '%u:%g:%a' \"$f\")\" = 0:1000:640 ] && [ \"$(wc -c < \"$f\")\" -le 131072 ] || { echo UNSAFE_EXISTING; exit 22; }; state=$(stat -c '%d:%i:%s:%Y' \"$f\"); else state=ABSENT; fi; }; check; ";
 private static final String READ=CHECK+STATE+"echo STATE=$state; if [ \"$state\" != ABSENT ]; then cat \"$f\"; fi; ";
 @Override public void onCreate(Bundle b){
   super.onCreate(b);LinearLayout p=new LinearLayout(this);p.setOrientation(1);p.setPadding(32,24,32,24);
   TextView t=new TextView(this);t.setTextSize(24);t.setText("Air2023 原生应用分身 C4");p.addView(t);
   TextView info=new TextView(this);info.setText("合并 C2/C3。首次先预览并保存名单，再启用 C4（设置+系统框架），停用 C2/C3 后手动软重启。以后新安装应用：打开本工具，预览并同步，再软重启。不会在运行中的系统框架修改名单，不自动创建分身或启用组件。保留上次名单，避免重启清理已支持分身；不自动删旧条目。名单保存在 /data/system，不是系统分区。请先备份非原厂分身，系统框架注入或恢复原厂可能导致异常及数据丢失。");p.addView(info);
   preview=button(p,"扫描已安装应用 / 新安装应用",new View.OnClickListener(){public void onClick(View v){run(false);}});
   save=button(p,"确认同步全部候选应用",new View.OnClickListener(){public void onClick(View v){if(staged==null)return;new AlertDialog.Builder(MainActivity.this).setTitle("保存分身名单").setMessage("仅保存预览名单，不立即修改系统服务。下次系统框架初始化后生效。请备份分身数据。").setNegativeButton("取消",null).setPositiveButton("保存",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){run(true);}}).show();}});
   copy=button(p,"复制报告",new View.OnClickListener(){public void onClick(View v){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("Clone C4",output.getText()));}});
   output=new TextView(this);output.setTextIsSelectable(true);output.setText("请先扫描。");p.addView(output);
   ScrollView sc=new ScrollView(this);sc.addView(p);setContentView(sc);refresh();
 }
 private Button button(LinearLayout p,String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setOnClickListener(l);p.addView(b);return b;}
 private void refresh(){preview.setEnabled(!busy);save.setEnabled(!busy&&staged!=null);copy.setEnabled(!busy);}
 private void run(final boolean write){
   if(busy||(write&&staged==null))return;
   final String old=token, data=staged;staged=null;token=null;busy=true;refresh();output.setText("执行中");
   new Thread(new Runnable(){public void run(){
     String msg;String next=null,nextToken=null;
     try{
       if(!write){
         String r=exec(READ,null);int sep=r.indexOf('\n');
         if(sep<0||!r.startsWith("STATE="))throw new IOException(r);
         String state=r.substring(6,sep);
         if(!state.equals("ABSENT")&&!state.matches("[0-9]+:[0-9]+:[0-9]+:[0-9]+"))throw new IOException("invalid state");
         Set<String> before=state.equals("ABSENT")?new TreeSet<String>():Policy.parse(r.substring(sep+1));
         Set<String> all=new TreeSet<String>(before),fresh=Policy.candidates(MainActivity.this);
         all.addAll(fresh);all.add(Policy.TEST);
         Set<String> added=new TreeSet<String>(all);added.removeAll(before);
         next=Policy.serialize(all);nextToken=state;
         msg="已保存："+before.size()+"\n当前合格应用："+fresh.size()+"\n新增："+added.size()+"\n待同步总数："+all.size()+"（另保留7项原厂）\n新增包名："+added+"\n完整名单："+all+"\n确认同步后需要软重启。";
       }else{
         if(old==null||(!old.equals("ABSENT")&&!old.matches("[0-9]+:[0-9]+:[0-9]+:[0-9]+")))throw new IOException("stale preview");
         Policy.parse(data);
         String script=CHECK+STATE+"[ \"$state\" = '"+old+"' ] || { echo STALE_PREVIEW; exit 23; }; umask 077; tmp=$(mktemp /data/system/.air2023_clonec4.XXXXXX); trap 'rm -f \"$tmp\"' EXIT; cat > \"$tmp\"; [ \"$(wc -c < \"$tmp\")\" -le 131072 ]; [ \"$(head -n 1 \"$tmp\")\" = AIR2023_CLONE_C4_V1 ]; [ \"$(tail -n 1 \"$tmp\")\" = END ]; chown 0:1000 \"$tmp\"; chmod 0640 \"$tmp\"; [ \"$(ls -Zd \"$tmp\" | awk '{print $1}')\" = u:object_r:system_data_file:s0 ] || { echo CONTEXT_MISMATCH; exit 24; }; check; [ \"$state\" = '"+old+"' ] || exit 23; mv -f \"$tmp\" \"$f\"; echo SAVED; cat \"$f\";";
         String r=exec(script,data.getBytes(StandardCharsets.UTF_8));
         if(!r.equals("SAVED\n"+data))throw new IOException("readback mismatch; rescan\n"+r);
         msg="C4_SNAPSHOT_SAVED\n名单已保存并读回校验："+Policy.parse(data).size()+"\n尚未在 system_server 生效。首次安装请停用 C2/C3、启用 C4 两个作用域，再备份后软重启。后续更新只需软重启。\n日志搜索 C4_ALLOWLIST_OK，核对新增数量。\n"+data;
       }
     }catch(Throwable e){msg="未完成："+e;}
     final String result=msg,n=next,k=nextToken;runOnUiThread(new Runnable(){public void run(){staged=n;token=k;busy=false;output.setText(result);refresh();}});
   }}).start();
 }
 private static String exec(String cmd,final byte[] data)throws Exception{
   final Process p=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();
   final ByteArrayOutputStream out=new ByteArrayOutputStream();final Exception[] errors=new Exception[2];
   Thread r=new Thread(new Runnable(){public void run(){try(InputStream in=p.getInputStream()){byte[] b=new byte[4096];int n;while((n=in.read(b))!=-1){if(out.size()+n>256*1024)throw new IOException("output limit");out.write(b,0,n);}}catch(Exception e){errors[0]=e;}}});
   Thread w=new Thread(new Runnable(){public void run(){try(OutputStream x=p.getOutputStream()){if(data!=null)x.write(data);}catch(Exception e){errors[1]=e;}}});
   r.start();w.start();boolean done=p.waitFor(30,TimeUnit.SECONDS);if(!done)p.destroyForcibly();r.join(1500);w.join(1500);
   if(!done||r.isAlive()||w.isAlive())throw new IOException("timeout");
   if(errors[0]!=null)throw errors[0];if(errors[1]!=null)throw errors[1];
   String s=new String(out.toByteArray(),StandardCharsets.UTF_8);if(p.exitValue()!=0)throw new IOException("Exit="+p.exitValue()+"\n"+s);return s;
 }
}
