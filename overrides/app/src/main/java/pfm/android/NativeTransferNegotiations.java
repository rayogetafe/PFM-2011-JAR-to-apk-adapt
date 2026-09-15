package pfm.android;

import android.app.*;
import android.content.*;
import android.text.InputType;
import android.widget.*;
import java.util.*;

/** Persistent, career-scoped negotiation state. No ownership mutation occurs here. */
final class NativeTransferNegotiations {
    private static final String PREFS="pfm_global_offers_v53";
    private NativeTransferNegotiations() {}
    private static android.content.SharedPreferences prefs(Context c){return c.getSharedPreferences(PREFS,0);}
    private static String key(NativeGlobalMarket.R r){return "s"+NativeWorldCenter.slot()+"_y"+NativeWorldCenter.season()+"_"+r.key();}
    static String status(Context c,NativeGlobalMarket.R r){return prefs(c).getString(key(r)+"_status","");}
    static int offer(Context c,NativeGlobalMarket.R r){return prefs(c).getInt(key(r)+"_offer",0);}
    private static void save(Context c,NativeGlobalMarket.R r,int offer,String status){prefs(c).edit().putInt(key(r)+"_offer",offer).putString(key(r)+"_status",status).apply();}

    static void prepare(Activity a,NativeGlobalMarket.R r,int cash,Runnable refresh){
        int ask=NativeGlobalMarket.quoted(r.ask),suggested=Math.max(r.p.value,ask*90/100);
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(NativeGlobalMarket.dp(a,18),0,NativeGlobalMarket.dp(a,18),0);
        TextView note=NativeGlobalMarket.tx(a,"Value "+NativeGlobalMarket.money(r.p.value)+"   Seller estimate "+NativeGlobalMarket.money(ask)+"\nBudget "+NativeGlobalMarket.money(cash)+"\nEnter transfer fee in £ millions:",14f,false);box.addView(note);
        EditText input=new EditText(a);input.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);input.setText(String.format(Locale.US,"%.1f",suggested/1000000.0));input.setSelectAllOnFocus(true);box.addView(input);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Prepare bid — "+r.p.name).setView(box).setNegativeButton("Cancel",null).setPositiveButton("SUBMIT",null).create();
        d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2->{
            int amount;try{amount=(int)Math.round(Double.parseDouble(input.getText().toString().replace(',','.'))*1000000.0);}catch(Throwable t){input.setError("Enter a valid amount");return;}
            if(amount<=0){input.setError("Offer must be positive");return;}if(amount>cash){input.setError("Offer exceeds your current budget");return;}
            String result;if(r.notForSale)result="REJECTED — exceptional replacement risk";else if(amount>=ask)result="PROVISIONALLY ACCEPTED — contract stage required";else if(amount*100L>=ask*85L)result="COUNTEROFFER — "+NativeGlobalMarket.money(ask);else result="REJECTED — offer too low";
            save(a,r,amount,result);d.dismiss();refresh.run();AlertDialog.Builder answer=new AlertDialog.Builder(a).setTitle(r.p.name).setMessage("Your offer: "+NativeGlobalMarket.money(amount)+"\nClub response: "+result+(result.startsWith("PROVISIONALLY")?"\n\nYou may now negotiate the player contract.":"\n\nNo budget or ownership change has been made."));if(result.startsWith("PROVISIONALLY"))answer.setNegativeButton("CONTRACT",(x,y)->contract(a,r,amount,refresh));answer.setPositiveButton("Close",null).show();
        }));d.show();
    }

    static void contract(Activity a,NativeGlobalMarket.R r,int fee,Runnable refresh){
        LinearLayout box=new LinearLayout(a);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(NativeGlobalMarket.dp(a,18),0,NativeGlobalMarket.dp(a,18),0);box.addView(NativeGlobalMarket.tx(a,"Accepted fee: "+NativeGlobalMarket.money(fee)+"\nEnter annual wage in £ millions and contract length (1–5 years):",14f,false));EditText wage=new EditText(a);wage.setHint("Annual wage");wage.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);wage.setText(String.format(Locale.US,"%.2f",r.p.wage/1000000.0));box.addView(wage);EditText years=new EditText(a);years.setHint("Years");years.setInputType(InputType.TYPE_CLASS_NUMBER);years.setText("3");box.addView(years);
        AlertDialog d=new AlertDialog.Builder(a).setTitle("Contract — "+r.p.name).setView(box).setNegativeButton("Cancel",null).setPositiveButton("COMPLETE TRANSFER",null).create();d.setOnShowListener(v->d.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v2->{int annual,length;try{annual=(int)Math.round(Double.parseDouble(wage.getText().toString().replace(',','.'))*1000000.0);length=Integer.parseInt(years.getText().toString());}catch(Throwable t){wage.setError("Check wage and years");return;}String result=NativeCareerStore.transfer(a,r,fee,annual,length);if(result.startsWith("COMPLETED")){save(a,r,fee,"COMPLETED — "+length+" year contract");d.dismiss();refresh.run();}new AlertDialog.Builder(a).setTitle(result.startsWith("COMPLETED")?"Transfer completed":"Transfer not completed").setMessage(result+"\n\nThe native ownership, finances, contract and deal journal are updated together. The native deal is reconciled into the legacy matchday roster.").setPositiveButton("Close",null).show();}));d.show();
    }

    static void show(Activity a,ArrayList<NativeGlobalMarket.R> all){
        ArrayList<String> rows=new ArrayList<String>();ArrayList<NativeGlobalMarket.R> refs=new ArrayList<NativeGlobalMarket.R>();for(NativeGlobalMarket.R r:all){String s=status(a,r);if(s.length()>0){rows.add(r.p.name+" · "+r.team.name+"\nOffer "+NativeGlobalMarket.money(offer(a,r))+"   "+s+(s.startsWith("PROVISIONALLY")?"\nTap to negotiate contract":""));refs.add(r);}}
        if(rows.isEmpty())rows.add("No negotiations in this career season.");ListView list=new ListView(a);list.setAdapter(new ArrayAdapter<String>(a,android.R.layout.simple_list_item_1,rows));AlertDialog dialog=new AlertDialog.Builder(a).setTitle("Negotiations").setView(list).setPositiveButton("Close",null).create();list.setOnItemClickListener((p,v,n,id)->{if(n<refs.size()){NativeGlobalMarket.R r=refs.get(n);if(status(a,r).startsWith("PROVISIONALLY")){dialog.dismiss();contract(a,r,offer(a,r),()->{});}}});dialog.show();
    }
}
