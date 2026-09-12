package pfm.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.Gravity;
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
import java.util.ArrayList;
import java.util.List;

/** Native pre-match Starting XI, squad-order and formation editor. */
public final class NativeLineup {
    private NativeLineup() {}

    // Same order as the stock bg formation menu / dw.a[9] formation templates.
    private static final String[] FORMATIONS={
            "4-3-3","4-5-1","3-4-3","3-5-2","5-4-1","5-2-3","4-2-4","5-3-2","4-4-2"
    };

    private static int dp(Context c,int v){
        return Math.round(v*c.getResources().getDisplayMetrics().density);
    }

    private static final class PlayerRow {
        int id,number,spe,res,qua,mor,age,goals,starts,apps,assists,rating10,ratingMatches;
        int fatigue,injury,yellow,red,suspension;
        String name,pos;

        static PlayerRow parse(String s){
            PlayerRow r=new PlayerRow();
            try{
                String[] p=s.split("\\t",-1);
                r.id=Integer.parseInt(p[0]);
                r.number=Integer.parseInt(p[1]);
                r.name=p[2];
                r.pos=p[3];
                r.spe=Integer.parseInt(p[4]);
                r.res=Integer.parseInt(p[5]);
                r.qua=Integer.parseInt(p[6]);
                r.mor=Integer.parseInt(p[7]);
                r.age=Integer.parseInt(p[8]);
                r.goals=Integer.parseInt(p[9]);
                r.starts=Integer.parseInt(p[10]);
                r.apps=Integer.parseInt(p[11]);
                r.assists=Integer.parseInt(p[12]);
                r.rating10=Integer.parseInt(p[13]);
                r.ratingMatches=Integer.parseInt(p[14]);
                r.fatigue=Integer.parseInt(p[15]);
                r.injury=Integer.parseInt(p[16]);
                r.yellow=Integer.parseInt(p[17]);
                r.red=Integer.parseInt(p[18]);
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

        String rating(){
            if(ratingMatches<=0)return "n/a";
            return (rating10/10)+"."+Math.abs(rating10%10);
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

    private static Class<?> bridge() throws Exception { return Class.forName("PfmSquadBridge"); }

    private static boolean canEdit(){
        try{return Boolean.TRUE.equals(bridge().getMethod("canEditLineup").invoke(null));}
        catch(Throwable t){return false;}
    }

    private static int swap(int a,int b){
        try{
            Method m=bridge().getMethod("swapLineupPositions",Integer.TYPE,Integer.TYPE);
            return ((Integer)m.invoke(null,Integer.valueOf(a),Integer.valueOf(b))).intValue();
        }catch(Throwable t){return -1;}
    }

    private static int formationIndex(){
        try{return ((Integer)bridge().getMethod("formationIndex").invoke(null)).intValue();}
        catch(Throwable t){return -1;}
    }

    private static int setFormation(int index){
        try{
            Method m=bridge().getMethod("setFormationIndex",Integer.TYPE);
            return ((Integer)m.invoke(null,Integer.valueOf(index))).intValue();
        }catch(Throwable t){return -1;}
    }

    private static String formationName(int index){
        return index>=0&&index<FORMATIONS.length?FORMATIONS[index]:"?";
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

    private static String slotName(int position){
        return position<11?("XI "+(position+1)):("B "+(position-10));
    }

    private static String selectionText(List<PlayerRow> players,int selected){
        if(selected<0||selected>=players.size())
            return "Tap any player, then tap any other player to swap their squad-order slots.";
        PlayerRow p=players.get(selected);
        return "Selected: "+slotName(selected)+"  #"+p.number+" "+p.name+". Tap another player to swap, or tap this row again to cancel.";
    }

    private static String xiSummary(List<PlayerRow> players){
        int unavailable=0,fatigue=0,count=0;
        int limit=Math.min(11,players.size());
        for(int i=0;i<limit;i++){
            PlayerRow p=players.get(i);
            if(p.unavailable())unavailable++;
            fatigue+=p.fatigue;
            count++;
        }
        int avg=count==0?0:Math.round((float)fatigue/count);
        String s="Starting XI: "+limit+"  •  avg FAT "+avg+"  •  formation "+formationName(formationIndex());
        if(unavailable>0)s+="  •  unavailable "+unavailable;
        return s;
    }

    public static void show(Activity a){
        final ArrayList<PlayerRow> players=new ArrayList<>(loadRows());
        if(players.size()<12){
            Toast.makeText(a,"Line-up is unavailable until a career squad is loaded",Toast.LENGTH_SHORT).show();
            return;
        }
        final boolean pauseToken=NativePause.begin();

        LinearLayout root=new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(a,8),dp(a,2),dp(a,8),0);

        String note="Full pre-match editor: reorder any two squad slots and choose the stock formation. XI 1–XI 11 start; B 1+ are bench/reserves.";
        if(autoRotation())note+="\nAuto rotation is ON and may rebuild the XI before the next matchday; it does not change formation.";
        TextView info=text(a,note,12.2f,false);
        info.setPadding(dp(a,6),dp(a,2),dp(a,6),dp(a,4));
        root.addView(info);

        final TextView editState=text(a,canEdit()?"Pre-match editing enabled":"Editing unavailable during a live match — use legacy substitutions",12.5f,true);
        editState.setPadding(dp(a,6),0,dp(a,6),dp(a,2));
        root.addView(editState);

        final TextView summary=text(a,xiSummary(players),12.3f,true);
        summary.setPadding(dp(a,6),0,dp(a,6),dp(a,3));
        root.addView(summary);

        final Button formation=new Button(a);
        formation.setText("FORMATION: "+formationName(formationIndex()));
        formation.setEnabled(canEdit());
        root.addView(formation,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.WRAP_CONTENT));

        final TextView selection=text(a,"",12f,false);
        selection.setPadding(dp(a,8),dp(a,4),dp(a,8),dp(a,4));
        selection.setBackgroundColor(0xfff2f2f2);
        root.addView(selection);

        LinearLayout actions=new LinearLayout(a);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        final Button cancel=new Button(a);
        cancel.setText("CLEAR");
        cancel.setEnabled(false);
        actions.addView(cancel,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        final Button undo=new Button(a);
        undo.setText("UNDO SWAP");
        undo.setEnabled(false);
        actions.addView(undo,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        final Button refresh=new Button(a);
        refresh.setText("REFRESH");
        actions.addView(refresh,new LinearLayout.LayoutParams(0,LinearLayout.LayoutParams.WRAP_CONTENT,1f));
        root.addView(actions);

        ListView list=new ListView(a);
        list.setDividerHeight(1);
        final int[] selected={-1};
        final int[] lastSwap={-1,-1};

        ArrayAdapter<PlayerRow> adapter=new ArrayAdapter<PlayerRow>(a,android.R.layout.simple_list_item_1,players){
            @Override public View getView(int position,View convertView,ViewGroup parent){
                LinearLayout row;
                TextView section,l1,l2,l3;
                if(convertView instanceof LinearLayout&&((LinearLayout)convertView).getChildCount()==4){
                    row=(LinearLayout)convertView;
                    section=(TextView)row.getChildAt(0);
                    l1=(TextView)row.getChildAt(1);
                    l2=(TextView)row.getChildAt(2);
                    l3=(TextView)row.getChildAt(3);
                }else{
                    row=new LinearLayout(a);
                    row.setOrientation(LinearLayout.VERTICAL);
                    row.setPadding(dp(a,11),dp(a,5),dp(a,11),dp(a,6));
                    row.setMinimumHeight(dp(a,68));
                    section=text(a,"",11.5f,true);
                    l1=text(a,"",16f,true);
                    l2=text(a,"",12.5f,false);
                    l3=text(a,"",11.5f,false);
                    row.addView(section);
                    row.addView(l1);
                    row.addView(l2);
                    row.addView(l3);
                }
                PlayerRow p=getItem(position);
                if(position==0){section.setVisibility(View.VISIBLE);section.setText("STARTING XI");}
                else if(position==11){section.setVisibility(View.VISIBLE);section.setText("BENCH / RESERVES");}
                else{section.setText("");section.setVisibility(View.GONE);}

                l1.setText(slotName(position)+"   #"+p.number+"  "+p.name+"   "+p.pos+(p.unavailable()?"  !":""));
                l2.setText("FAT "+p.fatigue+"   "+p.status()+"   MOR "+p.mor+"   RAT "+p.rating());
                l3.setText("SPE "+p.spe+"  RES "+p.res+"  QUA "+p.qua+"   ST "+p.starts+" AP "+p.apps+" G "+p.goals+" A "+p.assists);
                if(position==selected[0]){
                    row.setBackgroundColor(0xffcfe8cf);
                    l1.setText("✓  "+l1.getText());
                }else if(position<11&&p.unavailable()){
                    row.setBackgroundColor(0xffffe0e0);
                }else if(position<11){
                    row.setBackgroundColor(0xfff6fbf6);
                }else{
                    row.setBackgroundColor(Color.TRANSPARENT);
                }
                return row;
            }
        };
        list.setAdapter(adapter);

        Runnable syncUi=() -> {
            selection.setText(selectionText(players,selected[0]));
            cancel.setEnabled(selected[0]>=0);
            undo.setEnabled(lastSwap[0]>=0&&lastSwap[1]>=0&&canEdit());
            formation.setEnabled(canEdit());
            formation.setText("FORMATION: "+formationName(formationIndex()));
            summary.setText(xiSummary(players));
            adapter.notifyDataSetChanged();
        };

        Runnable reload=() -> {
            List<PlayerRow> fresh=loadRows();
            players.clear();
            players.addAll(fresh);
            selected[0]=-1;
            lastSwap[0]=lastSwap[1]=-1;
            syncUi.run();
        };

        formation.setOnClickListener(v -> {
            if(!canEdit()){
                Toast.makeText(a,"Formation cannot be changed during a live match",Toast.LENGTH_SHORT).show();
                return;
            }
            int current=formationIndex();
            new AlertDialog.Builder(a)
                    .setTitle("Formation")
                    .setSingleChoiceItems(FORMATIONS,current,(dialog,which) -> {
                        int r=setFormation(which);
                        if(r==1){
                            dialog.dismiss();
                            syncUi.run();
                            Toast.makeText(a,"Formation: "+FORMATIONS[which],Toast.LENGTH_SHORT).show();
                        }else if(r==-3){
                            dialog.dismiss();
                            Toast.makeText(a,"Formation cannot be changed during a live match",Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(a,"Could not update formation",Toast.LENGTH_SHORT).show();
                        }
                    })
                    .setNegativeButton("Cancel",null)
                    .show();
        });

        cancel.setOnClickListener(v -> {selected[0]=-1;syncUi.run();});
        refresh.setOnClickListener(v -> reload.run());
        undo.setOnClickListener(v -> {
            if(lastSwap[0]<0||lastSwap[1]<0)return;
            int r=swap(lastSwap[0],lastSwap[1]);
            if(r==1){
                List<PlayerRow> fresh=loadRows();
                players.clear();
                players.addAll(fresh);
                lastSwap[0]=lastSwap[1]=-1;
                selected[0]=-1;
                syncUi.run();
                Toast.makeText(a,"Last squad-order swap undone",Toast.LENGTH_SHORT).show();
            }else{
                lastSwap[0]=lastSwap[1]=-1;
                syncUi.run();
                Toast.makeText(a,"Could not undo the swap",Toast.LENGTH_SHORT).show();
            }
        });

        list.setOnItemClickListener((parent,view,position,id) -> {
            if(!canEdit()){
                editState.setText("Editing unavailable during a live match — use legacy substitutions");
                Toast.makeText(a,"Use the legacy match Substitutions screen during a live match",Toast.LENGTH_SHORT).show();
                return;
            }
            if(selected[0]<0){
                selected[0]=position;
                syncUi.run();
                return;
            }
            if(selected[0]==position){
                selected[0]=-1;
                syncUi.run();
                return;
            }

            int first=selected[0];
            String firstName=players.get(first).name;
            String secondName=players.get(position).name;
            boolean crossed=(first<11)!=(position<11);
            int result=swap(first,position);
            selected[0]=-1;
            if(result==1){
                lastSwap[0]=first;lastSwap[1]=position;
                List<PlayerRow> fresh=loadRows();
                players.clear();
                players.addAll(fresh);
                syncUi.run();
                String what=crossed?"Starting XI membership updated":"Squad order updated";
                Toast.makeText(a,what+": "+firstName+" ↔ "+secondName,Toast.LENGTH_SHORT).show();
            }else if(result==-3){
                editState.setText("Editing unavailable during a live match — use legacy substitutions");
                syncUi.run();
                Toast.makeText(a,"Live-match squad changes are blocked",Toast.LENGTH_SHORT).show();
            }else{
                syncUi.run();
                Toast.makeText(a,"Could not update the squad order",Toast.LENGTH_SHORT).show();
            }
        });
        root.addView(list,new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,0,1f));
        syncUi.run();

        AlertDialog dlg=new AlertDialog.Builder(a)
                .setTitle("Line-up / Tactics")
                .setView(root)
                .setPositiveButton("Close",null)
                .create();
        dlg.setOnDismissListener(x -> NativePause.end(pauseToken));
        dlg.setOnShowListener(x -> {
            Window w=dlg.getWindow();
            if(w!=null)w.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    Math.round(a.getResources().getDisplayMetrics().heightPixels*0.95f));
        });
        dlg.show();
    }
}
