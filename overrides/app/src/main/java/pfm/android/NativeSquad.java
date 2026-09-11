package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** First read-only native Android squad/player screen. */
public final class NativeSquad {
    private NativeSquad() {}

    private static int dp(Context c,int v){
        return Math.round(v*c.getResources().getDisplayMetrics().density);
    }

    private static final class PlayerRow {
        int id,number,spe,res,qua,mor,age,starts,apps,assists,rating10;
        String name,pos;

        static PlayerRow parse(String s){
            String[] p=s.split("\\t",-1);
            PlayerRow r=new PlayerRow();
            try{
                r.id=Integer.parseInt(p[0]);
                r.number=Integer.parseInt(p[1]);
                r.name=p[2];
                r.pos=p[3];
                r.spe=Integer.parseInt(p[4]);
                r.res=Integer.parseInt(p[5]);
                r.qua=Integer.parseInt(p[6]);
                r.mor=Integer.parseInt(p[7]);
                r.age=Integer.parseInt(p[8]);
                r.starts=Integer.parseInt(p[9]);
                r.apps=Integer.parseInt(p[10]);
                r.assists=Integer.parseInt(p[11]);
                r.rating10=Integer.parseInt(p[12]);
            }catch(Throwable t){
                r.name=s; r.pos="?";
            }
            return r;
        }

        String rating(){
            if(rating10<=0)return "-";
            return (rating10/10)+"."+Math.abs(rating10%10);
        }
    }

    private static String teamName(){
        try{
            Class<?> c=Class.forName("PfmSquadBridge");
            Method m=c.getMethod("teamName");
            Object r=m.invoke(null);
            return r==null?"Squad":String.valueOf(r);
        }catch(Throwable t){return "Squad";}
    }

    private static List<PlayerRow> loadRows(){
        ArrayList<PlayerRow> out=new ArrayList<>();
        try{
            Class<?> c=Class.forName("PfmSquadBridge");
            Method available=c.getMethod("available");
            Object ok=available.invoke(null);
            if(!Boolean.TRUE.equals(ok))return out;
            Method m=c.getMethod("rows");
            String[] rows=(String[])m.invoke(null);
            if(rows!=null)for(String s:rows)out.add(PlayerRow.parse(s));
        }catch(Throwable ignored){}
        return out;
    }

    private static TextView text(Context c,String s,float size,boolean bold){
        TextView v=new TextView(c);
        v.setText(s);
        v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }

    private static void showPlayer(Activity a,PlayerRow p){
        StringBuilder b=new StringBuilder();
        b.append("No. ").append(p.number).append("   ").append(p.pos).append('\n');
        b.append("Age: ").append(p.age).append("\n\n");
        b.append("SPE  ").append(p.spe).append("\n");
        b.append("RES  ").append(p.res).append("\n");
        b.append("QUA  ").append(p.qua).append("\n");
        b.append("MOR  ").append(p.mor).append("\n\n");
        b.append("Season\n");
        b.append("Starts: ").append(p.starts).append("\n");
        b.append("Appearances: ").append(p.apps).append("\n");
        b.append("Assists: ").append(p.assists).append("\n");
        b.append("Rating: ").append(p.rating()).append("\n\n");
        b.append("Read-only native view in v19. Line-up changes still use the legacy screen.");
        new AlertDialog.Builder(a)
                .setTitle("#"+p.number+"  "+p.name)
                .setMessage(b.toString())
                .setPositiveButton("Close",null)
                .show();
    }

    public static void show(Activity a){
        final List<PlayerRow> players=loadRows();
        if(players.isEmpty()){
            Toast.makeText(a,"Start or load a career first",Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout root=new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a,10),dp(a,4),dp(a,10),0);

        TextView info=text(a,"Tap a player for details. Native view reads directly from the Alpha 0.84 game core.",13f,false);
        info.setPadding(dp(a,6),dp(a,2),dp(a,6),dp(a,8));
        root.addView(info);

        ListView list=new ListView(a);
        list.setDividerHeight(1);
        ArrayAdapter<PlayerRow> adapter=new ArrayAdapter<PlayerRow>(a,android.R.layout.simple_list_item_1,players){
            @Override public View getView(int position,View convertView,ViewGroup parent){
                LinearLayout row;
                TextView l1,l2;
                if(convertView instanceof LinearLayout){
                    row=(LinearLayout)convertView;
                    l1=(TextView)row.getChildAt(0);
                    l2=(TextView)row.getChildAt(1);
                }else{
                    row=new LinearLayout(a);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,8));
                    row.setMinimumHeight(dp(a,62));
                    l1=text(a,"",17f,true);
                    l2=text(a,"",13f,false);
                    row.addView(l1);
                    row.addView(l2);
                }
                PlayerRow p=getItem(position);
                l1.setText("#"+p.number+"  "+p.name+"   "+p.pos);
                l2.setText("SPE "+p.spe+"   RES "+p.res+"   QUA "+p.qua+"   MOR "+p.mor+"   AGE "+p.age+
                        "\nST "+p.starts+"   AP "+p.apps+"   AST "+p.assists+"   RAT "+p.rating());
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent,view,position,id) -> showPlayer(a,players.get(position)));
        root.addView(list,new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,0,1f));

        AlertDialog dlg=new AlertDialog.Builder(a)
                .setTitle(teamName()+" — Squad")
                .setView(root)
                .setPositiveButton("Close",null)
                .create();
        dlg.setOnShowListener(x -> {
            Window w=dlg.getWindow();
            if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    Math.round(a.getResources().getDisplayMetrics().heightPixels*0.86f));
        });
        dlg.show();
    }
}
