package dev.air2023.compat06;
import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.content.*;
import android.widget.*;
import android.view.View;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class MainActivity extends Activity {
 private TextView result; private EditText packages; private RadioGroup type;
 private Button status, preview, apply, restore, copy; private boolean busy;
 private Plan plan; private String ops;
 private static final String CHECK="if [ \"$(id -u)\" != 0 ] || [ \"$(getprop ro.product.model)\" != DBY2-W00 ] || [ \"$(getprop ro.build.display.id)\" != 'DBY2-W00 4.2.0.252(C00E100R3P5)' ]; then echo DEVICE_OR_ROOT_MISMATCH; exit 20; fi; ";
 private static final class Plan {
   String feature,base,generated,ns,description; byte[] xml;
 }
 private static final class Run {
   byte[] data; int code; String text(){return new String(data,StandardCharsets.UTF_8);}
 }
 @Override public void onCreate(Bundle saved){
   super.onCreate(saved);
   try(InputStream in=getAssets().open("ops.sh")){ops=new String(read(in,64000),StandardCharsets.UTF_8);}catch(Exception e){ops=null;}
   LinearLayout p=new LinearLayout(this);p.setOrientation(1);int pad=(int)(16*getResources().getDisplayMetrics().density);p.setPadding(pad,pad,pad,pad);
   TextView title=new TextView(this);title.setText("Air2023 平行视界与分身 C1");title.setTextSize(24);p.addView(title);
   TextView info=new TextView(this);info.setText("保留 v0.4 和 D3。不修改系统分区，不自动重启、不创建分身、不删除应用数据。预览只读；应用为临时 bind mount，需验证实际系统读取。恢复只卸载本工具确认的挂载，不会删除已经创建的分身。先测试少量应用。");p.addView(info);
   type=new RadioGroup(this);RadioButton clone=new RadioButton(this);clone.setId(101);clone.setText("应用分身（优先测试）");type.addView(clone);RadioButton parallel=new RadioButton(this);parallel.setId(102);parallel.setText("平行视界（实验）");type.addView(parallel);p.addView(type);type.check(101);
   type.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener(){public void onCheckedChanged(RadioGroup g,int id){plan=null;refresh();}});
   packages=new EditText(this);packages.setHint("指定包名（空白=所有第三方应用）");packages.setSingleLine(false);p.addView(packages);
   packages.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){plan=null;refresh();}public void afterTextChanged(android.text.Editable e){}});
   status=button(p,"检查系统挂载环境",new View.OnClickListener(){public void onClick(View v){run(0);}});
   preview=button(p,"生成只读预览",new View.OnClickListener(){public void onClick(View v){run(1);}});
   apply=button(p,"应用当前预览（临时挂载）",new View.OnClickListener(){public void onClick(View v){if(plan==null)return;new AlertDialog.Builder(MainActivity.this).setTitle("确认临时挂载").setMessage("保留原厂配置，仅添加预览中的包名。挂载不保证系统已刷新缓存；不会自动重启。确认无其他同类模块？").setNegativeButton("取消",null).setPositiveButton("应用",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){run(2);}}).show();}});
   restore=button(p,"恢复当前功能的原厂配置",new View.OnClickListener(){public void onClick(View v){new AlertDialog.Builder(MainActivity.this).setTitle("恢复配置").setMessage("仅卸载本工具确认的挂载；不删除分身应用或其数据。系统缓存可能尚未刷新。").setNegativeButton("取消",null).setPositiveButton("恢复",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){run(3);}}).show();}});
   copy=button(p,"复制报告",new View.OnClickListener(){public void onClick(View v){((ClipboardManager)getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("CompatLab C1",result.getText()));}});
   result=new TextView(this);result.setTextIsSelectable(true);result.setTextSize(13);result.setText("先检查挂载环境。");p.addView(result);
   ScrollView sc=new ScrollView(this);sc.addView(p);setContentView(sc);refresh();
 }
 private Button button(LinearLayout p,String s,View.OnClickListener l){Button b=new Button(this);b.setText(s);b.setOnClickListener(l);p.addView(b);return b;}
 private String feature(){return type.getCheckedRadioButtonId()==102?"parallel":"clone";}
 private void refresh(){if(status==null||preview==null||apply==null||restore==null||copy==null)return;boolean can=!busy&&ops!=null;status.setEnabled(can);preview.setEnabled(can);apply.setEnabled(can&&plan!=null);restore.setEnabled(can);copy.setEnabled(!busy);packages.setEnabled(!busy);for(int i=0;i<type.getChildCount();i++)type.getChildAt(i).setEnabled(!busy);}
 private static String q(String s){return "'"+s.replace("'","'\\''")+"'";}
 private Run call(String action,String feature,byte[] input,String...args)throws Exception{
   String cmd=CHECK+"p=$(pidof system_server); case \"$p\" in ''|*[!0-9]*) echo NO_SYSTEM_SERVER; exit 21;; esac; ";
   String inner="/system/bin/sh -c "+q(ops)+" sh "+q(action)+" "+q(feature);
   for(String a:args)inner+=" "+q(a);
   cmd+="if [ \"$(readlink /proc/self/ns/mnt)\" = \"$(readlink /proc/$p/ns/mnt)\" ]; then "+inner+"; else nsenter -t \"$p\" -m -- "+inner+"; fi";
   return exec(cmd,input);
 }
 private static String line(String s,String key){for(String x:s.split("\n"))if(x.startsWith(key+"="))return x.substring(key.length()+1).trim();return "";}
 private static boolean has(String s,String key){for(String x:s.split("\n"))if(x.trim().equals(key))return true;return false;}
 private void run(final int action){
   if(busy||ops==null)return;final String f=feature(),selection=packages.getText().toString().trim();final Plan saved=plan;
   if(action==2&&(saved==null||!f.equals(saved.feature)))return;
   busy=true;plan=null;refresh();result.setText("执行中");
   new Thread(new Runnable(){public void run(){
     String report;Plan next=null;
     try {
       if(action==0||action==3){Run r=call(action==0?"status":"restore",f,null);report="Exit="+r.code+"\n"+r.text();}
       else if(action==1){next=makePlan(f,selection);report=next.description;}
       else {Run r=call("apply",f,saved.xml,saved.base,saved.generated,saved.ns);report="Exit="+r.code+"\n"+r.text()+"\n"+saved.description+"\n请先检查系统设置是否刷新，不要立即重启。";}
     }catch(Throwable e){report="操作未完成：\n"+e;}
     final String text=report;final Plan newPlan=next;runOnUiThread(new Runnable(){public void run(){plan=newPlan;result.setText(text);busy=false;refresh();}});
   }}).start();
 }
 private static boolean pkg(String x){return x.matches("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+")&&x.length()<=200;}
 private Plan makePlan(String f,String selection)throws Exception{
   Run st=call("status",f,null);String state=st.text();if(st.code!=0||!has(state,"END"))throw new IOException(state);
   if(!"0".equals(line(state,"exact_mounts")))throw new IOException("已有挂载，先检查或恢复。\n"+state);
   Run src=call("source",f,null);if(src.code!=0)throw new IOException(src.text());
   String base=sha(src.data);if(!base.equals(line(state,"sha256")))throw new IOException("原厂配置发生变化，请重试");
   Run pk=call("packages",f,null);if(pk.code!=0)throw new IOException(pk.text());
   TreeSet<String> installed=new TreeSet<String>();
   for(String l:pk.text().split("\n"))if(l.startsWith("package:")){String n=l.substring(8).trim();if(pkg(n)&&!n.startsWith("dev.air2023.")&&!n.equals("com.topjohnwu.magisk"))installed.add(n);}
   if(installed.isEmpty())throw new IOException("未读取到第三方应用");
   TreeSet<String> wanted=new TreeSet<String>();
   if(selection.isEmpty())wanted.addAll(installed);else for(String n:selection.split("[\\s,;，；]+")){if(!pkg(n)||!installed.contains(n))throw new IOException("不在可选第三方应用列表："+n);wanted.add(n);}
   String xml=new String(src.data,StandardCharsets.UTF_8);if(!Arrays.equals(src.data,xml.getBytes(StandardCharsets.UTF_8)))throw new IOException("非标准UTF-8，拒绝修改");
   String root=f.equals("clone")?"AppClone":"package_config";
   Set<String> existing=parse(xml,root);TreeSet<String> add=new TreeSet<String>(wanted);add.removeAll(existing);
   int pos=xml.lastIndexOf("</"+root+">");if(pos<0)throw new IOException("未识别根结束标签");
   StringBuilder entries=new StringBuilder("\n");
   for(String n:add){
     if(f.equals("clone"))entries.append("  <package name=\"").append(n).append("\"/>\n");
     else entries.append("  <package window_mode=\"1001\" support_multi_resume=\"true\" support_fullscreen_video=\"true\" support_camera_preview=\"true\" is_scaled=\"false\" need_relaunch=\"false\" default_setting=\"false\" is_dragable=\"true\" notch_adapt=\"false\" drag_to_fullscreen=\"true\" support_lock=\"true\" home=\"\" name=\"").append(n).append("\"/>\n");
   }
   String merged=xml.substring(0,pos)+entries+xml.substring(pos);Set<String> actual=parse(merged,root);Set<String> expected=new HashSet<String>(existing);expected.addAll(add);if(!actual.equals(expected))throw new IOException("生成XML校验失败");
   Plan out=new Plan();out.feature=f;out.base=base;out.xml=merged.getBytes(StandardCharsets.UTF_8);out.generated=sha(out.xml);out.ns=line(state,"namespace");
   out.description=state+"\n已存在条目："+existing.size()+"\n新增："+add.size()+"\n保留已存在条目："+(wanted.size()-add.size())+"\n"+add+"\n原厂SHA256："+base+"\n生成SHA256："+out.generated+"\n";
   if(add.isEmpty())throw new IOException("没有需要新增的包。\n"+out.description);
   if(!has(state,"SHARED_NAMESPACE_OK")||has(state,"TRANSACTION_DIR_EXISTS")||!out.ns.matches("mnt:\\[[0-9]+\\]"))throw new IOException("预览生成成功，但当前命名空间或事务状态不允许挂载。\n"+out.description);
   return out;
 }
 private static Set<String> parse(String text,String root)throws Exception{
   if(text.contains("<!DOCTYPE")||text.contains("<!ENTITY"))throw new IOException("不支持DTD");
   XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);p.setInput(new StringReader(text));
   Set<String> names=new HashSet<String>();boolean seen=false;int t;
   while((t=p.next())!=XmlPullParser.END_DOCUMENT){if(t==XmlPullParser.START_TAG){if(p.getDepth()==1){if(seen||!root.equals(p.getName()))throw new IOException("XML根不匹配");seen=true;}if(p.getDepth()==2&&"package".equals(p.getName())){String n=p.getAttributeValue(null,"name");if(n!=null&&!n.isEmpty())names.add(n);}}}
   if(!seen)throw new IOException("空XML");return names;
 }
 private static String sha(byte[] data)throws Exception{byte[] b=MessageDigest.getInstance("SHA-256").digest(data);StringBuilder s=new StringBuilder();for(byte x:b)s.append(String.format(Locale.US,"%02x",x&255));return s.toString();}
 private static byte[] read(InputStream in,int max)throws IOException{ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] x=new byte[8192];int n;while((n=in.read(x))!=-1){if(b.size()+n>max)throw new IOException("输出超限");b.write(x,0,n);}return b.toByteArray();}
 private static Run exec(String cmd,final byte[] input)throws Exception{
   final Process p=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();
   final byte[][] output=new byte[1][];final Exception[] err=new Exception[2];
   Thread reader=new Thread(new Runnable(){public void run(){try(InputStream in=p.getInputStream()){output[0]=read(in,8*1024*1024);}catch(Exception e){err[0]=e;}}});
   Thread writer=new Thread(new Runnable(){public void run(){try(OutputStream out=p.getOutputStream()){if(input!=null)out.write(input);}catch(Exception e){err[1]=e;}}});
   reader.start();writer.start();boolean done=p.waitFor(45,TimeUnit.SECONDS);if(!done)p.destroyForcibly();reader.join(2000);writer.join(2000);
   if(!done||reader.isAlive()||writer.isAlive())throw new IOException("操作超时，请重新检查挂载状态");if(err[0]!=null)throw err[0];if(output[0]==null)throw new IOException("无输出");
   Run r=new Run();r.code=p.exitValue();r.data=output[0];if(err[1]!=null&&r.code==0)throw err[1];return r;
 }
}
