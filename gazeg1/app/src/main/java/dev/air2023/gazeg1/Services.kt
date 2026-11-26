package dev.air2023.gazeg1

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.app.*
import android.content.*
import android.graphics.Path
import android.graphics.Rect
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
}
fun rotation(c:Context):Int = (c.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.rotation

class FlipService:AccessibilityService(){
    companion object {var current:FlipService?=null;private set}
    private var foreground="";private var busy=false
    override fun onServiceConnected(){current=this}
    override fun onAccessibilityEvent(e:AccessibilityEvent?){
        if(e?.eventType==AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED)foreground=e.packageName?.toString()?:""
    }
    override fun onInterrupt(){Session.enabled=false}
    override fun onDestroy(){if(current===this)current=null;Session.enabled=false;super.onDestroy()}
    fun allowed(pkg:String):Boolean{
        if(pkg.isBlank()||foreground!=pkg||getSystemService(KeyguardManager::class.java).isKeyguardLocked)return false
        val root=rootInActiveWindow?:return false
        val actual=try{root.packageName?.toString()}finally{root.recycle()}
        if(actual!=pkg)return false
        val wm=getSystemService(WINDOW_SERVICE) as WindowManager
        val screen=wm.maximumWindowMetrics.bounds
        val ws=windows ?:return false
        try{
            val apps=ws.filter{it.type==AccessibilityWindowInfo.TYPE_APPLICATION}
            if(apps.size!=1)return false
            val active=apps[0]
            val b=Rect();active.getBoundsInScreen(b)
            if(!active.isActive||b.width()<screen.width()*0.90||b.height()<screen.height()*0.85)return false
            if(ws.any{it.type==AccessibilityWindowInfo.TYPE_INPUT_METHOD})return false
            return true
        }finally{ws.forEach{it.recycle()}}
    }
    fun flip(pkg:String,kind:Int):Boolean{
        if(busy||!Session.enabled||!allowed(pkg))return false
        val b=(getSystemService(WINDOW_SERVICE) as WindowManager).maximumWindowMetrics.bounds
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
        val ok=dispatchGesture(GestureDescription.Builder().addStroke(GestureDescription.StrokeDescription(p,0,if(kind==4)80L else 320L)).build(),object:GestureResultCallback(){
            override fun onCompleted(g:GestureDescription?){busy=false;Session.count++}
            override fun onCancelled(g:GestureDescription?){busy=false}
        },null)
        if(!ok)busy=false
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
        if(!Session.enabled||c==null){gate=null;Session.status=if(f?.valid==true&&now-f.time<500)"相机已就绪 · 校准/待启动" else "等待单张清晰人脸"}
        else if(acc==null||!acc.allowed(Session.target)){gate=null;Session.status="暂停 · 不在指定全屏阅读应用"}
        else if(f==null||now-f.time>500){gate=null;Session.status="暂停 · 无新鲜图像"}
        else{
            if(gate==null||cfg!=c||dw!=Session.dwell){cfg=c;dw=Session.dwell;gate=Gate(c,dw)}
            if(gate!!.sample(f,now))acc.flip(Session.target,Session.gesture)
            Session.status=gate!!.state
        }
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
            pipe=CameraPipe(this,this,rot,{Session.latest=it},{msg->main.post{stopNow(msg)}}).also{it.start()}
            main.post(tick)
        }catch(e:Throwable){stopNow("启动失败："+e.javaClass.simpleName)}
        return START_NOT_STICKY
    }
    private fun stopNow(msg:String){Session.enabled=false;Session.running=false;Session.latest=null;Session.status=msg;main.removeCallbacks(tick);pipe?.stop();pipe=null;stopForeground(STOP_FOREGROUND_REMOVE);stopSelf()}
    override fun onDestroy(){Session.enabled=false;Session.running=false;Session.latest=null;main.removeCallbacksAndMessages(null);pipe?.stop();pipe=null;try{unregisterReceiver(receiver)}catch(_:Exception){};super.onDestroy()}
}
