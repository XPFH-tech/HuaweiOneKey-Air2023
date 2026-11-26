package dev.air2023.gazeg1

import kotlin.math.abs
import kotlin.math.hypot
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

data class EyeFrame(val time:Long,val rotation:Int,val iris:Float,val ear:Float,val valid:Boolean)
data class Calibration(val rotation:Int,val center:Float,val top:Float,val bottomEar:Float,val openEar:Float) {
    val sign get()=if(top<center) 1f else -1f
    val topGate get()=(center+0.62f*(top-center))*sign
    val bottomGate get()=bottomEar+0.30f*(openEar-bottomEar)
    val blinkGate get()=bottomEar*0.60f
    fun good()=listOf(center,top,bottomEar,openEar).all { it.isFinite() } &&
       abs(top-center)>=0.012f && bottomEar>=0.035f && openEar-bottomEar>=0.025f && openEar<0.9f
}
object Eyes {
    // GazeUp's iris/EAR concept, reimplemented with image aspect ratio and roll correction.
    fun extract(l:List<NormalizedLandmark>,w:Int,h:Int,t:Long,r:Int):EyeFrame {
        fun bad()=EyeFrame(t,r,0f,0f,false)
        if(l.size<478)return bad()
        fun x(i:Int)=l[i].x()*w
        fun y(i:Int)=l[i].y()*h
        fun eye(a:Int,b:Int,u:Int,d:Int,iris:Int):Pair<Float,Float>? {
            val dx=x(b)-x(a);val dy=y(b)-y(a);val width=hypot(dx,dy)
            if(width<w*0.025f||width>w*0.45f)return null
            val nx=-dy/width;val ny=dx/width
            // Normal vector points down in upright image coordinates.
            val sign=if(ny>=0)1f else -1f
            val ix=(iris..iris+4).map { x(it) }.average().toFloat()
            val iy=(iris..iris+4).map { y(it) }.average().toFloat()
            val offset=((ix-(x(a)+x(b))/2)*nx+(iy-(y(a)+y(b))/2)*ny)*sign/width
            val ear=abs((x(d)-x(u))*nx+(y(d)-y(u))*ny)/width
            return offset to ear
        }
        val a=eye(33,133,159,145,468)?:return bad()
        val b=eye(362,263,386,374,473)?:return bad()
        val score=(a.first+b.first)/2;val ear=(a.second+b.second)/2
        if(!score.isFinite()||!ear.isFinite()||abs(a.first-b.first)>0.17f||abs(a.second-b.second)>0.18f||ear<0.025f||ear>0.9f)return bad()
        return EyeFrame(t,r,score,ear,true)
    }
}
class Gate(private val c:Calibration,private val dwell:Long) {
    var state="阅读中";private set
    private var bottom=-1L;private var top=-1L;private var armed=-1L;private var cool=0L;private var last=-1L
    private var iris=0f;private var ear=0f;private var initialized=false
    fun reset(){bottom=-1;top=-1;armed=-1;last=-1;initialized=false;state="阅读中"}
    fun sample(f:EyeFrame,now:Long):Boolean {
        if(!f.valid||f.rotation!=c.rotation||now-f.time !in 0..500||f.ear<c.blinkGate){reset();return false}
        if(f.time<=last)return false
        if(last>=0&&f.time-last>500)reset()
        last=f.time
        if(now<cool){state="冷却";return false}
        if(!initialized){iris=f.iris*c.sign;ear=f.ear;initialized=true}else{iris=0.35f*f.iris*c.sign+0.65f*iris;ear=0.30f*f.ear+0.70f*ear}
        if(armed>=0&&now-armed>6500){reset();return false}
        if(armed<0){
            state="阅读中"
            if(ear<=c.bottomGate&&iris>c.topGate){if(bottom<0)bottom=now;if(now-bottom>=dwell){armed=now;state="已读到底部 · 抬眼翻页"}}else bottom=-1
        }else{
            state="已读到底部 · 抬眼翻页"
            if(iris<=c.topGate&&ear>c.bottomGate){if(top<0)top=now;if(now-top>=dwell){reset();cool=now+1800;state="冷却";return true}}else top=-1
        }
        return false
    }
}
