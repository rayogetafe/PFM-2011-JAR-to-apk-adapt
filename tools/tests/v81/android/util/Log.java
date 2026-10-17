package android.util;
/** Compile-only fixture for the adapter's diagnostic logger. */
public final class Log {public static int e(String tag,String text,Throwable cause){cause.printStackTrace();return 0;}}
