package pfm.android;

/** Legacy matchday hook that delegates market activity to the native transaction engine. */
public final class NativeMarketTick {
    private NativeMarketTick() {}
    public static void run(){
        try{
            android.content.Context c=AndroidRuntime.context();
            NativeWorldCenter.load(c);
            NativeAiMarket.run(c,true);
        }catch(Throwable ignored){}
    }
}
