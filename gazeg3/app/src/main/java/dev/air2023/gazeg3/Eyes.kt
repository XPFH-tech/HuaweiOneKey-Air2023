package dev.air2023.gazeg3

import kotlin.math.abs
import kotlin.math.hypot
import com.google.mediapipe.tasks.components.containers.NormalizedLandmark

data class EyeFrame(val time:Long,val rotation:Int,val iris:Float,val ear:Float,val valid:Boolean,val horizontal:Float=0f)
data class Calibration(val rotation:Int,val center:Float,val top:Float,val bottomEar:Float,val openEar:Float,val dual:Boolean=false,val leftTop:Float=0f,val rightTop:Float=0f,val leftBottom:Float=0f,val rightBottom:Float=0f,val edge:Boolean=false) {
    val sign get()=if(top<center) 1f else -1f
    val topGate get()=(center+(if(edge)0.52f else 0.62f)*(top-center))*sign
    val bottomGate get()=bottomEar+(if(edge)0.38f else 0.30f)*(openEar-bottomEar)
    val blinkGate get()=bottomEar*0.60f
    val horizontalSign get()=if(rightTop>leftTop)1f else -1f
    val rightGate get()=(leftBottom+(if(edge)0.60f else 0.65f)*(rightBottom-leftBottom))*horizontalSign
    val leftGate get()=(rightTop+(if(edge)0.60f else 0.65f)*(leftTop-rightTop))*horizontalSign
    fun good()=listOf(center,top,bottomEar,openEar).all { it.isFinite() } &&
       abs(top-center)>=0.012f && bottomEar>=0.035f && openEar-bottomEar>=0.025f && openEar<0.9f && (!dual || (listOf(leftTop,rightTop,leftBottom,rightBottom).all{it.isFinite()} && abs(rightTop-leftTop)>=0.025f && abs(rightBottom-leftBottom)>=0.025f && (rightTop-leftTop)*(rightBottom-leftBottom)>0f))
}
object Eyes {
    // GazeUp's iris/EAR concept, reimplemented with image aspect ratio and roll correction.
    fun extract(l:List<NormalizedLandmark>,w:Int,h:Int,t:Long,r:Int):EyeFrame {
        fun bad()=EyeFrame(t,r,0f,0f,false)
        if(l.size<478)return bad()
        fun x(i:Int)=l[i].x()*w
        fun y(i:Int)=l[i].y()*h
        fun eye(a:Int,b:Int,u:Int,d:Int,iris:Int):Triple<Float,Float,Float>? {
            val dx=x(b)-x(a);val dy=y(b)-y(a);val width=hypot(dx,dy)
            if(width<w*0.025f||width>w*0.45f)return null
            val nx=-dy/width;val ny=dx/width
            // Normal vector points down in upright image coordinates.
            val sign=if(ny>=0)1f else -1f
            val ix=(iris..iris+4).map { x(it) }.average().toFloat()
            val iy=(iris..iris+4).map { y(it) }.average().toFloat()
            val offset=((ix-(x(a)+x(b))/2)*nx+(iy-(y(a)+y(b))/2)*ny)*sign/width
            val ear=abs((x(d)-x(u))*nx+(y(d)-y(u))*ny)/width
            val horiz=((ix-(x(a)+x(b))/2)*dx+(iy-(y(a)+y(b))/2)*dy)*(if(dx>=0)1f else -1f)/(width*width)
            return Triple(offset,ear,horiz)
        }
        val a=eye(33,133,159,145,468)?:return bad()
        val b=eye(362,263,386,374,473)?:return bad()
        val score=(a.first+b.first)/2;val ear=(a.second+b.second)/2
        if(!score.isFinite()||!ear.isFinite()||abs(a.first-b.first)>0.17f||abs(a.second-b.second)>0.18f||ear<0.025f||ear>0.9f)return bad()
        val horizontal=(a.third+b.third)/2
        if(!horizontal.isFinite()||abs(a.third-b.third)>0.20f)return bad()
        return EyeFrame(t,r,score,ear,true,horizontal)
    }
}
class Gate(private val c:Calibration,private val dwell:Long) {
    var state="阅读中";private set
    private var bottom=-1L;private var top=-1L;private var armed=-1L;private var cool=0L;private var last=-1L
    private var iris=0f;private var ear=0f;private var horizontal=0f;private var initialized=false
    fun reset(){bottom=-1;top=-1;armed=-1;last=-1;initialized=false;state="阅读中"}
    fun sample(f:EyeFrame,now:Long):Boolean {
        if(!f.valid||f.rotation!=c.rotation||now-f.time !in 0..500||f.ear<c.blinkGate){reset();return false}
        if(f.time<=last)return false
        if(last>=0&&f.time-last>500)reset()
        last=f.time
        if(now<cool){state="翻页冷却中";return false}
        if(!initialized){iris=f.iris*c.sign;ear=f.ear;horizontal=f.horizontal*c.horizontalSign;initialized=true}else{iris=0.35f*f.iris*c.sign+0.65f*iris;ear=0.30f*f.ear+0.70f*ear;horizontal=0.45f*f.horizontal*c.horizontalSign+0.55f*horizontal}
        if(armed>=0&&now-armed>6500){reset();return false}
        if(armed<0){
            state=if(c.dual)"阅读中 · 等待右页底部" else "阅读中"
            if(ear<=c.bottomGate&&iris>c.topGate&&(!c.dual||horizontal>=c.rightGate)){if(bottom<0)bottom=now;state=if(c.dual)"检测到右页底部 · 请停留" else "检测到页面底部 · 请停留";if(now-bottom>=dwell){armed=now;state=if(c.dual)"右页已读完 · 看向左上" else "已读到底部 · 抬眼翻页"}}else bottom=-1
        }else{
            state=if(c.dual)"右页已读完 · 看向左上" else "已读到底部 · 抬眼翻页"
            if(iris<=c.topGate&&f.ear>c.blinkGate&&(!c.dual||horizontal<=c.leftGate)){if(top<0)top=now;state=if(c.dual)"检测到左上 · 正在确认" else "检测到抬眼 · 正在确认";if(now-top>=dwell){reset();cool=now+1800;state="冷却";return true}}else top=-1
        }
        return false
    }
}
