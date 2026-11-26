package dev.air2023.gazeg2

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.*
import android.content.*
import android.graphics.Path
import android.graphics.Rect
import android.graphics.PixelFormat
import android.view.Gravity
import android.view.View
import android.widget.TextView
import android.graphics.Color
import android.util.Log
import android.os.*
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo
import androidx.lifecycle.LifecycleService

object Session {
    @Volatile var latest:EyeFrame?=null
    @Volatile var status="已停止"
    @Volatile var running=false
    @Volatile var enabled=false
    @Volatile var count=0
    @Volatile var target=""
    @Volatile var gesture=0
    @Volatile var calibration:Calibration?=null
    @Volatile var dwell=550L
    @Volatile var diagnostic="未开始"
    @Volatile var testUntil=0L
    @Volatile var testAfter=0L
    @Volatile var testResult="未测试"
    @Volatile var overlay=true
    @Volatile var frameCount=0
    @Volatile var validCount=0
    @Volatile var triggerCount=0
    @Volatile var lastAction="尚未触发"
    @Volatile var feedbackUntil=0L
    @Volatile var feedbackTarget=""
    fun feedback(message:String){lastAction=message;feedbackTarget=target;feedbackUntil=SystemClock.elapsedRealtime()+2400;note("FEEDBACK "+message)}
    fun clearFeedback(){feedbackUntil=0;feedbackTarget=""}
    private val history=java.util.ArrayDeque<String>()
    @Synchronized fun note(s:String){if(history.peekLast()?.endsWith(" "+s)==true)return;if(history.size>=60)history.removeFirst();history.addLast(SystemClock.elapsedRealtime().toString()+" "+s);Log.i("Air2023GazeG2",s)}
    @Synchronized fun report():String=history.joinToString("\n")
}
fun rotation(c:Context):Int = (c.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation

class FlipService:AccessibilityService(){
    companion object {var current:FlipService?=null;private set}
    private var foreground="";private var busy=false;private var hint:TextView?=null
    private var hintText=""
    var targetIsActive=false;private set
    override fun onServiceConnected(){super.onServiceConnected();current=this;Session.note("ACCESS_CONNECTED")}
    override fun onAccessibilityEvent(e:AccessibilityEvent?){
        if(e?.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)foreground=e.packageName?.toString()?:""
    }
    override fun onInterrupt(){Session.clearFeedback();Session.enabled=false;Session.testUntil=0;hideHint()}
    override fun onDestroy(){if(current===this)current=null;Session.clearFeedback();Session.enabled=false;Session.testUntil=0;hideHint();super.onDestroy()}
    private fun fail(msg:String):Rect?{Session.diagnostic=msg;return null}
    // Use the actual active root, not cached window-state events (Huawei can report overlays).
    fun targetBounds(pkg:String):Rect?{
        targetIsActive=false
        if(pkg.isBlank())return fail("未指定应用")
        if(getSystemService(KeyguardManager::class.java).isKeyguardLocked)return fail("锁屏")
        val root=rootInActiveWindow?:return fail("无活动窗口根节点")
        val actual:String;val id:Int;val rb=Rect()
        try{actual=root.packageName?.toString()?:"";id=root.windowId;root.getBoundsInScreen(rb)}finally{root.recycle()}
        if(actual!=pkg)return fail("前台="+actual+"，目标="+pkg)
        targetIsActive=true
        val screen=(getSystemService(WINDOW_SERVICE) as WindowManager).maximumWindowMetrics.bounds
        val ws=windows
        try{
            if(ws.any{it.type==AccessibilityWindowInfo.TYPE_INPUT_METHOD})return fail("输入法可见")
            val active=ws.firstOrNull{it.id==id&&it.type==AccessibilityWindowInfo.TYPE_APPLICATION}
                ?:return fail("未找到目标应用窗口 ID="+id)
            val b=Rect();active.getBoundsInScreen(b)
            if(!b.intersect(screen)||b.width()<screen.width()*0.90||b.height()<screen.height()*0.82)return fail("目标非全屏 "+b.width()+"x"+b.height())
            // Other visible substantial app windows may mean split screen or a dialog.
            for(w in ws){
                if(w.id==id||w.type!=AccessibilityWindowInfo.TYPE_APPLICATION)continue
                val other=Rect();w.getBoundsInScreen(other)
                if(w.isActive||w.isFocused)return fail("其他应用窗口活动")
                if(other.intersect(screen)&&other.width()*other.height()>screen.width()*screen.height()*0.20f&&w.layer>active.layer)return fail("上层应用窗口")
            }
            Session.diagnostic="目标窗口通过 "+b.width()+"x"+b.height()
            return b
        }finally{ws.forEach{it.recycle()}}
    }
    fun allowed(pkg:String)=targetBounds(pkg)!=null
    fun showHint(s:String){
        if(!Session.overlay||s.isBlank()){hideHint();return}
        if(s==hintText&&hint!=null)return
        try{
            if(hint==null){
                val v=TextView(this).apply{setTextColor(Color.WHITE);textSize=14f;gravity=Gravity.CENTER;maxLines=2;setPadding((14*resources.displayMetrics.density).toInt(),(7*resources.displayMetrics.density).toInt(),(14*resources.displayMetrics.density).toInt(),(7*resources.displayMetrics.density).toInt());background=android.graphics.drawable.GradientDrawable().apply{cornerRadius=18*resources.displayMetrics.density;setColor(0xe6222529.toInt())}}
                val lp=WindowManager.LayoutParams(-2,-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,PixelFormat.TRANSLUCENT)
                lp.gravity=Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL;lp.y=(36*resources.displayMetrics.density).toInt()
                (getSystemService(WINDOW_SERVICE) as WindowManager).addView(v,lp);hint=v
            }
            hint?.text=s;hint?.setTextColor(when{ s.contains("手势已完成")->Color.rgb(166,244,188);s.contains("失败")||s.contains("暂停")||s.contains("拒绝")||s.contains("取消")->Color.rgb(255,198,146);s.contains("已读完")||s.contains("抬眼")||s.contains("已触发")->Color.rgb(255,234,155);else->Color.WHITE});hintText=s
        }catch(e:Exception){Session.note("OVERLAY_FAILED "+e.javaClass.simpleName)}
    }
    fun hideHint(){hint?.let{try{(getSystemService(WINDOW_SERVICE) as WindowManager).removeView(it)}catch(_:Exception){}};hint=null;hintText=""}
    fun flip(pkg:String,kind:Int,test:Boolean=false):Boolean{
        if(busy||(!Session.enabled&&!test)){Session.feedback("手势未发送：忙碌或已暂停");return false}
        val b=targetBounds(pkg)?:run{Session.feedback("手势未发送："+Session.diagnostic);return false}
        if(test)Session.testResult="测试手势已请求"
        fun x(f:Float)=b.left+b.width()*f
        fun y(f:Float)=b.top+b.height()*f
        val p=Path()
        when(kind){
            1->{p.moveTo(x(.30f),y(.50f));p.lineTo(x(.70f),y(.50f))}
            2->{p.moveTo(x(.50f),y(.75f));p.lineTo(x(.50f),y(.35f))}
            3->{p.moveTo(x(.50f),y(.35f));p.lineTo(x(.50f),y(.75f))}
            4->p.moveTo(x(.85f),y(.50f))
            else->{p.moveTo(x(.75f),y(.50f));p.lineTo(x(.35f),y(.50f))}
        }
        busy=true
        Session.feedback(if(test)"测试：正在发送翻页手势…" else "已触发 · 正在发送翻页手势…")
        val ok=try{dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,if(kind==4)80L else 320L)).build(),object:GestureResultCallback(){
            override fun onCompleted(g:GestureDescription?){busy=false;Session.count++;if(test)Session.testResult="系统确认手势完成，请核对阅读页是否变化";if(Session.running&&Session.target==pkg&&(Session.enabled||test))Session.feedback("手势已完成 · 第 "+Session.count+" 次");Session.note("GESTURE_COMPLETED test="+test)}
            override fun onCancelled(g:GestureDescription?){busy=false;if(test)Session.testResult="系统取消测试手势";if(Session.running&&Session.target==pkg&&(Session.enabled||test))Session.feedback("手势已取消");Session.note("GESTURE_CANCELLED")}
        },null)}catch(e:Exception){Session.note("GESTURE_EXCEPTION "+e.javaClass.simpleName);false}
        if(!ok){busy=false;if(test)Session.testResult="系统拒绝测试手势";Session.feedback("系统拒绝翻页手势");Session.note("GESTURE_REJECTED")}
        return ok
    }
}
class TrackingService:LifecycleService(){
    private val main=Handler(Looper.getMainLooper());private var pipe:CameraPipe?=null
    private var rot=0;private var gate:Gate?=null;private var cfg:Calibration?=null;private var dw=0L;private var start=0L
    private val receiver=object:BroadcastReceiver(){override fun onReceive(c:Context?,i:Intent?){stopNow("锁屏已停止")}}
    private val tick=object:Runnable{override fun run(){
        if(!Session.running)return
        val now=SystemClock.elapsedRealtime()
        if(rotation(this@TrackingService)!=rot){stopNow("方向变化，请重新启动及校准");return}
        if(now-start>45*60*1000L){stopNow("45分钟自动停止");return}
        if(getSystemService(PowerManager::class.java).currentThermalStatus>=PowerManager.THERMAL_STATUS_SEVERE){stopNow("温度过高，已停止");return}
        val f=Session.latest;val c=Session.calibration;val acc=FlipService.current
        val feedback=Session.feedbackUntil>now&&Session.feedbackTarget==Session.target
        val watching=Session.enabled||Session.testUntil>0||feedback
        val allowed=watching&&acc!=null&&acc.allowed(Session.target)
        if(Session.testUntil>0){
            if(now>Session.testUntil){Session.testUntil=0;Session.testResult="测试超时："+Session.diagnostic;Session.feedback(Session.testResult);Session.note("TEST_TIMEOUT "+Session.diagnostic)}
            else if(now>=Session.testAfter&&allowed){Session.testUntil=0;if(acc?.flip(Session.target,Session.gesture,true)!=true)Session.testResult=Session.lastAction}
        }
        if(!Session.enabled||c==null){gate=null;Session.status=if(Session.testUntil>0)"等待进入阅读应用 · 手势测试" else if(f?.valid==true&&now-f.time<500)"相机已就绪 · 校准/待启动" else "等待单张清晰人脸"}
        else if(!allowed){gate=null;Session.status="暂停 · "+(if(acc==null)"无障碍未连接" else Session.diagnostic)}
        else if(f==null||now-f.time>500){gate=null;Session.status="暂停 · 相机没有新画面"}
        else{
            if(gate==null||cfg!=c||dw!=Session.dwell){cfg=c;dw=Session.dwell;gate=Gate(c,dw)}
            if(gate!!.sample(f,now)){Session.triggerCount++;Session.note("GAZE_TRIGGER");Session.feedback("已触发翻页");acc?.flip(Session.target,Session.gesture)}
            Session.status=if(f.valid)gate!!.state else "暂停 · 未检测到有效人脸"
        }
        // Non-touchable text stays visible for a real duration, including a manual test.
        val hold=Session.feedbackUntil>now&&Session.feedbackTarget==Session.target
        if(acc!=null){
            if(acc.targetIsActive&&(Session.enabled||Session.testUntil>0||hold)){
                acc.showHint(if(hold)Session.lastAction else Session.status)
            }else acc.hideHint()
        }
        if(watching)Session.note(Session.status+" | "+Session.diagnostic)
        main.postDelayed(this,120)
    }}
    override fun onCreate(){super.onCreate();registerReceiver(receiver,IntentFilter(Intent.ACTION_SCREEN_OFF))}
    override fun onStartCommand(intent:Intent?,flags:Int,startId:Int):Int{
        super.onStartCommand(intent,flags,startId)
        if(intent?.action=="stop"){stopNow("已停止");return START_NOT_STICKY}
        if(Session.running)return START_NOT_STICKY
        if(Build.MODEL!="DBY2-W00"||Build.VERSION.SDK_INT!=31){stopNow("设备不匹配");return START_NOT_STICKY}
        try{
            val nm=getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(NotificationChannel("gaze","眼动翻页",NotificationManager.IMPORTANCE_LOW))
            val stop=PendingIntent.getService(this,1,Intent(this,TrackingService::class.java).setAction("stop"),PendingIntent.FLAG_IMMUTABLE)
            val open=PendingIntent.getActivity(this,2,Intent(this,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE)
            startForeground(2023,Notification.Builder(this,"gaze").setSmallIcon(android.R.drawable.ic_menu_view).setContentTitle("Air2023 眼动翻页 · 相机运行").setContentText("点击返回控制页，或停止相机").setContentIntent(open).addAction(0,"停止",stop).setOngoing(true).build(),android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
            rot=rotation(this);start=SystemClock.elapsedRealtime();Session.latest=null;Session.running=true;Session.enabled=false;Session.status="正在打开前置摄像头"
            Session.frameCount=0;Session.validCount=0;Session.triggerCount=0;Session.count=0;Session.lastAction="尚未触发";Session.clearFeedback()
            pipe=CameraPipe(this,this,rot,{Session.latest=it;Session.frameCount++;if(it.valid)Session.validCount++},{msg->main.post{stopNow(msg)}}).also{it.start()}
            main.post(tick)
        }catch(e:Throwable){stopNow("启动失败："+e.javaClass.simpleName)}
        return START_NOT_STICKY
    }
    private fun stopNow(msg:String){Session.note(msg);Session.clearFeedback();Session.testUntil=0;FlipService.current?.hideHint();Session.enabled=false;Session.running=false;Session.latest=null;Session.status=msg;main.removeCallbacks(tick);pipe?.stop();pipe=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
    override fun onDestroy(){Session.clearFeedback();Session.testUntil=0;FlipService.current?.hideHint();Session.enabled=false;Session.running=false;Session.latest=null;main.removeCallbacksAndMessages(null);pipe?.stop();pipe=null;try{unregisterReceiver(receiver)}catch(_:Exception){};super.onDestroy()}
}
