package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

/** Native transfer-market overview backed by the live PFM core. */
public final class NativeTransfers {
    private NativeTransfers() {}
    private static boolean routing;
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float size,boolean bold){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private static Class<?> bridge() throws Exception{return Class.forName("PfmTransferBridge");}
    private static boolean bool(String name){try{return Boolean.TRUE.equals(bridge().getMethod(name).invoke(null));}catch(Throwable ignored){return false;}}
    private static int integer(String name){try{return ((Integer)bridge().getMethod(name).invoke(null)).intValue();}catch(Throwable ignored){return 0;}}
    private static String money(int v){return NumberFormat.getIntegerInstance(Locale.US).format(v)+" $";}

    private static final class PlayerRow {
        int id,spe,res,qua,mor,age,price;String name="",club="",pos="?";
        static PlayerRow parse(String s){PlayerRow r=new PlayerRow();try{String[] p=s.split("\\t",-1);r.id=Integer.parseInt(p[0]);r.name=p[1];r.club=p[2];r.pos=p[3];r.spe=Integer.parseInt(p[4]);r.res=Integer.parseInt(p[5]);r.qua=Integer.parseInt(p[6]);r.mor=Integer.parseInt(p[7]);r.age=Integer.parseInt(p[8]);r.price=Integer.parseInt(p[9]);}catch(Throwable ignored){r.name=s;}return r;}
        int rating(){return (spe+res+qua)/3;}
    }
    private static ArrayList<PlayerRow> rows(){ArrayList<PlayerRow> out=new ArrayList<PlayerRow>();try{String[] values=(String[])bridge().getMethod("marketRows").invoke(null);if(values!=null)for(String s:values)out.add(PlayerRow.parse(s));}catch(Throwable ignored){}return out;}
    private static boolean open(int screen){try{Method m=bridge().getMethod("openLegacy",Integer.TYPE);return Boolean.TRUE.equals(m.invoke(null,Integer.valueOf(screen)));}catch(Throwable ignored){return false;}}
    private static void details(Activity a,PlayerRow p){String body="Club: "+p.club+"\nPosition: "+p.pos+"   Age: "+p.age+"\n\nSPE  "+p.spe+"\nRES  "+p.res+"\nQUA  "+p.qua+"\nMOR  "+p.mor+"\n\nOverall: "+p.rating()+"\nAsking price: "+money(p.price)+"\n\nUse BUY / MARKET to complete the transaction through the original game rules.";new AlertDialog.Builder(a).setTitle(p.name).setMessage(body).setPositiveButton("Close",null).show();}
    private static void route(Activity a,AlertDialog dialog,int screen,boolean pauseToken){
        if(routing)return;
        routing=true;
        dialog.setOnDismissListener(v->{
            NativePause.end(pauseToken);
            a.getWindow().getDecorView().postDelayed(()->{
                try{
                    if(!open(screen))Toast.makeText(a,"Could not open the legacy transfer screen",Toast.LENGTH_SHORT).show();
                    else Toast.makeText(a,"Selected in New players — press the green OK button",Toast.LENGTH_LONG).show();
                }
                finally{routing=false;}
            },180L);
        });
        dialog.dismiss();
    }

    public static void show(Activity a){
        if(!bool("available")){Toast.makeText(a,"Start or load a career first",Toast.LENGTH_SHORT).show();return;}
        final boolean pauseToken=NativePause.begin();
        final ArrayList<PlayerRow> market=rows();
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,8),dp(a,2),dp(a,8),0);
        boolean window=bool("transferOpen");TextView status=text(a,"Budget: "+money(integer("budget"))+"   •   Window: "+(window?"OPEN":"CLOSED")+"   •   Listed: "+market.size(),14f,true);status.setPadding(dp(a,7),dp(a,3),dp(a,7),dp(a,5));status.setBackgroundColor(window?0xffdff1df:0xffffe3cf);root.addView(status);
        TextView hint=text(a,"Tap a player for details. Buy, Search and Sell safely select the matching original menu row; press the green OK button once to enter it.",11.5f,false);hint.setPadding(dp(a,7),dp(a,4),dp(a,7),dp(a,5));root.addView(hint);
        LinearLayout actions=new LinearLayout(a);actions.setOrientation(LinearLayout.HORIZONTAL);Button buy=new Button(a);buy.setText("BUY → OK");actions.addView(buy,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));Button search=new Button(a);search.setText("SEARCH → OK");actions.addView(search,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));Button sell=new Button(a);sell.setText("SELL → OK");actions.addView(sell,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));root.addView(actions);
        if(market.isEmpty()){TextView empty=text(a,"No players are currently listed. AI clubs populate the market as career rounds and off-season market ticks advance.",14f,false);empty.setGravity(android.view.Gravity.CENTER);root.addView(empty,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));}
        else{
            ListView list=new ListView(a);list.setDividerHeight(1);
            ArrayAdapter<PlayerRow> adapter=new ArrayAdapter<PlayerRow>(a,android.R.layout.simple_list_item_1,market){@Override public View getView(int pos,View cv,ViewGroup parent){LinearLayout row;TextView l1,l2;if(cv instanceof LinearLayout&&((LinearLayout)cv).getChildCount()==2){row=(LinearLayout)cv;l1=(TextView)row.getChildAt(0);l2=(TextView)row.getChildAt(1);}else{row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,9),dp(a,5),dp(a,9),dp(a,5));l1=text(a,"",14f,true);l2=text(a,"",11.5f,false);row.addView(l1);row.addView(l2);}PlayerRow p=getItem(pos);l1.setText(p.name+"   "+p.pos+"   "+money(p.price));l2.setText(p.club+"   •   OVR "+p.rating()+"   SPE "+p.spe+"  RES "+p.res+"  QUA "+p.qua+"  MOR "+p.mor+"   AGE "+p.age);row.setBackgroundColor(p.price<=integer("budget")?Color.TRANSPARENT:0xffffeeee);return row;}};
            list.setAdapter(adapter);list.setOnItemClickListener((p,v,pos,id)->details(a,market.get(pos)));root.addView(list,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));
        }
        final AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Transfers / Market").setView(root).setNegativeButton("Close",null).create();buy.setOnClickListener(v->route(a,dialog,33,pauseToken));search.setOnClickListener(v->route(a,dialog,41,pauseToken));sell.setOnClickListener(v->route(a,dialog,34,pauseToken));dialog.setOnDismissListener(v->{NativePause.end(pauseToken);routing=false;});dialog.setOnShowListener(v->{Window w=dialog.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*0.94f));});dialog.show();
    }
}
