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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/** Native pre-match Starting XI editor. Formation itself is still owned by the legacy core. */
public final class NativeLineup {
    private NativeLineup() {}

    private static int dp(Context c,int v){
        return Math.round(v*c.getResources().getDisplayMetrics().density);
    }

    private static final class PlayerRow {
        int id,number,fatigue,injury,suspension;
        String name,pos;

        static PlayerRow parse(String s){
            PlayerRow r=new PlayerRow();
            try{
                String[] p=s.split("\\t",-1);
                r.id=Integer.parseInt(p[0]);
                r.number=Integer.parseInt(p[1]);
                r.name=p[2];
                r.pos=p[3];
                r.fatigue=Integer.parseInt(p[15]);
                r.injury=Integer.parseInt(p[16]);
                r.suspension=Integer.parseInt(p[19]);
            }catch(Throwable t){
                r.name=s; r.pos="?";
            }
            return r;
        }

        boolean unavailable(){ return injury>0||suspension>0; }

        String status(){
            if(injury>0&&suspension>0)return "INJ "+injury+"r / SUSP "+suspension+"r";
            if(injury>0)return "INJ "+injury+"r";
            if(suspension>0)return "SUSP "+suspension+"r";
            return "available";
        }
    }

    private static List<PlayerRow> loadRows(){
        ArrayList<PlayerRow> out=new ArrayList<>();
        try{
            Class<?> c=Class.forName("PfmSquadBridge");
            String[] rows=(String[])c.getMethod("rows").invoke(null);
            if(rows!=null)for(String s:rows)out.add(PlayerRow.parse(s));
        }catch(Throwable ignored){}
        return out;
    }

    private static boolean canEdit(){
        try{
            Class<?> c=Class.forName("PfmSquadBridge");
            return Boolean.TRUE.equals(c.getMethod("canEditLineup").invoke(null));
        }catch(Throwable t){return false;}
    }

    private static int swap(int a,int b){
        try{
            Class<?> c=Class.forName("PfmSquadBridge");
            Method m=c.getMethod("swapLineupPositions",Integer.TYPE,Integer.TYPE);
            return ((Integer)m.invoke(null,Integer.valueOf(a),Integer.valueOf(b))).intValue();
        }catch(Throwable t){return -1;}
    }

    private static boolean autoRotation(){
        try{
            Class<?> c=Class.forName("PfmSettingsBridge");
            return Boolean.TRUE.equals(c.getMethod("getAutoRotation").invoke(null));
        }catch(Throwable t){return false;}
    }

    private static TextView text(Context c,String s,float size,boolean bold){
        TextView v=new TextView(c);
        v.setText(s);
        v.setTextSize(size);
        if(bold)v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        return v;
    }

    public static void show(Activity a){
        final ArrayList<PlayerRow> players=new ArrayList<>(loadRows());
        if(players.size()<12){
            Toast.makeText(a,"Line-up is unavailable until a career squad is loaded",Toast.LENGTH_SHORT).show();
            return;
        }

        LinearLayout root=new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a,8),dp(a,2),dp(a,8),0);

        String note="Tap one player, then a player on the other side of the XI / bench split to swap them. Formation is not changed.";
        if(autoRotation())note+="\nAuto rotation is ON and may rebuild the XI before the next matchday.";
        TextView info=text(a,note,12.5f,false);
        info.setPadding(dp(a,6),dp(a,2),dp(a,6),dp(a,6));
        root.addView(info);

        final TextView editState=text(a,canEdit()?"Pre-match XI editing enabled":"XI editing unavailable during a live match — use legacy substitutions",12.5f,true);
        editState.setPadding(dp(a,6),0,dp(a,6),dp(a,6));
        root.addView(editState);

        ListView list=new ListView(a);
        list.setDividerHeight(1);
        final int[] selected={-1};

        ArrayAdapter<PlayerRow> adapter=new ArrayAdapter<PlayerRow>(a,android.R.layout.simple_list_item_1,players){
            @Override public View getView(int position,View convertView,ViewGroup parent){
                LinearLayout row;
                TextView section,l1,l2;
                if(convertView instanceof LinearLayout&&((LinearLayout)convertView).getChildCount()==3){
                    row=(LinearLayout)convertView;
                    section=(TextView)row.getChildAt(0);
                    l1=(TextView)row.getChildAt(1);
                    l2=(TextView)row.getChildAt(2);
                }else{
                    row=new LinearLayout(a);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(dp(a,11),dp(a,5),dp(a,11),dp(a,6));
                    row.setMinimumHeight(dp(a,58));
                    section=text(a,"",11.5f,true);
                    l1=text(a,"",16f,true);
                    l2=text(a,"",12.5f,false);
                    row.addView(section);
                    row.addView(l1);
                    row.addView(l2);
                }
                PlayerRow p=getItem(position);
                if(position==0){ section.setVisibility(View.VISIBLE); section.setText("STARTING XI"); }
                else if(position==11){ section.setVisibility(View.VISIBLE); section.setText("BENCH / RESERVES"); }
                else { section.setText(""); section.setVisibility(View.GONE); }

                String slot=position<11?("XI "+(position+1)):"BENCH";
                l1.setText(slot+"   #"+p.number+"  "+p.name+"   "+p.pos+(p.unavailable()?"  !":""));
                l2.setText("FAT "+p.fatigue+"   "+p.status());
                row.setBackgroundColor(position==selected[0]?0xffd9ead3:Color.TRANSPARENT);
                return row;
            }
        };
        list.setAdapter(adapter);
        list.setOnItemClickListener((parent,view,position,id) -> {
            if(!canEdit()){
                editState.setText("XI editing unavailable during a live match — use legacy substitutions");
                Toast.makeText(a,"Use the legacy match Substitutions screen during a live match",Toast.LENGTH_SHORT).show();
                return;
            }
            if(selected[0]<0){
                selected[0]=position;
                adapter.notifyDataSetChanged();
                Toast.makeText(a,position<11?"Now select a bench player":"Now select a Starting XI player",Toast.LENGTH_SHORT).show();
                return;
            }
            if(selected[0]==position){
                selected[0]=-1;
                adapter.notifyDataSetChanged();
                return;
            }
            if((selected[0]<11)==(position<11)){
                selected[0]=position;
                adapter.notifyDataSetChanged();
                Toast.makeText(a,position<11?"Now select a bench player":"Now select a Starting XI player",Toast.LENGTH_SHORT).show();
                return;
            }

            int first=selected[0];
            int result=swap(first,position);
            selected[0]=-1;
            if(result==1){
                List<PlayerRow> fresh=loadRows();
                players.clear();
                players.addAll(fresh);
                adapter.notifyDataSetChanged();
                Toast.makeText(a,"Starting XI updated",Toast.LENGTH_SHORT).show();
            }else if(result==-3){
                editState.setText("XI editing unavailable during a live match — use legacy substitutions");
                Toast.makeText(a,"Live-match XI changes are blocked",Toast.LENGTH_SHORT).show();
            }else{
                adapter.notifyDataSetChanged();
                Toast.makeText(a,"Could not update the Starting XI",Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(list,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));

        AlertDialog dlg=new AlertDialog.Builder(a)
                .setTitle("Starting XI")
                .setView(root)
                .setPositiveButton("Close",null)
                .create();
        dlg.setOnShowListener(x -> {
            Window w=dlg.getWindow();
            if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    Math.round(a.getResources().getDisplayMetrics().heightPixels*0.92f));
        });
        dlg.show();
    }
}
