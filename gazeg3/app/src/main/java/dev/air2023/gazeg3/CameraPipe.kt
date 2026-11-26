package dev.air2023.gazeg3

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import java.util.concurrent.Executors
import java.nio.ByteBuffer

class CameraPipe(private val ctx:Context,private val owner:LifecycleOwner,private val rotation:Int,private val frame:(EyeFrame)->Unit,private val error:(String)->Unit){
    private val worker=Executors.newSingleThreadExecutor()
    private var provider:ProcessCameraProvider?=null
    private var analysis:ImageAnalysis?=null
    private var model:FaceLandmarker?=null
    @Volatile private var closed=false
    private var last=0L
    fun start(){
        worker.execute {
            try{
                model=FaceLandmarker.createFromOptions(ctx,FaceLandmarker.FaceLandmarkerOptions.builder()
                    .setBaseOptions(BaseOptions.builder().setModelAssetPath("face_landmarker.task").build())
                    .setRunningMode(RunningMode.VIDEO).setNumFaces(2).build())
                if(closed)return@execute
                val future=ProcessCameraProvider.getInstance(ctx)
                future.addListener({
                    if(!closed)try{
                        provider=future.get()
                        val a=ImageAnalysis.Builder().setTargetResolution(Size(640,480)).setTargetRotation(rotation)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                        analysis=a
                        a.setAnalyzer(worker){analyze(it)}
                        provider!!.bindToLifecycle(owner,CameraSelector.DEFAULT_FRONT_CAMERA,a)
                    }catch(e:Exception){error("前置相机不可用："+e.javaClass.simpleName)}
                },ContextCompat.getMainExecutor(ctx))
            }catch(e:Throwable){if(!closed)error("模型初始化失败："+e.javaClass.simpleName)}
        }
    }
    private fun analyze(img:ImageProxy){
        var raw:Bitmap?=null;var rotated:Bitmap?=null
        try{
            val now=SystemClock.elapsedRealtime()
            if(closed||now-last<105)return
            last=now
            val p=img.planes[0];val w=img.width;val h=img.height
            if(p.pixelStride!=4||p.rowStride<w*4)throw IllegalStateException("RGBA stride")
            val packed=ByteBuffer.allocateDirect(w*h*4);val b=p.buffer.duplicate()
            for(y in 0 until h){val from=y*p.rowStride;if(from+w*4>b.capacity())throw IllegalStateException("RGBA row");b.limit(b.capacity());b.position(from);b.limit(from+w*4);packed.put(b)}
            packed.flip()
            val bmp=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);raw=bmp;bmp.copyPixelsFromBuffer(packed)
            val degrees=img.imageInfo.rotationDegrees
            val upright=if(degrees==0)bmp else Bitmap.createBitmap(bmp,0,0,w,h,Matrix().apply{postRotate(degrees.toFloat())},false)
            rotated=upright
            val mp=BitmapImageBuilder(upright).build()
            try{
                val result=model?.detectForVideo(mp,now)
                val faces=result?.faceLandmarks()
                val f=if(faces?.size==1)Eyes.extract(faces[0],upright.width,upright.height,now,rotation) else EyeFrame(now,rotation,0f,0f,false)
                if(!closed)frame(f)
            }finally{mp.close()}
        }catch(e:Throwable){if(!closed)error("相机分析失败："+e.javaClass.simpleName)}
        finally{if(rotated!=null&&rotated!==raw)rotated.recycle();raw?.recycle();img.close()}
    }
    fun stop(){
        closed=true
        analysis?.clearAnalyzer();analysis?.let{provider?.unbind(it)};analysis=null
        worker.execute{try{model?.close()}catch(_:Exception){};model=null}
        worker.shutdown()
    }
}
