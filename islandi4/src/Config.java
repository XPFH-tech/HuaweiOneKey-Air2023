package dev.air2023.islandi4;
import java.util.*;
final class Config{
 static final String FILE="air2023_i4";
 static final String[] KEYS={"enabled","charge","media","metadata","volume","bluetooth","touch"};
 static final String[] NAMES={"灵动岛总开关","充电提示","媒体播放","歌曲名称及封面","媒体音量提示","蓝牙音频接入提示","点击展开音乐控制"};
 static Map<?,?> data=Collections.emptyMap();
 static boolean load()throws Exception{
   Class<?> c=Class.forName("de.robv.android.xposed.XSharedPreferences");
   Object x=c.getConstructor(String.class,String.class).newInstance("dev.air2023.islandi4",FILE);
   c.getMethod("reload").invoke(x);
   Object all=c.getMethod("getAll").invoke(x);
   if(!(all instanceof Map))throw new IllegalStateException("not map");
   Map<?,?> m=(Map<?,?>)all;
   if(!Integer.valueOf(4).equals(m.get("schema")))throw new IllegalStateException("configuration unavailable");
   for(String key:KEYS)if(!(m.get(key) instanceof Boolean))throw new IllegalStateException("missing setting "+key);
   data=new HashMap<Object,Object>(m);
   return on("enabled");
 }
 static boolean on(String key){return Boolean.TRUE.equals(data.get(key));}
 static int number(String key,int fallback,int min,int max){
   Object v=data.get(key);return v instanceof Integer?Math.max(min,Math.min(max,(Integer)v)):fallback;
 }
 static int offset(){return number("offset",0,-120,120);}
 static int width(){return number("width",100,80,125);}
}
