package dev.air2023.gazeg3
import org.junit.Test
import org.junit.Assert.*
class ReaderAppsTest{
 @Test fun readers(){assertTrue(ReaderApps.isReader("com.dragon.read","番茄免费小说"));assertTrue(ReaderApps.permitted("com.huawei.books","华为阅读",true));assertTrue(ReaderApps.permitted("com.huawei.android.reader","华为阅读",true))}
 @Test fun exclusions(){assertFalse(ReaderApps.permitted("com.android.settings","阅读",true));assertFalse(ReaderApps.permitted("dev.air2023.gazeg2","眼动阅读",false));assertFalse(ReaderApps.permitted("com.huawei.wallet","钱包",true));assertFalse(ReaderApps.permitted("com.tencent.mm","微信",false))}
 @Test fun others(){assertTrue(ReaderApps.permitted("org.example.newbook","自定义电子书",false));assertFalse(ReaderApps.permitted("com.huawei.somecore","核心服务",true))}
}