package dev.air2023.gazeg3
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import java.text.Collator
import java.util.Locale

data class ReaderApp(val pkg:String,val title:String,val component:ComponentName,val recommended:Boolean){
    fun launchIntent()=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(component).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
object ReaderApps{
    private val known=setOf("com.tencent.weread","com.dragon.read","com.qidian.qdreader","io.legado.app.release","io.legado.app","com.huawei.books","com.huawei.reader","com.huawei.hwireader","com.chaozh.ireaderfree","com.qq.reader","com.baidu.yuedu","com.duokan.reader","com.shuqi.controller","com.amazon.kindle","org.readera","com.flyersoft.moonreader","com.flyersoft.moonreaderp","com.adobe.reader")
    private val words=listOf("阅读","读书","小说","书城","书旗","掌阅","起点","番茄","多看","漫画","电子书","书籍","reader","reading","kindle","ebook","pdf","epub","legado","manga")
    fun isReader(pkg:String,label:String)=pkg.lowercase(Locale.ROOT) in known||words.any{label.lowercase(Locale.ROOT).contains(it)}
    fun permitted(pkg:String,label:String,system:Boolean):Boolean{
        val id=pkg.lowercase(Locale.ROOT)
        if(id.startsWith("dev.air2023.")||id.startsWith("com.android.")||id=="android"||id.startsWith("com.topjohnwu.")||id.startsWith("org.lsposed.")||listOf("rootkit","magisk","zygisk","vector.manager","packageinstaller","permissioncontroller","systemui","launcher","settings","wallet","payment","bank","alipay").any{id.contains(it)}||id=="com.tencent.mm"||id=="com.tencent.mobileqq")return false
        return if(system||id.startsWith("com.huawei.android."))isReader(pkg,label) else true
    }
    fun list(c:Context):List<ReaderApp>{
        val pm=c.packageManager
        val q=Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val records=pm.queryIntentActivities(q,0).mapNotNull{ri->
            val ai=ri.activityInfo?:return@mapNotNull null
            val app=ai.applicationInfo?:return@mapNotNull null
            if(!ai.enabled||!ai.exported||!app.enabled)return@mapNotNull null
            val pkg=ai.packageName
            val title=app.loadLabel(pm).toString().ifBlank{ri.loadLabel(pm).toString().ifBlank{pkg}}
            val sys=app.flags and (ApplicationInfo.FLAG_SYSTEM or ApplicationInfo.FLAG_UPDATED_SYSTEM_APP)!=0
            if(!permitted(pkg,title,sys))return@mapNotNull null
            ReaderApp(pkg,title,ComponentName(pkg,ai.name),isReader(pkg,title))
        }.distinctBy{it.pkg}
        val col=Collator.getInstance(Locale.CHINA)
        return records.sortedWith(Comparator{a,b->if(a.recommended!=b.recommended){if(a.recommended)-1 else 1}else{val n=col.compare(a.title,b.title);if(n!=0)n else a.pkg.compareTo(b.pkg)}})
    }
}
