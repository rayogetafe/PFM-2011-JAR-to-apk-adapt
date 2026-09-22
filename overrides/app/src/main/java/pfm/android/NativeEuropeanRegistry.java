package pfm.android;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.util.*;

/** Audited non-top-five club layer. Rosters remain quarantined until ownership conflicts are resolved. */
public final class NativeEuropeanRegistry {
 private NativeEuropeanRegistry(){}
 static final class Club {String id,country,name,shortName,europe,profile,priority,badge,note;int rep,fin,acad,activeFrom;}
 private static ArrayList<Club> clubs;
 private static int dp(Context c,int n){return Math.round(n*c.getResources().getDisplayMetrics().density);}
 static synchronized ArrayList<Club> all(Context c){if(clubs!=null)return clubs;clubs=new ArrayList<Club>();try{BufferedReader r=new BufferedReader(new InputStreamReader(c.getAssets().open("europe_clubs_v72.tsv"),"UTF-8"));String s;r.readLine();while((s=r.readLine())!=null){String[] q=s.split("\t",-1);if(q.length<15)continue;Club x=new Club();x.id=q[0];x.country=q[1];x.name=q[2];x.shortName=q[3];x.europe=q[6];x.rep=Integer.parseInt(q[7]);x.fin=Integer.parseInt(q[8]);x.acad=Integer.parseInt(q[9]);x.profile=q[10];x.priority=q[11];x.activeFrom=Integer.parseInt(q[12]);x.badge=q[13];x.note=q[14];clubs.add(x);}r.close();}catch(Throwable ignored){clubs.clear();}return clubs;}
 private static Drawable badge(Context c,Club x){try{InputStream in=c.getAssets().open("europe_badges_v72/"+x.badge);Bitmap b=BitmapFactory.decodeStream(in);in.close();return new BitmapDrawable(c.getResources(),b);}catch(Throwable t){return null;}}
 private static void showRoster(Activity a,Club club){
  ArrayList<String> names=new ArrayList<String>();int conflicts=0;
  try(BufferedReader reader=new BufferedReader(new InputStreamReader(a.getAssets().open("europe_roster_review_v73.tsv"),"UTF-8"))){
   String line;reader.readLine();while((line=reader.readLine())!=null){String[] fields=line.split("\t",-1);if(fields.length!=4||!club.id.equals(fields[0]))continue;
    String label=fields[1];if("CROSS_CLUB".equals(fields[3])){label+="  ·  multiple clubs in source";conflicts++;}
    if("DATE_CONFLICT".equals(fields[3])){label+="  ·  date conflict";conflicts++;}names.add(label);
   }
  }catch(IOException error){new AlertDialog.Builder(a).setMessage("Roster reference unavailable: "+error.getMessage()).setPositiveButton("Close",null).show();return;}
  LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);
  TextView note=new TextView(a);note.setPadding(dp(a,12),dp(a,8),dp(a,12),dp(a,8));
  note.setText(names.size()+" supplied names · "+conflicts+" flagged here. Season-wide research draft; dates and identities are not confirmed. These names are not career players and cannot transfer.");root.addView(note);
  ListView list=new ListView(a);list.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,names));root.addView(list,new LinearLayout.LayoutParams(-1,0,1));
  AlertDialog dialog=new AlertDialog.Builder(a).setTitle(club.name+" · roster draft").setView(root).setPositiveButton("Close",null).create();
  dialog.setOnShowListener(v->{Window window=dialog.getWindow();if(window!=null)window.setLayout(-1,Math.round(a.getResources().getDisplayMetrics().heightPixels*.85f));});dialog.show();
 }
 private static ArrayAdapter<Club> adapter(Activity a,ArrayList<Club> rows){return new ArrayAdapter<Club>(a,android.R.layout.simple_list_item_1,rows){public View getView(int n,View old,ViewGroup parent){Club x=getItem(n);LinearLayout row=new LinearLayout(a);row.setPadding(dp(a,6),dp(a,4),dp(a,6),dp(a,4));ImageView image=new ImageView(a);Drawable d=badge(a,x);if(d!=null)image.setImageDrawable(d);row.addView(image,new LinearLayout.LayoutParams(dp(a,52),dp(a,52)));TextView text=new TextView(a);text.setText(x.name+"  ·  "+x.country+"\nRep "+x.rep+"  Fin "+x.fin+"  Acad "+x.acad+"  ·  "+x.profile+(x.activeFrom>2010?"  ·  active "+x.activeFrom:""));text.setTextSize(13.5f);text.setPadding(dp(a,8),0,0,0);row.addView(text,new LinearLayout.LayoutParams(0,-2,1));return row;}};}
 public static void show(Activity a){final boolean pause=NativePause.begin();ArrayList<Club> source=all(a),shown=new ArrayList<Club>();LinearLayout root=new LinearLayout(a);root.setOrientation(LinearLayout.VERTICAL);TextView status=new TextView(a);status.setPadding(dp(a,8),dp(a,6),dp(a,8),dp(a,6));status.setText("129 audited club records and supplied badges. CLUB_ONLY means active market/competition metadata without importing the conflicted draft rosters.");root.addView(status);Spinner country=new Spinner(a);TreeSet<String> cs=new TreeSet<String>();for(Club x:source)cs.add(x.country);ArrayList<String> labels=new ArrayList<String>();labels.add("ALL COUNTRIES");labels.addAll(cs);country.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_spinner_dropdown_item,labels));root.addView(country);ListView list=new ListView(a);ArrayAdapter<Club> ad=adapter(a,shown);list.setAdapter(ad);root.addView(list,new LinearLayout.LayoutParams(-1,0,1));Runnable refresh=()->{shown.clear();String selected=String.valueOf(country.getSelectedItem());for(Club x:source)if("ALL COUNTRIES".equals(selected)||x.country.equals(selected))shown.add(x);status.setText("Shown "+shown.size()+" / "+source.size()+" clubs  ·  CLUB_ONLY\n2321 supplied names for review; 35 cross-club identities flagged. No career import.");ad.notifyDataSetChanged();};country.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onItemSelected(android.widget.AdapterView<?> p,View v,int n,long id){refresh.run();}public void onNothingSelected(android.widget.AdapterView<?> p){}});list.setOnItemClickListener((p,v,n,id)->{Club x=shown.get(n);new AlertDialog.Builder(a).setTitle(x.name).setMessage(x.country+"\nEuropean status: "+x.europe+"\nReputation "+x.rep+" / Finance "+x.fin+" / Academy "+x.acad+"\nTransfer profile: "+x.profile+"\nPriority: "+x.priority+"\nActive from: "+x.activeFrom+(x.note.length()>0?"\n\n"+x.note:"")).setNeutralButton("Roster draft",(dialog,which)->showRoster(a,x)).setPositiveButton("Close",null).show();});refresh.run();AlertDialog d=new AlertDialog.Builder(a).setTitle("European Club Registry").setView(root).setPositiveButton("Close",null).create();d.setOnDismissListener(v->NativePause.end(pause));d.setOnShowListener(v->{Window w=d.getWindow();if(w!=null)w.setLayout(-1,Math.round(a.getResources().getDisplayMetrics().heightPixels*.94f));});d.show();}
}
