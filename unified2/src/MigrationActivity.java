package dev.air2023.unified2;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.*;
import android.os.Bundle;
import android.util.Xml;
import android.view.View;
import android.widget.*;
import org.xmlpull.v1.XmlPullParser;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;

public final class MigrationActivity extends Activity {
 private static final String[] FILES={"air2023_u1","air2023_b04","air2023_i4"};
 private static final String[][] BOOLS={{"base","volume","island"},{"timeout","doubletap","seconds","app_details","cells","dedupe","dock","portrait","drawer","notext","form_title","blur","safe"},{"enabled","charge","media","metadata","volume","bluetooth","touch"}};
 private TextView output; private Button inspect,apply; private List<Map<String,Object>> staged; private boolean busy;
 public void onCreate(Bundle b){super.onCreate(b);LinearLayout p=new LinearLayout(this);p.setOrientation(1);p.setPadding(24,24,24,24);
  TextView title=new TextView(this);title.setText("U2 迁移与分身检查");title.setTextSize(24);p.addView(title);
  TextView help=new TextView(this);help.setText("先备份非原厂分身。只读检查原 C4R1 白名单及 U1 配置。导入只写 U2 三组开关；不改旧 APK、不改白名单、不复制 D3/M1 撤销记录。若原配置位置不可读，手动按旧设置配置。导入前需在 Vector 启用 U2，强停并重开 U2；切换过程中不要打开分身设置或重启。");p.addView(help);
  inspect=new Button(this);inspect.setText("只读检查原分身名单及 U1 配置");p.addView(inspect);inspect.setOnClickListener(new View.OnClickListener(){public void onClick(View v){check();}});
  apply=new Button(this);apply.setText("确认导入 U1 三组开关");apply.setEnabled(false);p.addView(apply);apply.setOnClickListener(new View.OnClickListener(){public void onClick(View v){new AlertDialog.Builder(MigrationActivity.this).setTitle("导入到 U2").setMessage("覆盖 U2 的三组开关、基础设置和灵动岛设置。不会修改旧模块或重启。").setNegativeButton("取消",null).setPositiveButton("导入",new DialogInterface.OnClickListener(){public void onClick(DialogInterface d,int w){importPrefs();}}).show();}});
  output=new TextView(this);output.setTextIsSelectable(true);p.addView(output);ScrollView sc=new ScrollView(this);sc.addView(p);setContentView(sc);
 }
 private void check(){if(busy)return;busy=true;staged=null;apply.setEnabled(false);inspect.setEnabled(false);output.setText("检查中");
  new Thread(new Runnable(){public void run(){String msg;List<Map<String,Object>> data=null;
   try{msg=dev.air2023.unified2.clone.MainActivity.preflight();}catch(Exception e){msg="分身名单检查失败： "+e+"\n不要切换或软重启；保留 C4R1。";}
   try{List<Map<String,Object>> tmp=new ArrayList<Map<String,Object>>();for(int i=0;i<FILES.length;i++)tmp.add(parse(readOld(FILES[i]),i));data=tmp;msg+="\nU1 三组配置读取及类型检查通过，可导入。\n"+tmp;}catch(Exception e){msg+="\nU1 配置未能完整读取： "+e+"\n请手动对照旧 U1 设置，不会写入部分配置。";}
   final String text=msg;final List<Map<String,Object>> vals=data;runOnUiThread(new Runnable(){public void run(){if(isFinishing()||isDestroyed())return;busy=false;staged=vals;inspect.setEnabled(true);apply.setEnabled(vals!=null);output.setText(text);}});
  }}).start();
 }
 private static String readOld(String name)throws Exception{
  String path="/data/user/0/dev.air2023.unified1/shared_prefs/"+name+".xml";
  String cmd="if [ \"$(id -u)\" != 0 ]; then exit 20; fi; f='"+path+"'; if [ ! -f \"$f\" ]; then f=$(find /data/misc -maxdepth 6 -type f -path '*/dev.air2023.unified1/"+name+".xml' 2>/dev/null); fi; [ -n \"$f\" ] && [ -f \"$f\" ] && [ \"$(wc -c < \"$f\")\" -le 65536 ] || exit 21; cat \"$f\"";
  final Process proc=new ProcessBuilder("su","-c",cmd).redirectErrorStream(true).start();final ByteArrayOutputStream buf=new ByteArrayOutputStream();Thread reader=new Thread(new Runnable(){public void run(){try(InputStream in=proc.getInputStream()){byte[] b=new byte[2048];int n;while((n=in.read(b))!=-1){if(buf.size()+n>65536)return;buf.write(b,0,n);}}catch(IOException e){}}});reader.start();boolean done=proc.waitFor(20,TimeUnit.SECONDS);if(!done)proc.destroyForcibly();reader.join(1000);if(!done||reader.isAlive()||proc.exitValue()!=0)throw new IOException(name+" 无法读取");return new String(buf.toByteArray(),StandardCharsets.UTF_8);
 }
 private static Map<String,Object> parse(String xml,int index)throws Exception{
  XmlPullParser p=Xml.newPullParser();p.setFeature(XmlPullParser.FEATURE_PROCESS_DOCDECL,false);p.setInput(new StringReader(xml));Map<String,Object> m=new HashMap<String,Object>();
  Set<String> bool=new HashSet<String>(Arrays.asList(BOOLS[index]));
  Set<String> ints=new HashSet<String>();ints.add("schema");if(index==1)ints.add("radius");if(index==2){ints.add("offset");ints.add("width");}
  boolean root=false;int t;while((t=p.next())!=XmlPullParser.END_DOCUMENT){if(t!=XmlPullParser.START_TAG)continue;if(p.getDepth()==1){if(!"map".equals(p.getName()))throw new IOException("invalid map");root=true;continue;}if(p.getDepth()!=2)continue;String k=p.getAttributeValue(null,"name"),v=p.getAttributeValue(null,"value");if(bool.contains(k)){if(!"boolean".equals(p.getName())||!("true".equals(v)||"false".equals(v))||m.containsKey(k))throw new IOException("boolean "+k);m.put(k,Boolean.valueOf(v));}else if(ints.contains(k)){if(!"int".equals(p.getName())||m.containsKey(k))throw new IOException("int "+k);m.put(k,Integer.valueOf(v));}}
  if(!root||!Integer.valueOf(index==0?1:4).equals(m.get("schema"))||!m.keySet().containsAll(bool)||!m.keySet().containsAll(ints))throw new IOException("missing keys "+FILES[index]);
  if(index==1&&((Integer)m.get("radius")<1||(Integer)m.get("radius")>100))throw new IOException("radius");
  if(index==2&&((Integer)m.get("offset")< -120||(Integer)m.get("offset")>120||(Integer)m.get("width")<80||(Integer)m.get("width")>125))throw new IOException("island geometry");
  return m;
 }
 private void importPrefs(){if(staged==null||busy)return;try{
   SharedPreferences[] p=new SharedPreferences[3];for(int i=0;i<3;i++)p[i]=getSharedPreferences(FILES[i],MODE_WORLD_READABLE);
   // Publish subordinate settings first; master flags last.
   for(int j:new int[]{1,2,0}){SharedPreferences.Editor ed=p[j].edit();for(Map.Entry<String,Object> e:staged.get(j).entrySet()){if(e.getValue() instanceof Boolean)ed.putBoolean(e.getKey(),(Boolean)e.getValue());else ed.putInt(e.getKey(),(Integer)e.getValue());}ed.putLong("published",System.currentTimeMillis());if(!ed.commit())throw new IOException("写入 "+FILES[j]);}
   Toast.makeText(this,"导入完成，返回首页核对并重新发布",Toast.LENGTH_LONG).show();setResult(RESULT_OK);finish();
  }catch(Exception e){output.append("\n导入未确认："+e+"；不要重启，请核对三组开关并重新发布。");}
 }
}
