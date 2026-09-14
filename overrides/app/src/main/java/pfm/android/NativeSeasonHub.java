package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.BitmapFactory;
import android.graphics.drawable.GradientDrawable;
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
import android.widget.ImageView;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;

/** Read-only native season overview, league table and user fixture list. */
public final class NativeSeasonHub {
    private NativeSeasonHub() {}

    private static int dp(Context c,int v){return Math.round(v*c.getResources().getDisplayMetrics().density);}
    private static TextView text(Context c,String s,float size,boolean bold){TextView v=new TextView(c);v.setText(s);v.setTextSize(size);if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);return v;}

    private static Class<?> bridge() throws Exception{return Class.forName("PfmSeasonBridge");}
    private static String callString(String name){try{Object o=bridge().getMethod(name).invoke(null);return o==null?"":String.valueOf(o);}catch(Throwable t){return "";}}
    private static String[] callRows(String name){try{return (String[])bridge().getMethod(name).invoke(null);}catch(Throwable t){return new String[0];}}
    private static boolean available(){try{return Boolean.TRUE.equals(bridge().getMethod("available").invoke(null));}catch(Throwable t){return false;}}

    private static final class Overview {
        String team="",season="",next="";int played,total,rank,points,w,d,l,gf,ga;
        static Overview parse(String s){Overview o=new Overview();try{String[] p=s.split("\\t",-1);o.team=p[0];o.season=p[1];o.played=Integer.parseInt(p[2]);o.total=Integer.parseInt(p[3]);o.rank=Integer.parseInt(p[4]);o.points=Integer.parseInt(p[5]);o.w=Integer.parseInt(p[6]);o.d=Integer.parseInt(p[7]);o.l=Integer.parseInt(p[8]);o.gf=Integer.parseInt(p[9]);o.ga=Integer.parseInt(p[10]);o.next=p[11];}catch(Throwable ignored){}return o;}
    }

    private static final class TableRow {
        int rank,p,w,d,l,gf,ga,gd,pts,teamId=-1;String club="";boolean user;
        static TableRow parse(String s){TableRow r=new TableRow();try{String[] p=s.split("\\t",-1);r.rank=Integer.parseInt(p[0]);r.club=p[1];r.p=Integer.parseInt(p[2]);r.w=Integer.parseInt(p[3]);r.d=Integer.parseInt(p[4]);r.l=Integer.parseInt(p[5]);r.gf=Integer.parseInt(p[6]);r.ga=Integer.parseInt(p[7]);r.gd=Integer.parseInt(p[8]);r.pts=Integer.parseInt(p[9]);r.user="1".equals(p[10]);if(p.length>11)r.teamId=Integer.parseInt(p[11]);}catch(Throwable ignored){}return r;}
    }

    private static final class MatchRow {
        int round,hg,ag;String venue="?",opp="?",home="?",away="?";boolean played,next;
        static MatchRow parse(String s){MatchRow r=new MatchRow();try{String[] p=s.split("\\t",-1);r.round=Integer.parseInt(p[0]);r.venue=p[1];r.opp=p[2];r.home=p[3];r.away=p[4];r.hg=Integer.parseInt(p[5]);r.ag=Integer.parseInt(p[6]);r.played="1".equals(p[7]);r.next="1".equals(p[8]);}catch(Throwable ignored){}return r;}
        String resultForUser(){if(!played||hg<0||ag<0)return "";int uf="H".equals(venue)?hg:ag,ua="H".equals(venue)?ag:hg;return uf>ua?"W":(uf<ua?"L":"D");}
    }

    private static ArrayList<TableRow> table(){ArrayList<TableRow> a=new ArrayList<>();for(String s:callRows("tableRows"))a.add(TableRow.parse(s));return a;}
    private static ArrayList<MatchRow> schedule(){ArrayList<MatchRow> a=new ArrayList<>();for(String s:callRows("scheduleRows"))a.add(MatchRow.parse(s));return a;}

    public static void show(Activity a){
        if(!available()){Toast.makeText(a,"Season data is unavailable until a career is loaded",Toast.LENGTH_SHORT).show();return;}
        final Overview ov=Overview.parse(callString("overview"));
        final String league=callString("leagueCode");
        final ArrayList<TableRow> standings=table();
        final ArrayList<MatchRow> matches=schedule();

        LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(a,8),dp(a,3),dp(a,8),0);

        TextView title=text(a,ov.team+"   •   "+ov.season,18f,true);title.setPadding(dp(a,7),dp(a,2),dp(a,7),dp(a,2));root.addView(title);
        TextView summary=text(a,"#"+ov.rank+"   "+ov.points+" pts   •   P "+ov.played+"  W "+ov.w+"  D "+ov.d+"  L "+ov.l+"   •   GF "+ov.gf+" : "+ov.ga+"  GD "+(ov.gf-ov.ga),13.2f,true);
        summary.setPadding(dp(a,7),0,dp(a,7),dp(a,4));root.addView(summary);
        String roundText=ov.played>=ov.total?"Season complete":"Next: "+ov.next+"   •   "+(ov.played+1)+" / "+ov.total;
        TextView next=text(a,roundText,13f,false);next.setPadding(dp(a,7),dp(a,4),dp(a,7),dp(a,7));next.setBackgroundColor(0xfff2f2f2);root.addView(next);

        Button leagueCenter=new Button(a);leagueCenter.setText("LEAGUE CENTER • ALL MATCHES / CLUBS");leagueCenter.setOnClickListener(v->NativeLeagueCenter.show(a));root.addView(leagueCenter,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        LinearLayout tabs=new LinearLayout(a);tabs.setOrientation(LinearLayout.HORIZONTAL);
        final Button tableBtn=new Button(a);tableBtn.setText("TABLE");
        final Button scheduleBtn=new Button(a);scheduleBtn.setText("SCHEDULE");
        final Button historyBtn=new Button(a);historyBtn.setText("HISTORY");
        tabs.addView(tableBtn,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        tabs.addView(scheduleBtn,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        tabs.addView(historyBtn,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));root.addView(tabs);

        final LinearLayout tableHead=tableRow(a,"#","CLUB","P","W","D","L","GF","GA","GD","PTS",true,null);root.addView(tableHead);
        final TextView legend=text(a,"CHAMPIONS LEAGUE     EUROPA LEAGUE     RELEGATION",9.8f,true);legend.setPadding(dp(a,5),dp(a,2),dp(a,5),dp(a,2));legend.setBackgroundColor(0xfff4f4f4);root.addView(legend);
        final ListView tableList=new ListView(a);tableList.setDividerHeight(1);
        ArrayAdapter<TableRow> tableAdapter=new ArrayAdapter<TableRow>(a,android.R.layout.simple_list_item_1,standings){
            @Override public View getView(int pos,View cv,ViewGroup parent){
                TableRow r=getItem(pos);LinearLayout row=tableRow(a,String.valueOf(r.rank),r.club,String.valueOf(r.p),String.valueOf(r.w),String.valueOf(r.d),String.valueOf(r.l),String.valueOf(r.gf),String.valueOf(r.ga),(r.gd>0?"+":"")+r.gd,String.valueOf(r.pts),r.user,r.teamId<0?null:(league+(r.teamId+1)+".png"));
                int color=zoneColor(league,r.rank,standings.size());GradientDrawable bg=new GradientDrawable();bg.setColor(color);if(r.user){bg.setStroke(dp(a,2),0xff2e7d32);}row.setBackground(bg);return row;
            }};
        tableList.setAdapter(tableAdapter);root.addView(tableList,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));

        final TextView schedHead=text(a,"YOUR LEAGUE FIXTURES",11.5f,true);schedHead.setPadding(dp(a,7),dp(a,5),dp(a,7),dp(a,4));schedHead.setVisibility(View.GONE);root.addView(schedHead);
        final ListView schedList=new ListView(a);schedList.setDividerHeight(1);schedList.setVisibility(View.GONE);
        ArrayAdapter<MatchRow> schedAdapter=new ArrayAdapter<MatchRow>(a,android.R.layout.simple_list_item_1,matches){
            @Override public View getView(int pos,View cv,ViewGroup parent){
                LinearLayout row;TextView l1,l2;
                if(cv instanceof LinearLayout&&((LinearLayout)cv).getChildCount()==2){row=(LinearLayout)cv;l1=(TextView)row.getChildAt(0);l2=(TextView)row.getChildAt(1);}else{row=new LinearLayout(a);row.setOrientation(LinearLayout.VERTICAL);row.setPadding(dp(a,9),dp(a,6),dp(a,9),dp(a,6));l1=text(a,"",14.5f,true);l2=text(a,"",11.8f,false);row.addView(l1);row.addView(l2);}
                MatchRow r=getItem(pos);String score=r.played&&r.hg>=0&&r.ag>=0?(r.hg+" – "+r.ag):"UPCOMING";String outcome=r.resultForUser();
                l1.setText("R"+r.round+"   "+r.home+"  "+score+"  "+r.away+(outcome.length()>0?"   ["+outcome+"]":""));
                l2.setText(("H".equals(r.venue)?"HOME":"AWAY")+" vs "+r.opp+(r.next?"   •   NEXT MATCH":""));
                if(r.next)row.setBackgroundColor(0xfffff2cc);else if(r.played)row.setBackgroundColor(0xfff5f5f5);else row.setBackgroundColor(Color.TRANSPARENT);return row;
            }};
        schedList.setAdapter(schedAdapter);root.addView(schedList,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));

        final boolean[] tableMode={true};
        Runnable sync=()->{tableHead.setVisibility(tableMode[0]?View.VISIBLE:View.GONE);legend.setVisibility(tableMode[0]?View.VISIBLE:View.GONE);tableList.setVisibility(tableMode[0]?View.VISIBLE:View.GONE);schedHead.setVisibility(tableMode[0]?View.GONE:View.VISIBLE);schedList.setVisibility(tableMode[0]?View.GONE:View.VISIBLE);tableBtn.setEnabled(!tableMode[0]);scheduleBtn.setEnabled(tableMode[0]);if(!tableMode[0]&&!matches.isEmpty()){int target=Math.max(0,Math.min(matches.size()-1,ov.played-2));schedList.setSelection(target);}};
        tableBtn.setOnClickListener(v->{tableMode[0]=true;sync.run();});scheduleBtn.setOnClickListener(v->{tableMode[0]=false;sync.run();});sync.run();
        historyBtn.setOnClickListener(v->NativeHistory.show(a));

        AlertDialog dlg=new AlertDialog.Builder(a).setTitle("Season / Competition").setView(root).setPositiveButton("Close",null).create();
        dlg.setOnShowListener(x->{Window w=dlg.getWindow();if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,Math.round(a.getResources().getDisplayMetrics().heightPixels*0.94f));});dlg.show();
    }

    private static TextView cell(Context c,String value,float size,boolean bold,int gravity){TextView v=text(c,value,size,bold);v.setGravity(gravity);v.setSingleLine(true);v.setPadding(dp(c,1),dp(c,1),dp(c,1),dp(c,1));return v;}
    private static int zoneColor(String league,int rank,int count){int cl="en".equals(league)||"es".equals(league)||"it".equals(league)?4:("de".equals(league)||"fr".equals(league)?3:0);int el="fr".equals(league)?1:(cl>0?2:0);int releg="de".equals(league)?2:(cl>0?3:0);if(rank<=cl)return 0xffdcecff;if(rank<=cl+el)return 0xffffefc2;if(releg>0&&rank>count-releg)return 0xffffdddd;return Color.TRANSPARENT;}
    private static LinearLayout tableRow(Context c,String rank,String club,String p,String w,String d,String l,String gf,String ga,String gd,String pts,boolean bold,String badge){
        LinearLayout row=new LinearLayout(c);row.setOrientation(LinearLayout.HORIZONTAL);row.setPadding(dp(c,2),0,dp(c,2),0);
        float[] weights={.55f,3.8f,.62f,.62f,.62f,.62f,.68f,.68f,.82f,.82f};String[] values={rank,club,p,w,d,l,gf,ga,gd,pts};
        for(int i=0;i<values.length;i++){int gravity=i==1?(android.view.Gravity.START|android.view.Gravity.CENTER_VERTICAL):android.view.Gravity.CENTER;if(i==1&&badge!=null){LinearLayout clubCell=new LinearLayout(c);clubCell.setOrientation(LinearLayout.HORIZONTAL);clubCell.setGravity(android.view.Gravity.CENTER_VERTICAL);try{InputStream in=c.getAssets().open("badges/"+badge);ImageView logo=new ImageView(c);logo.setImageBitmap(BitmapFactory.decodeStream(in));logo.setScaleType(ImageView.ScaleType.FIT_CENTER);clubCell.addView(logo,new LinearLayout.LayoutParams(dp(c,18),dp(c,18)));in.close();}catch(Throwable ignored){}clubCell.addView(cell(c,values[i],10.5f,bold,gravity),new LinearLayout.LayoutParams(0,dp(c,22),1f));row.addView(clubCell,new LinearLayout.LayoutParams(0,dp(c,22),weights[i]));}else row.addView(cell(c,values[i],i==1?10.5f:10.1f,bold,gravity),new LinearLayout.LayoutParams(0,dp(c,22),weights[i]));}
        return row;
    }
}
