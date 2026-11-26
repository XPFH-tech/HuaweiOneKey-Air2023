package dev.air2023.magicp1;
import android.os.Build;
import android.util.Log;
import android.util.Xml;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class Entry implements IXposedHookLoadPackage {
 private static final AtomicBoolean installed=new AtomicBoolean();
 private static final String TAG="Air2023MagicP1";
 private static final int LIMIT=6;
 private static final long MAX_XML=16L*1024*1024;
 private static final Map<Object,Set<String>> added=Collections.synchronizedMap(new WeakHashMap<Object,Set<String>>());
 private static void report(String s){try{XposedBridge.log("["+TAG+"] "+s);}catch(Throwable ignored){}try{Log.i(TAG,s);}catch(Throwable ignored){}}
 private static Method method(Class<?> c,String n,Class<?> ret,Class<?>... args)throws Exception{
   Method m=c.getDeclaredMethod(n,args);if(m.getReturnType()!=ret||Modifier.isStatic(m.getModifiers()))throw new NoSuchMethodException(n);m.setAccessible(true);return m;
 }
 private static Field field(Class<?> c,String n,Class<?> type)throws Exception{
   Field f=c.getDeclaredField(n);if(f.getType()!=type||Modifier.isStatic(f.getModifiers()))throw new NoSuchFieldException(n);f.setAccessible(true);return f;
 }
 @Override public void handleLoadPackage(XC_LoadPackage.LoadPackageParam p){
   if(p==null||!"android".equals(p.packageName)||!"android".equals(p.processName))return;
   if(!"DBY2-W00".equals(Build.MODEL)||!"DBY2-W00 4.2.0.252(C00E100R3P5)".equals(Build.DISPLAY)||Build.VERSION.SDK_INT!=31){report("P1_SKIP_BUILD");return;}
   if(!installed.compareAndSet(false,true))return;
   try{install(p.classLoader);}catch(Throwable t){report("P1_INSTALL_ERROR "+t);}
 }
 private static void install(ClassLoader cl)throws Exception{
   final Class<?> cfg=Class.forName("com.huawei.config.HwMagicWindowConfig",false,cl);
   final Class<?> loader=Class.forName("com.huawei.config.HwMagicWindowConfigLoader",false,cl);
   final Class<?> attr=Class.forName("com.huawei.display.DeviceAttribute",false,cl);
   final Class<?> base=Class.forName("com.huawei.config.BaseAppConfig",false,cl);
   final Class<?> local=Class.forName("com.huawei.config.LocalConfig",false,cl);
   final Method load=method(loader,"loadPackage",void.class,cfg,String.class,int.class);
   final Method listFiles=method(loader,"getLocalConfigFiles",List.class,int.class);
   final Method parse=method(loader,"parsePackageXmlIfNeeded",boolean.class,cfg,XmlPullParser.class,String.class);
   final Method installedNames=method(cfg,"getAllInstalledPkgList",List.class);
   final Method getConfig=method(cfg,"getAppConfig",base,String.class);
   final Method mode=method(cfg,"getWindowMode",int.class,String.class);
   final Method defaultSetting=method(local,"isDefaultSetting",boolean.class);
   final Method sync=method(cfg,"syncSettingsWithWhiteList",void.class);
   final Method getSwitches=method(cfg,"getHwMagicWinEnabledApps",Map.class);
   final Field currentUser=field(cfg,"mCurrentUserId",int.class);
   final Field cfgScene=field(cfg,"mScene",int.class);
   final Field cfgLoader=field(cfg,"mCfgLoader",loader);
   final Field cfgAttr=field(cfg,"mAttribute",attr);
   final Field locals=field(cfg,"mLocalConfigs",Map.class),easy=field(cfg,"mEasyGoConfigs",Map.class);
   final Method virtual=method(attr,"isVirtualContainer",boolean.class);
   final Method productive=method(attr,"isProductiveDesktopContainer",boolean.class);
   final Method fold=method(attr,"isFoldableDevice",boolean.class);
   XposedBridge.hookMethod(sync,new XC_MethodHook(){
     @Override protected void afterHookedMethod(MethodHookParam mp){
       if(mp.getThrowable()!=null)return;
       try{
         Set<String> names=added.get(mp.thisObject);if(names==null||names.isEmpty())return;
         Map<?,?> switches=(Map<?,?>)getSwitches.invoke(mp.thisObject);
         for(String n:names)report("P1_NATIVE_SWITCH package="+n+" present="+switches.containsKey(n)+" enabled="+switches.get(n));
       }catch(Throwable t){report("P1_SWITCH_ERROR "+t.getClass().getName());}
     }
   });
   XposedBridge.hookMethod(load,new XC_MethodHook(){
     @Override protected void afterHookedMethod(MethodHookParam mp){
       if(mp.getThrowable()!=null)return;
       try{
         Object c=mp.args[0];
         // Full load, only within initial local-container construction. Never mutate live maps on install events.
         if(!"".equals(mp.args[1])||!Integer.valueOf(0).equals(mp.args[2])||currentUser.getInt(c)!=0||cfgScene.getInt(c)!=0||cfgLoader.get(c)!=mp.thisObject)return;
         boolean initial=false;
         for(StackTraceElement f:Thread.currentThread().getStackTrace())if(cfg.getName().equals(f.getClassName())&&"init".equals(f.getMethodName())){initial=true;break;}
         if(!initial)return;
         Object a=cfgAttr.get(c);
         if(a==null||Boolean.TRUE.equals(virtual.invoke(a))||Boolean.TRUE.equals(productive.invoke(a))||Boolean.TRUE.equals(fold.invoke(a)))return;
         if(added.containsKey(c))return;
         Map<?,?> localMap=(Map<?,?>)locals.get(c),easyMap=(Map<?,?>)easy.get(c);
         if(localMap==null||localMap.isEmpty()||easyMap==null){report("P1_SKIP_EMPTY_NATIVE_CONFIG");return;}
         Set<String> approved=Policy.readSnapshot();
         List<?> nativeInstalled=(List<?>)installedNames.invoke(c);
         if(nativeInstalled==null||nativeInstalled.isEmpty()){report("P1_SKIP_NO_INSTALLED");return;}
         List<?> nativeFiles=(List<?>)listFiles.invoke(mp.thisObject,0);
         Set<String> stockNames=readStock(nativeFiles);
         if(stockNames.size()<100){report("P1_SKIP_STOCK_TOO_SMALL "+stockNames.size());return;}
         // Include only previously reviewed C4R1 candidates. Stock entries, including explicitly
         // disabled, region-forbidden, and duplicate entries, are excluded using the raw XML names.
         Set<String> eligible=new TreeSet<String>();
         for(String n:approved)if(Policy.valid(n)&&!Policy.excluded(n)&&nativeInstalled.contains(n)&&!stockNames.contains(n)&&!localMap.containsKey(n)&&!easyMap.containsKey(n)&&getConfig.invoke(c,n)==null)eligible.add(n);
         List<String> order=new ArrayList<String>();
         for(String n:new String[]{"org.zotero.android","com.mickstarify.zooforzotero","com.kylin.read","com.jingdong.app.reader.campus","com.maimemo.android.momo","com.cnki.android.cnkimobile","com.positron_it.zlib"})if(eligible.remove(n))order.add(n);
         order.addAll(eligible);
         report("P1_PREVIEW stockNames="+stockNames.size()+" installed="+nativeInstalled.size()+" eligible="+order.size()+" pilotLimit="+LIMIT);
         Set<String> done=new LinkedHashSet<String>();added.put(c,done);
         for(String n:order){
           if(done.size()>=LIMIT)break;
           if(localMap.containsKey(n)||easyMap.containsKey(n)||getConfig.invoke(c,n)!=null)continue;
           // Use OEM parser and OEM synchronization, not an independently constructed config object.
           String x="<package name=\""+n+"\" window_mode=\"1001\" default_setting=\"false\" support_fullscreen_video=\"true\" support_multi_resume=\"false\" support_camera_preview=\"true\" is_scaled=\"false\" need_relaunch=\"false\" is_dragable=\"false\" notch_adapt=\"false\" home=\"\" drag_to_fullscreen=\"false\" support_lock=\"true\" is_force_fullscreen=\"false\" dragging_to_freeform=\"false\" show_in_freeform=\"false\" need_refine_ui=\"false\"/>";
           XmlPullParser parser=Xml.newPullParser();parser.setInput(new StringReader(x));
           if(parser.nextTag()!=XmlPullParser.START_TAG)throw new IOException("synthetic XML");
           parse.invoke(mp.thisObject,c,parser,"");
           Object created=localMap.get(n);
           if(created==null){report("P1_NATIVE_PARSER_SKIP "+n);continue;}
           if(!local.isInstance(created)||!Integer.valueOf(1).equals(mode.invoke(c,n))||!Boolean.FALSE.equals(defaultSetting.invoke(created))){
             report("P1_UNEXPECTED_CONFIG_STOP "+n);break;
           }
           done.add(n);report("P1_CONFIG_ADDED "+n+" default=false mode=1");
         }
         report("P1_LOAD_DONE added="+done.size());
       }catch(Throwable t){report("P1_LOAD_ERROR "+t);}
     }
   });
   report("P1_HOOK_READY");
 }
 private static Set<String> readStock(List<?> files)throws Exception{
   if(files==null||files.isEmpty()||files.size()>32)throw new IOException("native source list");
   Set<String> names=new HashSet<String>();long total=0;
   for(Object o:files){
     if(!(o instanceof File))throw new IOException("native source type");
     File f=(File)o;
     if(!f.isFile()||!f.canRead()||f.length()<=0)throw new IOException("native source unreadable");
     total+=f.length();if(total>MAX_XML)throw new IOException("native XML total too large");
     try(InputStream in=new BufferedInputStream(new FileInputStream(f))){
       XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);p.setInput(in,null);
       if(p.nextTag()!=XmlPullParser.START_TAG||!"package_config".equals(p.getName()))throw new IOException("native XML root mismatch");
       int t;while((t=p.next())!=XmlPullParser.END_DOCUMENT)if(t==XmlPullParser.START_TAG&&"package".equals(p.getName())){
         String n=p.getAttributeValue(null,"name");if(n!=null&&!n.isEmpty())names.add(n);
         if(names.size()>100000)throw new IOException("native XML entry cap");
       }
     }
   }
   return names;
 }
}
