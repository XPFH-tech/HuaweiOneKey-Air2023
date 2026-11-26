package dev.air2023.themeprobe1;
import android.app.Activity;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.View;
import android.widget.*;

public final class MainActivity extends Activity{
 private static final String PKG="com.huawei.android.thememanager",REF="12.0.44.300";
 TextView result;Button open;
 int dp(int n){return Math.round(n*getResources().getDisplayMetrics().density);}
 @Override public void onCreate(Bundle b){
   super.onCreate(b);LinearLayout body=new LinearLayout(this);body.setOrientation(1);body.setPadding(dp(20),dp(18),dp(20),dp(20));
   TextView title=new TextView(this);title.setTextSize(24);title.setText("Air2023 主题兼容检测 T1");body.addView(title);
   TextView note=new TextView(this);note.setText("只读诊断，不解锁主题，不改变试用、购买或会员状态。无需 Root 授权。\n第一步：直接读取主题应用版本。第二步（可选）：在 Vector 仅勾选华为主题，强行停止主题应用后重新打开，查看 Air2023ThemeT1 日志。不要勾选系统框架，不必软重启。U1 和 C4R1 保持不变。");body.addView(note);
   Button refresh=new Button(this);refresh.setText("重新检测");body.addView(refresh);refresh.setOnClickListener(new View.OnClickListener(){public void onClick(View v){scan();}});
   open=new Button(this);open.setText("打开华为主题");body.addView(open);open.setOnClickListener(new View.OnClickListener(){public void onClick(View v){try{Intent i=getPackageManager().getLaunchIntentForPackage(PKG);if(i==null)throw new ActivityNotFoundException();startActivity(i);}catch(Throwable t){Toast.makeText(MainActivity.this,"没有可用的主题启动入口",Toast.LENGTH_SHORT).show();}}});
   Button copy=new Button(this);copy.setText("复制检测报告");body.addView(copy);copy.setOnClickListener(new View.OnClickListener(){public void onClick(View v){ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);if(cm!=null){cm.setPrimaryClip(ClipData.newPlainText("Air2023 Theme T1",result.getText()));Toast.makeText(MainActivity.this,"已复制",Toast.LENGTH_SHORT).show();}}});
   result=new TextView(this);result.setTextIsSelectable(true);result.setTextSize(15);body.addView(result);
   ScrollView sc=new ScrollView(this);sc.addView(body);setContentView(sc);scan();
 }
 void scan(){
   StringBuilder s=new StringBuilder("T1_REPORT\n型号："+Build.MODEL+"\n系统："+Build.DISPLAY+"\n增量："+Build.VERSION.INCREMENTAL+"\nSDK："+Build.VERSION.SDK_INT+"\n目标固件："+("DBY2-W00".equals(Build.MODEL)&&"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)&&Build.VERSION.SDK_INT==31?"匹配":"不匹配")+"\n\n主题包名："+PKG+"\n原项目参考版本："+REF+"\n");
   try{
     PackageManager pm=getPackageManager();PackageInfo p=pm.getPackageInfo(PKG,PackageManager.GET_ACTIVITIES|PackageManager.GET_SERVICES);
     ApplicationInfo a=p.applicationInfo;
     s.append("实际版本：").append(p.versionName).append("\n版本代码：").append(Build.VERSION.SDK_INT>=28?p.getLongVersionCode():p.versionCode);
     s.append("\n参考版本对照：").append(REF.equals(p.versionName)?"相同（不代表全部接口兼容）":"不同（需要独立验证）");
     if(a!=null){s.append("\n应用启用：").append(a.enabled).append("\n系统预装或更新：").append((a.flags&(ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0).append("\nTarget SDK：").append(a.targetSdkVersion).append("\n拆分 APK 数量：").append(a.splitSourceDirs==null?0:a.splitSourceDirs.length);}
     s.append("\n声明 Activity 数量：").append(p.activities==null?0:p.activities.length).append("\n声明 Service 数量：").append(p.services==null?0:p.services.length);
     boolean launch=pm.getLaunchIntentForPackage(PKG)!=null;open.setEnabled(launch);s.append("\n桌面启动入口：").append(launch?"存在":"未发现");
   }catch(PackageManager.NameNotFoundException e){open.setEnabled(false);s.append("主题应用未发现或不可见");}
   catch(Throwable e){open.setEnabled(false);s.append("检测失败：").append(e.getClass().getSimpleName());}
   try{Class.forName("android.hwtheme.HwThemeManager",false,getClassLoader());s.append("\n普通应用进程框架类：可见");}
   catch(Throwable e){s.append("\n普通应用进程框架类：不可见或受限（不代表系统进程不存在）");}
   s.append("\n\nVector 深入检测：请另行查看 T1_START / T1_FRAMEWORK_CLASS / T1_LOCAL_PREVIEW / T1_ONLINE_PREVIEW / T1_DONE。仅检查类是否存在，不验证授权方法或完整运行兼容性。\nT1_END");
   result.setText(s.toString());
 }
}
