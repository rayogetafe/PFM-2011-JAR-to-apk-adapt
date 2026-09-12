package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import java.io.InputStream;
import java.util.ArrayList;

/** Native career archive with the same complete table presentation as the live season hub. */
public final class NativeHistory {
    private NativeHistory() {}
    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float z,boolean b){TextView v=new TextView(c);v.setText(s);v.setTextSize(z);if(b)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}
    private static Class<?> bridge()throws Exception{return Class.forName("PfmHistoryBridge");}
    private static Class<?> seasonBridge()throws Exception{return Class.forName("PfmSeasonBridge");}
    private static boolean available(){try{return Boolean.TRUE.equals(bridge().getMethod("available").invoke(null));}catch(Throwable t){return false;}}
    private static int current(){try{return ((Integer)bridge().getMethod("currentSeason").invoke(null)).intValue();}catch(Throwable t){return 2010;}}
    private static int[] seasons(){try{return (int[])bridge().getMethod("seasons").invoke(null);}catch(Throwable t){return new int[0];}}
    private static String league(){try{return String.valueOf(seasonBridge().getMethod("leagueCode").invoke(null));}catch(Throwable t){return "";}}
    private static String[] rows(String method,int season){try{return (String[])bridge().getMethod(method,Integer.TYPE).invoke(null,Integer.valueOf(season));}catch(Throwable t){return new String[0];}}

    private static final class TableRow {
        int rank,p,w,d,l,gf,ga,gd,pts,teamId=-1;String club="";boolean user;
        static TableRow parse(String s){TableRow r=new TableRow();try{String[] x=s.split("\\t",-1);r.rank=Integer.parseInt(x[0]);r.club=x[1];r.p=Integer.parseInt(x[2]);r.w=Integer.parseInt(x[3]);r.d=Integer.parseInt(x[4]);r.l=Integer.parseInt(x[5]);r.gf=Integer.parseInt(x[6]);r.ga=Integer.parseInt(x[7]);r.gd=Integer.parseInt(x[8]);r.pts=Integer.parseInt(x[9]);r.user="1".equals(x[10]);if(x.length>11)r.teamId=Integer.parseInt(x[11]);}catch(Throwable ignored){}return r;}
    }
    private static final class PlayerRow {String name="";int g,ap,st,rat,spe,res,qua;static PlayerRow parse(String s){PlayerRow r=new PlayerRow();try{String[] x=s.split("\\t",-1);r.name=x[1];r.g=Integer.parseInt(x[2]);r.ap=Integer.parseInt(x[3]);r.st=Integer.parseInt(x[4]);r.rat=Integer.parseInt(x[5]);r.spe=Integer.parseInt(x[6]);r.res=Integer.parseInt(x[7]);r.qua=Integer.parseInt(x[8]);}catch(Throwable ignored){}return r;}String rating(){return rat<=0?"n/a":(rat/10)+"."+(rat%10);}}
    private static ArrayList<TableRow> table(int y){ArrayList<TableRow> a=new ArrayList<TableRow>();for(String s:rows("tableRows",y))a.add(TableRow.parse(s));return a;}
    private static ArrayList<PlayerRow> players(int y){ArrayList<PlayerRow> a=new ArrayList<PlayerRow>();for(String s:rows("playerRows",y))a.add(PlayerRow.parse(s));return a;}

    public static void show(Activity a){
        if(!available()){Toast.makeText(a,"Career history is unavailable until a career is loaded",Toast.LENGTH_SHORT).show();return;}
        final int[] years=seasons();if(years.length==0)return;final String league=league();final boolean pause=NativePause.begin();
        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,6),dp(a,2),dp(a,6),0);
        TextView intro=text(a,"FINAL TABLES & PLAYER-SEASON RECORDS",12f,true);intro.setPadding(dp(a,6),dp(a,3),dp(a,6),dp(a,4));root.addView(intro);
        final Spinner picker=new Spinner(a);ArrayList<String> labels=new ArrayList<String>();for(int y:years)labels.add(y+"/"+(y+1)+(y==current()?"  •  CURRENT":""));ArrayAdapter<String> pickAdapter=new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,labels);picker.setAdapter(pickAdapter);root.addView(picker);
        LinearLayout tabs=new LinearLayout(a);final Button tableBtn=new Button(a);tableBtn.setText("FINAL TABLE");final Button squadBtn=new Button(a);squadBtn.setText("SQUAD STATS");tabs.addView(tableBtn,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));tabs.addView(squadBtn,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));root.addView(tabs);
        final LinearLayout tableHead=tableRow(a,"#","CLUB","P","W","D","L","GF","GA","GD","PTS",true,null);root.addView(tableHead);
        final TextView legend=text(a,"CHAMPIONS LEAGUE     EUROPA LEAGUE     RELEGATION",9.8f,true);legend.setPadding(dp(a,5),dp(a,2),dp(a,5),dp(a,2));legend.setBackgroundColor(0xfff4f4f4);root.addView(legend);
        final TextView playerHead=text(a,"PLAYER-SEASON STATS   •   ST starts, AP apps, G goals",10f,true);playerHead.setPadding(dp(a,5),dp(a,3),dp(a,5),dp(a,3));playerHead.setVisibility(View.GONE);root.addView(playerHead);
        final ListView list=new ListView(a);list.setDividerHeight(1);root.addView(list,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));final boolean[] tableMode={true};
        final Runnable refresh=()->{
            int y=years[picker.getSelectedItemPosition()];
            tableHead.setVisibility(tableMode[0]?View.VISIBLE:View.GONE);legend.setVisibility(tableMode[0]?View.VISIBLE:View.GONE);playerHead.setVisibility(tableMode[0]?View.GONE:View.VISIBLE);
            if(tableMode[0]){
                final ArrayList<TableRow> rs=table(y);
                list.setAdapter(new ArrayAdapter<TableRow>(a,android.R.layout.simple_list_item_1,rs){@Override public View getView(int pos,View cv,ViewGroup parent){TableRow r=getItem(pos);String badge=r.teamId<0?null:(league+(r.teamId+1)+".png");LinearLayout row=tableRow(a,String.valueOf(r.rank),r.club,String.valueOf(r.p),String.valueOf(r.w),String.valueOf(r.d),String.valueOf(r.l),String.valueOf(r.gf),String.valueOf(r.ga),(r.gd>0?"+":"")+r.gd,String.valueOf(r.pts),r.user,badge);GradientDrawable bg=new GradientDrawable();bg.setColor(zoneColor(league,r.rank,rs.size()));if(r.user)bg.setStroke(dp(a,2),0xff2e7d32);row.setBackground(bg);return row;}});
            }else{
                final ArrayList<PlayerRow> rs=players(y);
                list.setAdapter(new ArrayAdapter<PlayerRow>(a,android.R.layout.simple_list_item_1,rs){@Override public View getView(int pos,View cv,ViewGroup parent){PlayerRow r=getItem(pos);LinearLayout row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,8),dp(a,4),dp(a,8),dp(a,4));row.addView(text(a,r.name,14f,true));row.addView(text(a,"ST "+r.st+"   AP "+r.ap+"   G "+r.g+"   RAT "+r.rating()+"   •   SPE "+r.spe+" RES "+r.res+" QUA "+r.qua,11.5f,false));return row;}});
            }
            tableBtn.setEnabled(!tableMode[0]);squadBtn.setEnabled(tableMode[0]);
        };
        tableBtn.setOnClickListener(v->{tableMode[0]=true;refresh.run();});squadBtn.setOnClickListener(v->{tableMode[0]=false;refresh.run();});picker.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int pos,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});refresh.run();
        AlertDialog dlg=new AlertDialog.Builder(a).setTitle("Career History").setView(root).setPositiveButton("Close",null).create();dlg.setOnDismissListener(v->NativePause.end(pause));dlg.setOnShowListener(v->{Window w=dlg.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*.94f));});dlg.show();
    }

    private static TextView cell(Context c,String value,float size,boolean bold,int gravity){TextView v=text(c,value,size,bold);v.setGravity(gravity);v.setSingleLine(true);v.setPadding(dp(c,1),dp(c,1),dp(c,1),dp(c,1));return v;}
    private static int zoneColor(String league,int rank,int count){int cl="en".equals(league)||"es".equals(league)||"it".equals(league)?4:("de".equals(league)||"fr".equals(league)?3:0);int el="fr".equals(league)?1:(cl>0?2:0);int releg="de".equals(league)?2:(cl>0?3:0);if(rank<=cl)return 0xffdcecff;if(rank<=cl+el)return 0xffffefc2;if(releg>0&&rank>count-releg)return 0xffffdddd;return Color.TRANSPARENT;}
    private static LinearLayout tableRow(Context c,String rank,String club,String p,String w,String d,String l,String gf,String ga,String gd,String pts,boolean bold,String badge){
        LinearLayout row=new LinearLayout(c);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(c,2),0,dp(c,2),0);
        float[] weights={.55f,3.8f,.62f,.62f,.62f,.62f,.68f,.68f,.82f,.82f};String[] values={rank,club,p,w,d,l,gf,ga,gd,pts};
        for(int i=0;i<values.length;i++){int gravity=i==1?(Gravity.START|Gravity.CENTER_VERTICAL):Gravity.CENTER;if(i==1&&badge!=null){LinearLayout clubCell=new LinearLayout(c);clubCell.setOrientation(LinearLayout.HORIZONTAL);clubCell.setGravity(Gravity.CENTER_VERTICAL);try{InputStream in=NativeHistory.class.getResourceAsStream("/resources/"+badge);if(in!=null){ImageView logo=new ImageView(c);logo.setImageBitmap(BitmapFactory.decodeStream(in));logo.setScaleType(ImageView.ScaleType.FIT_CENTER);clubCell.addView(logo,new LinearLayout.LayoutParams(dp(c,18),dp(c,18)));in.close();}}catch(Throwable ignored){}clubCell.addView(cell(c,values[i],10.2f,bold,gravity),new LinearLayout.LayoutParams(0,dp(c,22),1f));row.addView(clubCell,new LinearLayout.LayoutParams(0,dp(c,22),weights[i]));}else row.addView(cell(c,values[i],i==1?10.2f:9.8f,bold,gravity),new LinearLayout.LayoutParams(0,dp(c,22),weights[i]));}
        return row;
    }
}
