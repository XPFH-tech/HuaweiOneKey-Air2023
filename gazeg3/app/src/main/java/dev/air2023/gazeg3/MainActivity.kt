package dev.air2023.gazeg3

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.*
import android.provider.Settings
import android.view.*
import android.widget.*
import android.text.Editable
import android.text.TextWatcher
import android.app.AlertDialog

class MainActivity:Activity(){
    private val h=Handler(Looper.getMainLooper());private lateinit var status:TextView
    private lateinit var chosen:TextView;private var selectedPkg="";private lateinit var edgeBox:CheckBox;private var calEdge=true;private lateinit var gestures:Spinner;private lateinit var dwell:SeekBar;private lateinit var dwellLabel:TextView;private lateinit var layouts:Spinner
    private var calibrating=false;private var stage=0;private var stageStart=0L;private var last=0L;private var calRot=0
    private var target:TextView?=null;private var prompt:TextView?=null
    private val frames=ArrayList<EyeFrame>();private val med=ArrayList<Triple<Float,Float,Float>>()
    private var dual=false;private var calPanel:FrameLayout?=null
    private val prefs by lazy{getSharedPreferences("g1",0)}
    private val pulse=object:Runnable{override fun run(){
        val f=Session.latest;val now=SystemClock.elapsedRealtime()
        if(calibrating){
            if(!Session.running||rotation(this@MainActivity)!=calRot){abort("相机停止或方向变化");}
            else{
                val elapsed=now-stageStart
                if(elapsed>1000&&f!=null&&f.valid&&f.rotation==calRot&&f.time!=last&&now-f.time in 0..500){frames.add(f);last=f.time}
                prompt?.text="注视圆点 · "+(if(dual)listOf("中间","左下","右下","左上","右上") else listOf("中间","底部","顶部"))[stage]+" · "+frames.size+" 帧"
                if(elapsed>=3500){
                    if(frames.size<8){abort("有效帧不足，请正对屏幕并增加环境光")}
                    else{
                        fun median(x:List<Float>)=x.sorted()[x.size/2]
                        med.add(Triple(median(frames.map{it.iris}),median(frames.map{it.ear}),median(frames.map{it.horizontal})))
                        stage++
                        if(stage==(if(dual)5 else 3)){
                            val c=if(dual)Calibration(calRot,med[0].first,(med[3].first+med[4].first)/2,(med[1].second+med[2].second)/2,maxOf(med[0].second,(med[3].second+med[4].second)/2),true,med[3].third,med[4].third,med[1].third,med[2].third,calEdge) else Calibration(calRot,med[0].first,med[2].first,med[1].second,maxOf(med[0].second,med[2].second),edge=calEdge)
                            if(!c.good())abort("校准区分度不足，请固定头部重试；双页需要左右虹膜分离")
                            else{save(c);calibrating=false;Session.calibration=c;home();toast("校准成功")}
                        }else showStage()
                    }
                }
            }
        }else if(::status.isInitialized){
            status.text=Session.status+"\n方向："+rotation(this@MainActivity)+" | 无障碍："+(if(FlipService.current!=null)"已连接" else "未连接")+"\n"+(if(f?.valid==true&&now-f.time<500)"上下 %.3f · 左右 %.3f · EAR %.3f".format(f.iris,f.horizontal,f.ear) else "未检测到有效单人脸")+"\n眼动触发："+Session.triggerCount+" | 完成手势："+Session.count+" | 本方向校准："+(if(load()!=null)"有效" else "未校准")+"\n帧数："+Session.validCount+"/"+Session.frameCount+"\n"+Session.diagnostic+"\n最近动作："+Session.lastAction+"\n手势测试："+Session.testResult
        }
        h.postDelayed(this,120)
    }}
    override fun onCreate(b:Bundle?){super.onCreate(b);home();h.post(pulse)}
    private fun toast(s:String)=Toast.makeText(this,s,Toast.LENGTH_LONG).show()
    private fun home(){
        val p=LinearLayout(this);p.orientation=1;p.setPadding(24,24,24,24)
        fun text(s:String,size:Float=15f):TextView=TextView(this).apply{text=s;textSize=size;p.addView(this)}
        fun button(s:String,action:()->Unit){p.addView(Button(this).apply{text=s;setOnClickListener{action()}})}
        text("Air2023 眼动阅读 G3",25f)
        text("仅前置相机、本地计算。横竖屏单独校准；仅指定的全屏阅读应用发送手势。切换方向、锁屏、过热或45分钟自动停止。")
        status=text(Session.status,17f)
        selectedPkg=prefs.getString("pkg","")?:""
        button("1. 启用眼动翻页无障碍服务"){startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}
        button("2. 启动前置摄像头"){
            if(checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED)requestPermissions(arrayOf(Manifest.permission.CAMERA),10)
            else startForegroundService(Intent(this,TrackingService::class.java))
        }
        text("阅读排版")
        layouts=Spinner(this);layouts.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("单页 / 纵向滚动","横屏双页（左→右）"));layouts.setSelection(prefs.getInt(profileKey(selectedPkg,"layout"),prefs.getInt("layout"+rotation(this),if(resources.configuration.orientation==2)1 else 0)).coerceIn(0,1));p.addView(layouts)
        edgeBox=CheckBox(this).apply{text="靠边校准（推荐：更偏左上、右下）";isChecked=prefs.getBoolean("edge"+rotation(this@MainActivity),true);setOnCheckedChangeListener{_,v->prefs.edit().putBoolean("edge"+rotation(this@MainActivity),v).apply()}};p.addView(edgeBox)
        button("3. 当前排版校准（三点 / 五点）"){begin()}
        text("阅读应用 · 从本机选择")
        chosen=text("",18f)
        refreshChosen()
        button("选择已安装的阅读软件"){pickReader()}
        button("仅打开所选阅读软件（不启用翻页）"){
            val app=selectedApp()?:return@button
            saveProfile();pauseActions();launch(app)
        }
        text("下一页手势")
        gestures=Spinner(this);gestures.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,arrayOf("左滑","右滑","上滑","下滑","右侧点击"));gestures.setSelection(prefs.getInt(profileKey(selectedPkg,"gesture"),prefs.getInt("gesture",0)).coerceIn(0,4));p.addView(gestures)
        dwellLabel=text("");dwell=SeekBar(this);dwell.max=12;dwell.progress=((prefs.getLong(profileKey(selectedPkg,"dwell"),prefs.getLong("dwell",550))-350)/100).toInt().coerceIn(0,12);p.addView(dwell)
        fun update(){dwellLabel.text="驻留时间："+(350+dwell.progress*100)+" ms（底部及顶部各一次）"}
        dwell.setOnSeekBarChangeListener(object:SeekBar.OnSeekBarChangeListener{override fun onProgressChanged(s:SeekBar?,n:Int,u:Boolean){update()};override fun onStartTrackingTouch(s:SeekBar?){};override fun onStopTrackingTouch(s:SeekBar?){}});update()
        val show=CheckBox(this).apply{text="阅读时显示文字提示（触发/完成/失败）";isChecked=prefs.getBoolean("overlay",true);setOnCheckedChangeListener{_,b->Session.overlay=b;prefs.edit().putBoolean("overlay",b).apply()}};p.addView(show);Session.overlay=show.isChecked
        button("独立手势测试（无需校准）"){
            if(!Session.running||FlipService.current==null){toast("请先启动相机服务并启用无障碍");return@button}
            val app=selectedApp()?:return@button;val name=app.pkg;saveProfile()
            Session.clearFeedback();Session.enabled=false;Session.target=name;Session.gesture=gestures.selectedItemPosition;Session.testResult="等待目标应用";val now=SystemClock.elapsedRealtime();Session.testAfter=now+3000;Session.testUntil=now+20000;Session.note("TEST_ARMED "+name);toast("20秒内进入目标阅读页，只发送一次手势")
        }
        button("4. 启用眼动翻页并打开所选阅读软件"){
            val c=load();val app=selectedApp()?:return@button;val name=app.pkg
            if(!Session.running||FlipService.current==null||c==null){toast("先启动相机、启用无障碍并完成本方向校准");return@button}
            saveProfile();pauseActions();Session.target=name;Session.calibration=c;Session.gesture=gestures.selectedItemPosition;Session.dwell=(350+dwell.progress*100).toLong()
            prefs.edit().putString("pkg",name).putInt("gesture",Session.gesture).putLong("dwell",Session.dwell).apply()
            Session.armAfter=SystemClock.elapsedRealtime()+3500;Session.enabled=true
            if(!launch(app)){pauseActions();return@button}
            toast("眼动翻页已开启 · 请进入正文")
        }
        button("暂停翻页（保持相机）"){Session.clearFeedback();Session.enabled=false;Session.testUntil=0;FlipService.current?.hideHint()}
        button("停止相机与翻页"){Session.clearFeedback();Session.testUntil=0;Session.enabled=false;stopService(Intent(this,TrackingService::class.java))}
        button("复制 G3 诊断报告"){val report="G3_REPORT\n"+status.text+"\n"+Session.report();(getSystemService(CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(android.content.ClipData.newPlainText("G3",report));toast("已复制")}
        text("进入书籍正文后使用眼动翻页；模块不能区分正文与书架，浏览书架时建议暂停。")
        text("提示：固定平板，约30–60cm，正常读到底部并停留，再抬眼到顶部。不同方向需分别校准；不支持小窗、分屏。可随时从通知栏停止。")
        val sc=ScrollView(this);sc.addView(p);setContentView(sc)
    }
    private fun profileKey(name:String,key:String)="app."+name+".r"+rotation(this)+"."+key
    private fun refreshChosen(){val a=ReaderApps.list(this).firstOrNull{it.pkg==selectedPkg};chosen.text=if(a==null)"未选择可用阅读软件" else a.title+"\n"+a.pkg}
    private fun selectedApp():ReaderApp?{
        val a=ReaderApps.list(this).firstOrNull{it.pkg==selectedPkg}
        if(a==null)toast("请先选择已安装且可打开的阅读软件")
        return a
    }
    private fun saveProfile(){
        if(selectedPkg.isBlank()||!::gestures.isInitialized||!::dwell.isInitialized)return
        prefs.edit().putString("pkg",selectedPkg).putInt(profileKey(selectedPkg,"layout"),layouts.selectedItemPosition)
            .putInt(profileKey(selectedPkg,"gesture"),gestures.selectedItemPosition)
            .putLong(profileKey(selectedPkg,"dwell"),(350+dwell.progress*100).toLong()).apply()
    }
    private fun pauseActions(){Session.clearFeedback();Session.enabled=false;Session.testUntil=0;Session.armAfter=0;FlipService.current?.hideHint()}
    private fun launch(a:ReaderApp):Boolean{
        return try{startActivity(a.launchIntent());Session.note("LAUNCH "+a.pkg);true}catch(e:Exception){Session.note("LAUNCH_FAILED "+e.javaClass.simpleName);toast("无法打开 "+a.title+"，请检查应用是否可用");false}
    }
    private fun pickReader(){
        val all=ReaderApps.list(this)
        val box=LinearLayout(this).apply{orientation=1;setPadding(24,8,24,8)}
        val query=EditText(this).apply{hint="搜索名称或包名";setSingleLine(true)}
        val more=CheckBox(this).apply{text="显示其他可启动应用";isChecked=false}
        val info=TextView(this);val list=ListView(this)
        box.addView(query);box.addView(more);box.addView(info);box.addView(list,LinearLayout.LayoutParams(-1,(300*resources.displayMetrics.density).toInt()))
        var visible=emptyList<ReaderApp>()
        fun filter(){
            val q=query.text.toString().trim()
            visible=all.filter{(more.isChecked||it.recommended)&&(q.isEmpty()||it.title.contains(q,true)||it.pkg.contains(q,true))}
            info.text=if(visible.isEmpty())"未找到匹配应用，可勾选显示其他应用" else "已安装应用："+visible.size
            list.adapter=object:ArrayAdapter<ReaderApp>(this@MainActivity,android.R.layout.simple_list_item_2,android.R.id.text1,visible){
                override fun getView(position:Int,convertView:View?,parent:android.view.ViewGroup):View{
                    val row=super.getView(position,convertView,parent);val a=getItem(position)!!
                    row.findViewById<TextView>(android.R.id.text1).text=a.title
                    row.findViewById<TextView>(android.R.id.text2).text=a.pkg
                    return row
                }
            }
        }
        val dialog=AlertDialog.Builder(this).setTitle("选择阅读软件").setView(box).setNegativeButton("取消",null).create()
        list.setOnItemClickListener{_,_,n,_->val a=visible.getOrNull(n)?:return@setOnItemClickListener;saveProfile();pauseActions();selectedPkg=a.pkg;prefs.edit().putString("pkg",a.pkg).apply()
            layouts.setSelection(prefs.getInt(profileKey(a.pkg,"layout"),if(resources.configuration.orientation==2)1 else 0).coerceIn(0,1))
            gestures.setSelection(prefs.getInt(profileKey(a.pkg,"gesture"),0).coerceIn(0,4))
            dwell.progress=((prefs.getLong(profileKey(a.pkg,"dwell"),550)-350)/100).toInt().coerceIn(0,12)
            refreshChosen();dialog.dismiss()}
        query.addTextChangedListener(object:TextWatcher{override fun beforeTextChanged(s:CharSequence?,start:Int,count:Int,after:Int){};override fun onTextChanged(s:CharSequence?,start:Int,before:Int,count:Int){filter()};override fun afterTextChanged(s:Editable?) {}})
        more.setOnCheckedChangeListener{_,_->filter()}
        filter();dialog.show()
    }
    private fun begin(){
        if(!Session.running){toast("请先开启摄像头");return}
        saveProfile();Session.clearFeedback();Session.testUntil=0;Session.enabled=false;calibrating=true;calRot=rotation(this);calEdge=edgeBox.isChecked;dual=layouts.selectedItemPosition==1;prefs.edit().putInt("layout"+calRot,if(dual)1 else 0).apply();stage=0;med.clear()
        val p=FrameLayout(this);calPanel=p;p.setBackgroundColor(Color.rgb(18,20,24))
        target=TextView(this).apply{text="●";textSize=48f;setTextColor(Color.GREEN);gravity=Gravity.CENTER}
        prompt=TextView(this).apply{setTextColor(Color.WHITE);textSize=16f;gravity=Gravity.CENTER}
        p.addView(target,FrameLayout.LayoutParams(-2,-2))
        p.addView(prompt,FrameLayout.LayoutParams(-1,-2,Gravity.CENTER_VERTICAL))
        val cancel=Button(this).apply{text="取消校准";setOnClickListener{abort("已取消")}}
        p.addView(cancel,FrameLayout.LayoutParams(-2,-2,Gravity.END or Gravity.CENTER_VERTICAL))
        setContentView(p);p.post{if(calibrating)showStage()}
    }
    private fun showStage(){
        frames.clear();last=0;stageStart=SystemClock.elapsedRealtime()
        val panel=calPanel?:return
        val lx=if(calEdge).12f else .25f;val rx=1f-lx;val ty=if(calEdge).10f else .16f;val by=1f-ty
        val points=if(dual)listOf(.50f to .50f,lx to by,rx to by,lx to ty,rx to ty) else listOf(.50f to .50f,.50f to by,.50f to ty)
        val point=points[stage];val size=(48*resources.displayMetrics.density).toInt()
        target?.layoutParams=FrameLayout.LayoutParams(size,size).apply{leftMargin=(panel.width*point.first-size/2).toInt().coerceAtLeast(0);topMargin=(panel.height*point.second-size/2).toInt().coerceAtLeast(0)}
        prompt?.translationY=if(stage==0)100f else 0f
    }
    private fun abort(s:String){calibrating=false;Session.enabled=false;home();toast(s)}
    private fun save(c:Calibration){val k="r"+c.rotation+(if(c.dual)"d" else "s")+(if(calEdge)"e" else "n");prefs.edit().putFloat(k+"center",c.center).putFloat(k+"top",c.top).putFloat(k+"bottom",c.bottomEar).putFloat(k+"open",c.openEar).putFloat(k+"lt",c.leftTop).putFloat(k+"rt",c.rightTop).putFloat(k+"lb",c.leftBottom).putFloat(k+"rb",c.rightBottom).apply()}
    private fun load():Calibration?{val rot=rotation(this);val d=if(::layouts.isInitialized)layouts.selectedItemPosition==1 else false;val k="r"+rot+(if(d)"d" else "s")+(if(edgeBox.isChecked)"e" else "n");if(!prefs.contains(k+"center"))return null
        return Calibration(rot,prefs.getFloat(k+"center",0f),prefs.getFloat(k+"top",0f),prefs.getFloat(k+"bottom",0f),prefs.getFloat(k+"open",0f),d,prefs.getFloat(k+"lt",0f),prefs.getFloat(k+"rt",0f),prefs.getFloat(k+"lb",0f),prefs.getFloat(k+"rb",0f),edgeBox.isChecked).takeIf{it.good()}}
    override fun onRequestPermissionsResult(r:Int,p:Array<out String>,g:IntArray){super.onRequestPermissionsResult(r,p,g);if(r==10&&g.firstOrNull()==PackageManager.PERMISSION_GRANTED)startForegroundService(Intent(this,TrackingService::class.java))}
    override fun onPause(){if(calibrating){calibrating=false;Session.enabled=false;home()};super.onPause()}
    override fun onDestroy(){h.removeCallbacksAndMessages(null);super.onDestroy()}
}
