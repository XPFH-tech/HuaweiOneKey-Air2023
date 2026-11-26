package dev.air2023.gazeg2
import org.junit.Test
import org.junit.Assert.*
class GateTest {
 private val c=Calibration(0,0f,-0.10f,0.12f,0.28f)
 @Test fun rejectsBadCalibration(){assertFalse(c.copy(top=0f).good());assertTrue(c.good())}
 @Test fun bottomThenTop(){val g=Gate(c,350);var flips=0
   for(t in 1000L..2000L step 120)if(g.sample(EyeFrame(t,0,0f,.12f,true),t))flips++
   for(t in 2100L..3300L step 120)if(g.sample(EyeFrame(t,0,-.10f,.28f,true),t))flips++
   assertEquals(1,flips)
 }
 @Test fun topAlone(){val g=Gate(c,350);for(t in 1000L..4000L step 100)assertFalse(g.sample(EyeFrame(t,0,-.1f,.28f,true),t))}
 @Test fun lossDisarms(){val g=Gate(c,350);for(t in 1000L..2000L step 100)g.sample(EyeFrame(t,0,0f,.12f,true),t);g.sample(EyeFrame(2100,0,0f,0f,false),2100);for(t in 2200L..4000L step 100)assertFalse(g.sample(EyeFrame(t,0,-.1f,.28f,true),t))}
 @Test fun doublePageLeftBottomDoesNotArm(){val d=c.copy(dual=true,leftTop=-.1f,rightTop=.1f,leftBottom=-.1f,rightBottom=.1f);assertTrue(d.good());val g=Gate(d,350)
   for(t in 1000L..2000L step 100)assertFalse(g.sample(EyeFrame(t,0,0f,.12f,true,-.10f),t))
   for(t in 2100L..3600L step 100)assertFalse(g.sample(EyeFrame(t,0,-.10f,.28f,true,.10f),t))
 }
 @Test fun doublePageRightBottomThenLeftTop(){val d=c.copy(dual=true,leftTop=-.1f,rightTop=.1f,leftBottom=-.1f,rightBottom=.1f);val g=Gate(d,350);var flips=0
   for(t in 1000L..2200L step 100)if(g.sample(EyeFrame(t,0,0f,.12f,true,.10f),t))flips++
   for(t in 2300L..3900L step 100)if(g.sample(EyeFrame(t,0,-.10f,.28f,true,-.10f),t))flips++
   assertEquals(1,flips)
 }
 @Test fun wrongRotation(){val g=Gate(c,350);for(t in 1000L..4000L step 100)assertFalse(g.sample(EyeFrame(t,1,0f,.12f,true),t))}
}
